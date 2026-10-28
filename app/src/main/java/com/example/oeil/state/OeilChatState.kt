package com.example.oeil.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.oeil.data.MockReplySource
import com.example.oeil.data.ReplySource
import com.example.oeil.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds the conversation state for [com.example.oeil.screen.OeilScreen].
 *
 * Kept as a plain class so the screen only deals with layout, while message
 * history, typing flags and reply timing live in one testable place.
 *
 * Replies are produced by a Java [ReplySource], so a real backend SDK can be
 * dropped in without touching any Compose code.
 */
class OeilChatState(
    private val replySource: ReplySource = MockReplySource()
) {

    val messages = mutableStateListOf<ChatMessage>()

    var input by mutableStateOf("")
        private set

    var isTyping by mutableStateOf(false)
        private set

    fun updateInput(value: String) {
        input = value
    }

    fun applySuggestion(suggestion: String) {
        input = suggestion
    }

    /** Clears the thread; cancels any reply still in flight. */
    fun startNewChat() {
        replyJob?.cancel()
        replyJob = null
        messages.clear()
        input = ""
        isTyping = false
    }

    fun send(scope: CoroutineScope) {
        val text = input.trim()
        if (text.isEmpty() || isTyping) return

        messages.add(ChatMessage(content = text, isUser = true))
        input = ""
        isTyping = true

        replyJob = scope.launch {
            delay(REPLY_DELAY_MS)
            // ReplySource is a blocking Java API: keep it off the main thread.
            val reply = withContext(Dispatchers.IO) { replySource.answer(text) }
            messages.add(ChatMessage(content = reply, isUser = false))
            isTyping = false
        }
    }

    private var replyJob: Job? = null

    private companion object {
        const val REPLY_DELAY_MS = 1_100L
    }
}
