package com.example.oeil.data

import com.example.oeil.model.ChatMessage
import com.example.oeil.model.ReplyMode

/** Adapts the offline canned [ReplySource] to the history-aware seam. */
class MockConversationReplySource(
    private val delegate: ReplySource = MockReplySource()
) : ConversationReplySource {

    override suspend fun reply(messages: List<ChatMessage>, mode: ReplyMode): String {
        val query = messages.lastOrNull { it.isUser }?.content
            ?: messages.lastOrNull()?.content
            ?: return ""
        return delegate.answer(query, mode)
    }
}
