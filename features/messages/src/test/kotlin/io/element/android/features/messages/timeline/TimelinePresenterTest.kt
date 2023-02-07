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

@file:OptIn(ExperimentalCoroutinesApi::class)

package io.element.android.features.messages.timeline

import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrixtest.FakeMatrixClient
import io.element.android.libraries.matrixtest.core.A_ROOM_ID
import io.element.android.libraries.matrixtest.room.FakeMatrixRoom
import io.element.android.libraries.matrixtest.timeline.AN_EVENT_ID
import io.element.android.libraries.matrixtest.timeline.FakeMatrixTimeline
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TimelinePresenterTest {
    @Test
    fun `present - initial state`() = runTest {
        val presenter = TimelinePresenter(
            testCoroutineDispatchers(),
            FakeMatrixClient(),
            FakeMatrixRoom(A_ROOM_ID)
        )
        moleculeFlow(RecompositionClock.Immediate) {
            presenter.present()
        }.test {
            val initialState = awaitItem()
            assertThat(initialState.timelineItems.size).isEqualTo(0)
        }
    }

    @Test
    fun `present - load more`() = runTest {
        val matrixTimeline = FakeMatrixTimeline()
        val matrixRoom = FakeMatrixRoom(A_ROOM_ID, matrixTimeline = matrixTimeline)
        val presenter = TimelinePresenter(
            testCoroutineDispatchers(),
            FakeMatrixClient(),
            matrixRoom
        )
        moleculeFlow(RecompositionClock.Immediate) {
            presenter.present()
        }.test {
            val initialState = awaitItem()
            assertThat(initialState.hasMoreToLoad).isTrue()
            matrixTimeline.givenHasMoreToLoad(false)
            initialState.eventSink.invoke(TimelineEvents.LoadMore)
            val loadedState = awaitItem()
            assertThat(loadedState.hasMoreToLoad).isFalse()
        }
    }

    @Test
    fun `present - set highlighted event`() = runTest {
        val matrixTimeline = FakeMatrixTimeline()
        val matrixRoom = FakeMatrixRoom(A_ROOM_ID, matrixTimeline = matrixTimeline)
        val presenter = TimelinePresenter(
            testCoroutineDispatchers(),
            FakeMatrixClient(),
            matrixRoom
        )
        moleculeFlow(RecompositionClock.Immediate) {
            presenter.present()
        }.test {
            val initialState = awaitItem()
            assertThat(initialState.highlightedEventId).isNull()
            initialState.eventSink.invoke(TimelineEvents.SetHighlightedEvent(AN_EVENT_ID))
            val withHighlightedState = awaitItem()
            assertThat(withHighlightedState.highlightedEventId).isEqualTo(AN_EVENT_ID)
            initialState.eventSink.invoke(TimelineEvents.SetHighlightedEvent(null))
            val withoutHighlightedState = awaitItem()
            assertThat(withoutHighlightedState.highlightedEventId).isNull()
        }
    }
}
