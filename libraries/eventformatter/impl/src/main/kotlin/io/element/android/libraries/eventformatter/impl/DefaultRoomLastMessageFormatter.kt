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

package io.element.android.libraries.eventformatter.impl

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.eventformatter.api.RoomLastMessageFormatter
import io.element.android.libraries.eventformatter.impl.mode.RenderingMode
import io.element.android.libraries.matrix.api.timeline.item.event.AudioMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.EmoteMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.EventTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.FailedToParseMessageLikeContent
import io.element.android.libraries.matrix.api.timeline.item.event.FailedToParseStateContent
import io.element.android.libraries.matrix.api.timeline.item.event.FileMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.ImageMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.LocationMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.MessageType
import io.element.android.libraries.matrix.api.timeline.item.event.NoticeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.OtherMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.PollContent
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileChangeContent
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileTimelineDetails
import io.element.android.libraries.matrix.api.timeline.item.event.RedactedContent
import io.element.android.libraries.matrix.api.timeline.item.event.RoomMembershipContent
import io.element.android.libraries.matrix.api.timeline.item.event.StateContent
import io.element.android.libraries.matrix.api.timeline.item.event.StickerContent
import io.element.android.libraries.matrix.api.timeline.item.event.StickerMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.UnableToDecryptContent
import io.element.android.libraries.matrix.api.timeline.item.event.UnknownContent
import io.element.android.libraries.matrix.api.timeline.item.event.VideoMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.VoiceMessageType
import io.element.android.libraries.matrix.ui.messages.toPlainText
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.services.toolbox.api.strings.StringProvider
import javax.inject.Inject

@ContributesBinding(SessionScope::class)
class DefaultRoomLastMessageFormatter @Inject constructor(
    private val sp: StringProvider,
    private val roomMembershipContentFormatter: RoomMembershipContentFormatter,
    private val profileChangeContentFormatter: ProfileChangeContentFormatter,
    private val stateContentFormatter: StateContentFormatter,
) : RoomLastMessageFormatter {
    companion object {
        // Max characters to display in the last message. This works around https://github.com/element-hq/element-x-android/issues/2105
        private const val MAX_SAFE_LENGTH = 500
    }

    override fun format(event: EventTimelineItem, isDmRoom: Boolean): CharSequence? {
        val isOutgoing = event.isOwn
        // Note: we do not use disambiguated display name here, see
        // https://github.com/element-hq/element-x-ios/issues/1845#issuecomment-1888707428
        val senderDisplayName = (event.senderProfile as? ProfileTimelineDetails.Ready)?.displayName ?: event.sender.value
        return when (val content = event.content) {
            is MessageContent -> processMessageContents(content, senderDisplayName, isDmRoom)
            RedactedContent -> {
                val message = sp.getString(CommonStrings.common_message_removed)
                if (!isDmRoom) {
                    prefix(message, senderDisplayName)
                } else {
                    message
                }
            }
            is StickerContent -> {
                content.body
            }
            is UnableToDecryptContent -> {
                val message = sp.getString(CommonStrings.common_waiting_for_decryption_key)
                if (!isDmRoom) {
                    prefix(message, senderDisplayName)
                } else {
                    message
                }
            }
            is RoomMembershipContent -> {
                roomMembershipContentFormatter.format(content, senderDisplayName, isOutgoing)
            }
            is ProfileChangeContent -> {
                profileChangeContentFormatter.format(content, senderDisplayName, isOutgoing)
            }
            is StateContent -> {
                stateContentFormatter.format(content, senderDisplayName, isOutgoing, RenderingMode.RoomList)
            }
            is PollContent -> {
                val message = sp.getString(CommonStrings.common_poll_summary, content.question)
                prefixIfNeeded(message, senderDisplayName, isDmRoom)
            }
            is FailedToParseMessageLikeContent, is FailedToParseStateContent, is UnknownContent -> {
                prefixIfNeeded(sp.getString(CommonStrings.common_unsupported_event), senderDisplayName, isDmRoom)
            }
        }?.take(MAX_SAFE_LENGTH)
    }

    private fun processMessageContents(messageContent: MessageContent, senderDisplayName: String, isDmRoom: Boolean): CharSequence? {
        val internalMessage = when (val messageType: MessageType = messageContent.type) {
            // Doesn't need a prefix
            is EmoteMessageType -> {
                return "* $senderDisplayName ${messageType.body}"
            }
            is TextMessageType -> {
                messageType.toPlainText()
            }
            is VideoMessageType -> {
                sp.getString(CommonStrings.common_video)
            }
            is ImageMessageType -> {
                sp.getString(CommonStrings.common_image)
            }
            is StickerMessageType -> {
                sp.getString(CommonStrings.common_sticker)
            }
            is LocationMessageType -> {
                sp.getString(CommonStrings.common_shared_location)
            }
            is FileMessageType -> {
                sp.getString(CommonStrings.common_file)
            }
            is AudioMessageType -> {
                sp.getString(CommonStrings.common_audio)
            }
            is VoiceMessageType -> {
                sp.getString(CommonStrings.common_voice_message)
            }
            is OtherMessageType -> {
                messageType.body
            }
            is NoticeMessageType -> {
                messageType.body
            }
        }
        return prefixIfNeeded(internalMessage, senderDisplayName, isDmRoom)
    }

    private fun prefixIfNeeded(message: String, senderDisplayName: String, isDmRoom: Boolean): CharSequence = if (isDmRoom) {
        message
    } else {
        prefix(message, senderDisplayName)
    }

    private fun prefix(message: String, senderDisplayName: String): AnnotatedString {
        return buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(senderDisplayName)
            }
            append(": ")
            append(message)
        }
    }
}
