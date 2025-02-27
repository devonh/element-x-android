/*
 * Copyright (c) 2023 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.element.android.features.logout.impl.direct

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.features.logout.api.direct.DirectLogoutEvents
import io.element.android.features.logout.api.direct.DirectLogoutPresenter
import io.element.android.features.logout.api.direct.DirectLogoutState
import io.element.android.features.logout.impl.tools.isBackingUp
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.runCatchingUpdatingState
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.featureflag.api.FeatureFlagService
import io.element.android.libraries.featureflag.api.FeatureFlags
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.encryption.BackupUploadState
import io.element.android.libraries.matrix.api.encryption.EncryptionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@ContributesBinding(SessionScope::class)
class DefaultDirectLogoutPresenter @Inject constructor(
    private val matrixClient: MatrixClient,
    private val encryptionService: EncryptionService,
    private val featureFlagService: FeatureFlagService,
) : DirectLogoutPresenter {
    @Composable
    override fun present(): DirectLogoutState {
        val localCoroutineScope = rememberCoroutineScope()

        val logoutAction: MutableState<AsyncAction<String?>> = remember {
            mutableStateOf(AsyncAction.Uninitialized)
        }

        val secureStorageFlag by featureFlagService.isFeatureEnabledFlow(FeatureFlags.SecureStorage)
            .collectAsState(initial = null)

        val backupUploadState: BackupUploadState by remember(secureStorageFlag) {
            when (secureStorageFlag) {
                true -> encryptionService.waitForBackupUploadSteadyState()
                false -> flowOf(BackupUploadState.Done)
                else -> emptyFlow()
            }
        }
            .collectAsState(initial = BackupUploadState.Unknown)

        var isLastSession by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            isLastSession = encryptionService.isLastDevice().getOrNull() ?: false
        }

        fun handleEvents(event: DirectLogoutEvents) {
            when (event) {
                is DirectLogoutEvents.Logout -> {
                    if (logoutAction.value.isConfirming() || event.ignoreSdkError) {
                        localCoroutineScope.logout(logoutAction, event.ignoreSdkError)
                    } else {
                        logoutAction.value = AsyncAction.Confirming
                    }
                }
                DirectLogoutEvents.CloseDialogs -> {
                    logoutAction.value = AsyncAction.Uninitialized
                }
            }
        }

        return DirectLogoutState(
            canDoDirectSignOut = !isLastSession &&
                !backupUploadState.isBackingUp(),
            logoutAction = logoutAction.value,
            eventSink = ::handleEvents
        )
    }

    private fun CoroutineScope.logout(
        logoutAction: MutableState<AsyncAction<String?>>,
        ignoreSdkError: Boolean,
    ) = launch {
        suspend {
            matrixClient.logout(ignoreSdkError)
        }.runCatchingUpdatingState(logoutAction)
    }
}
