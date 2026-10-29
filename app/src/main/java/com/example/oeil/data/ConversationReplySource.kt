package com.example.oeil.data

import com.example.oeil.model.ChatMessage
import com.example.oeil.model.ReplyMode

/**
 * History-aware reply seam. The old [ReplySource] only saw the last question;
 * a real model needs the whole conversation for context.
 */
interface ConversationReplySource {
    suspend fun reply(messages: List<ChatMessage>, mode: ReplyMode): String
}
