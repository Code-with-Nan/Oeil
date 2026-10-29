package com.example.oeil.model

import java.util.UUID

/**
 * A conversation kept in [com.example.oeil.state.OeilChatState.threads] so it can
 * be reopened from the drawer.
 */
data class ChatThread(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ChatMessage>,
    val updatedAt: Long = System.currentTimeMillis()
)
