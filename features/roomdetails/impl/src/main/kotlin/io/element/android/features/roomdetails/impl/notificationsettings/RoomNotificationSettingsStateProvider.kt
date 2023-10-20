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

package io.element.android.features.roomdetails.impl.notificationsettings

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.architecture.Async
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.matrix.api.room.RoomNotificationSettings

internal class RoomNotificationSettingsStateProvider : PreviewParameterProvider<RoomNotificationSettingsState> {
    override val values: Sequence<RoomNotificationSettingsState>
        get() = sequenceOf(
            RoomNotificationSettingsState(
                roomName = "Room 1",
                Async.Success(RoomNotificationSettings(
                    mode = RoomNotificationMode.MUTE,
                    isDefault = true)),
                pendingRoomNotificationMode = null,
                pendingSetDefault = null,
                defaultRoomNotificationMode = RoomNotificationMode.ALL_MESSAGES,
                setNotificationSettingAction = Async.Uninitialized,
                restoreDefaultAction = Async.Uninitialized,
                eventSink = { },
            ),
            RoomNotificationSettingsState(
                roomName = "Room 1",
                Async.Success(RoomNotificationSettings(
                    mode = RoomNotificationMode.MUTE,
                    isDefault = false)),
                pendingRoomNotificationMode = null,
                pendingSetDefault = null,
                defaultRoomNotificationMode = RoomNotificationMode.ALL_MESSAGES,
                setNotificationSettingAction = Async.Uninitialized,
                restoreDefaultAction = Async.Uninitialized,
                eventSink = { },
            ),
        )
}
