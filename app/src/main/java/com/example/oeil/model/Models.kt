package com.example.oeil.model

import android.net.Uri
import java.util.UUID

/** Local profile used to scope conversation history on this device. */
data class User(val id: String, val name: String, val email: String? = null)

/** An image picked with the system photo picker, shown as a chip in the composer. */
data class Attachment(val uri: Uri, val label: String)

/** Answer style, picked from the header pill. */
enum class ReplyMode(val label: String) {
    Auto("Auto"),
    Precise("Precise"),
    Creative("Creative");

    fun next(): ReplyMode = entries[(ordinal + 1) % entries.size]
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val attachmentLabel: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

/** A saved conversation that can be reopened from the drawer. */
data class ChatThread(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ChatMessage>,
    val updatedAt: Long = System.currentTimeMillis(),
)

data class ScreenElement(
    val id: String,
    val label: String,
    val clickable: Boolean,
    val editable: Boolean,
)

/** A launchable app the assistant may open. */
data class InstalledApp(val label: String, val packageName: String)

data class SystemAction(
    val type: String,
    val summary: String,
    val targetId: String? = null,
    val targetLabel: String? = null,
    val text: String? = null,
    val direction: String? = null,
    val packageName: String? = null,
    val appName: String? = null,
    val url: String? = null,
)

sealed interface AgentTurn {
    data class Message(val text: String) : AgentTurn
    data class Action(val action: SystemAction) : AgentTurn
}
