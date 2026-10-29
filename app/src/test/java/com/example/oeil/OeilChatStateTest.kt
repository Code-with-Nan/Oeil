package com.example.oeil

import com.example.oeil.model.ReplyMode
import com.example.oeil.state.OeilChatState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the conversation bookkeeping behind the drawer: saving, reopening and
 * clearing threads. The reply coroutine is parked with an unreachable delay so
 * message lists stay deterministic.
 */
class OeilChatStateTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private fun state() = OeilChatState(replyDelayMs = Long.MAX_VALUE)

    @Test
    fun `first send opens a thread titled from the question`() {
        val chat = state()
        chat.updateInput("how does compose work")
        chat.send(scope)

        assertEquals(1, chat.threads.size)
        assertEquals("how does compose work", chat.threads[0].title)
        assertEquals(chat.threads[0].id, chat.activeThreadId)
        assertEquals("", chat.input)
        assertTrue(chat.isTyping)
    }

    @Test
    fun `long questions are truncated for the drawer row`() {
        val chat = state()
        chat.updateInput("a".repeat(80))
        chat.send(scope)

        val title = chat.threads[0].title
        assertTrue(title.length <= 42)
        assertTrue(title.endsWith("…"))
    }

    @Test
    fun `stopping a reply then sending again updates the same thread`() {
        val chat = state()
        chat.updateInput("first")
        chat.send(scope)
        chat.stopGenerating()

        assertFalse(chat.isTyping)
        // Blocked while a reply is in flight, allowed once it is stopped.
        chat.updateInput("second")
        chat.send(scope)

        assertEquals(1, chat.threads.size)
        assertEquals(2, chat.threads[0].messages.size)
        assertEquals("first", chat.threads[0].messages[0].content)
    }

    @Test
    fun `new chat keeps history and clears the draft`() {
        val chat = state()
        chat.updateInput("keep me")
        chat.send(scope)
        chat.stopGenerating()
        chat.startNewChat()

        assertTrue(chat.messages.isEmpty())
        assertNull(chat.activeThreadId)
        assertEquals(1, chat.threads.size)

        chat.openThread(chat.threads[0].id)
        assertEquals(1, chat.messages.size)
        assertEquals("keep me", chat.messages[0].content)
        assertEquals(chat.threads[0].id, chat.activeThreadId)
    }

    @Test
    fun `reopening the newest thread keeps it first in recency order`() {
        val chat = state()
        chat.updateInput("older")
        chat.send(scope)
        chat.stopGenerating()
        chat.startNewChat()
        chat.updateInput("newest")
        chat.send(scope)
        chat.stopGenerating()

        assertEquals(listOf("newest", "older"), chat.threads.map { it.title })

        chat.openThread(chat.threads[1].id)
        chat.updateInput("more on the old one")
        chat.send(scope)
        chat.stopGenerating()

        assertEquals("older", chat.threads[0].title)
        assertEquals(listOf("older", "more on the old one"), chat.threads[0].messages.map { it.content })
    }

    @Test
    fun `clearing removes history and the open draft`() {
        val chat = state()
        chat.updateInput("bye")
        chat.send(scope)
        chat.clearAll()

        assertTrue(chat.threads.isEmpty())
        assertTrue(chat.messages.isEmpty())
        assertNull(chat.activeThreadId)
        assertEquals("", chat.input)
    }

    @Test
    fun `an empty draft can never be sent`() {
        val chat = state()
        assertFalse(chat.canSend)

        chat.updateInput("   ")
        assertFalse(chat.canSend)

        chat.updateInput("text")
        assertTrue(chat.canSend)
    }

    @Test
    fun `mode cycles through all three styles`() {
        val chat = state()
        assertEquals(ReplyMode.Auto, chat.mode)

        chat.cycleMode()
        assertEquals(ReplyMode.Precise, chat.mode)

        chat.cycleMode()
        assertEquals(ReplyMode.Creative, chat.mode)

        chat.cycleMode()
        assertEquals(ReplyMode.Auto, chat.mode)
    }
}
