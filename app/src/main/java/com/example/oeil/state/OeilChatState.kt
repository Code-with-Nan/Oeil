package com.example.oeil.state

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.oeil.data.AndroidActionExecutor
import com.example.oeil.data.AndroidAgentSource
import com.example.oeil.data.ConversationReplySource
import com.example.oeil.data.OeilDatabase
import com.example.oeil.model.AgentTurn
import com.example.oeil.model.Attachment
import com.example.oeil.model.ChatMessage
import com.example.oeil.model.ChatThread
import com.example.oeil.model.ReplyMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Conversation state for the chat screen: messages, drafts, replies and history. */
class OeilChatState(
    private val replySource: ConversationReplySource = ConversationReplySource { _, _ ->
        error("No reply source configured.")
    },
    private val replyDelayMs: Long = 0L,
    private val store: OeilDatabase? = null,
    private val userId: String? = null,
    private val androidAgentSource: AndroidAgentSource? = null,
    private val androidActionExecutor: AndroidActionExecutor? = null,
) {
    val messages = mutableStateListOf<ChatMessage>()
    val threads = mutableStateListOf<ChatThread>()

    var input by mutableStateOf("")
        private set
    var isTyping by mutableStateOf(false)
        private set
    var mode by mutableStateOf(ReplyMode.Auto)
        private set
    var androidMode by mutableStateOf(false)
        private set
    var draftAttachment by mutableStateOf<Attachment?>(null)
        private set
    var activeThreadId by mutableStateOf<String?>(null)
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    private val persistence = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private var replyJob: Job? = null

    val canSend: Boolean
        get() = !isTyping && (input.isNotBlank() || draftAttachment != null)

    /** Bottom row to keep on screen: the typing placeholder, else the last message. */
    val anchorIndex: Int
        get() = if (isTyping) messages.size else messages.size - 1

    suspend fun loadHistory() {
        val database = store ?: return
        val owner = userId ?: return
        val saved = withContext(Dispatchers.IO) { database.threads(owner) }
        threads.clear()
        threads.addAll((saved + threads).distinctBy { it.id }.sortedByDescending { it.updatedAt })
    }

    fun updateInput(value: String) {
        input = value
    }

    /** Adds dictated text without discarding what was already typed. */
    fun appendInput(text: String) {
        input = if (input.isBlank()) text else "${input.trimEnd()} $text"
    }

    fun applySuggestion(suggestion: String) {
        input = suggestion
    }

    fun attach(uri: Uri, label: String) {
        draftAttachment = Attachment(uri, label)
    }

    fun clearAttachment() {
        draftAttachment = null
    }

    fun cycleMode() {
        mode = mode.next()
    }

    @JvmName("updateAndroidMode")
    fun setAndroidMode(enabled: Boolean) {
        androidMode = enabled && androidAgentSource != null && androidActionExecutor != null
    }

    fun send(scope: CoroutineScope) {
        val text = input.trim()
        val attachment = draftAttachment
        if (isTyping || (text.isEmpty() && attachment == null)) return

        messages.add(
            ChatMessage(
                content = text.ifEmpty { attachment?.label.orEmpty() },
                isUser = true,
                attachmentLabel = attachment?.label,
            )
        )
        input = ""
        draftAttachment = null
        isTyping = true
        syncThread()

        val history = messages.toList()
        val activeMode = mode
        val useAndroid = androidMode

        replyJob = scope.launch {
            if (replyDelayMs > 0) delay(replyDelayMs)
            try {
                if (useAndroid) {
                    runAndroidTask(text, activeMode)
                } else {
                    val reply = withContext(Dispatchers.IO) { replySource.reply(history, activeMode) }
                    messages.add(ChatMessage(content = reply, isUser = false))
                }
                isTyping = false
                syncThread()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                messages.add(ChatMessage(content = failureMessage(error), isUser = false))
                isTyping = false
                syncThread()
            }
        }
    }

    fun stopGenerating() {
        cancelReply()
        isTyping = false
    }

    fun openThread(id: String) {
        if (id == activeThreadId) return
        val thread = threads.firstOrNull { it.id == id } ?: return
        cancelReply()
        messages.clear()
        messages.addAll(thread.messages)
        activeThreadId = thread.id
        input = ""
        draftAttachment = null
        isTyping = false
    }

    fun startNewChat() {
        cancelReply()
        messages.clear()
        activeThreadId = null
        input = ""
        draftAttachment = null
        isTyping = false
    }

    fun clearAll() {
        cancelReply()
        threads.clear()
        messages.clear()
        activeThreadId = null
        input = ""
        draftAttachment = null
        isTyping = false
        store?.let { database ->
            userId?.let { owner -> persistence.launch { database.clearThreads(owner) } }
        }
    }

    fun close() {
        cancelReply()
        persistence.cancel()
    }

    private fun cancelReply() {
        replyJob?.cancel()
        replyJob = null
    }

    private suspend fun runAndroidTask(task: String, activeMode: ReplyMode) {
        val agent = checkNotNull(androidAgentSource)
        val controller = checkNotNull(androidActionExecutor)
        controller.updateTaskStatus("Preparing the task")
        try {
            var taskContext = task
            repeat(MAX_AGENT_STEPS) {
                controller.updateTaskStatus("Reading the screen")
                val screen = controller.screenElements()
                val apps = try {
                    controller.installedApps()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    emptyList()
                }

                controller.updateTaskStatus("Thinking about the next step")
                val turn = when (val step = agent.nextStep(
                    task = taskContext,
                    screen = screen,
                    apps = apps,
                    history = messages.toList(),
                    mode = activeMode,
                )) {
                    is AgentTurn.Message -> {
                        messages.add(ChatMessage(content = step.text, isUser = false))
                        syncThread()
                        return
                    }

                    is AgentTurn.Action -> step
                }

                messages.add(ChatMessage(content = "Android action: ${turn.action.summary}", isUser = false))
                syncThread()
                controller.updateTaskStatus("Waiting for approval: ${turn.action.summary}")
                val result = controller.approveAndPerform(turn.action)
                if (result == null) {
                    messages.add(ChatMessage(content = "Action denied. I stopped here.", isUser = false))
                    syncThread()
                    return
                }
                messages.add(ChatMessage(content = result, isUser = false))
                syncThread()
                controller.updateTaskStatus("Checking the result")
                taskContext += "\nApproved action result: $result"
                delay(AGENT_SCREEN_SETTLE_MS)
            }
            messages.add(
                ChatMessage(
                    content = "I stopped after $MAX_AGENT_STEPS actions. Tell me what to do next.",
                    isUser = false,
                )
            )
            syncThread()
        } finally {
            controller.updateTaskStatus(null)
        }
    }

    /** Mirrors the open conversation into the drawer and SQLite. */
    private fun syncThread() {
        if (messages.isEmpty()) return
        val snapshot = messages.toList()
        val existing = activeThreadId?.let { id -> threads.indexOfFirst { it.id == id } } ?: -1
        val thread = if (existing >= 0) {
            threads.removeAt(existing).copy(
                title = titleOf(snapshot),
                messages = snapshot,
                updatedAt = System.currentTimeMillis(),
            )
        } else {
            ChatThread(title = titleOf(snapshot), messages = snapshot)
        }
        activeThreadId = thread.id
        threads.add(0, thread)
        store?.let { database ->
            userId?.let { owner -> persistence.launch { database.saveThread(owner, thread) } }
        }
    }

    private fun failureMessage(error: Throwable): String {
        val detail = error.message?.trim().orEmpty()
        return if (detail.isBlank()) {
            "I couldn't get a reply from the AI service. Please try again."
        } else {
            "I couldn't get a reply: $detail"
        }
    }

    private fun titleOf(items: List<ChatMessage>): String {
        val first = items.firstOrNull { it.isUser }?.content ?: items.first().content
        val flat = first.replace('\n', ' ').trim()
        return if (flat.length <= TITLE_MAX) flat else flat.take(TITLE_MAX - 1).trimEnd() + "…"
    }

    private companion object {
        const val TITLE_MAX = 42
        const val MAX_AGENT_STEPS = 8
        const val AGENT_SCREEN_SETTLE_MS = 1000L
    }
}
