package com.example.oeil.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    /** Display name of an attached image, or null when the message has none. */
    val attachmentLabel: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
