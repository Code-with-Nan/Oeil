package com.example.oeil.model

data class ScreenElement(
    val id: String,
    val label: String,
    val clickable: Boolean,
    val editable: Boolean
)

/** A launchable app that the assistant may open. */
data class InstalledApp(
    val label: String,
    val packageName: String
)

data class SystemAction(
    val type: String,
    val summary: String,
    val targetId: String? = null,
    val targetLabel: String? = null,
    val text: String? = null,
    val direction: String? = null,
    val packageName: String? = null,
    val appName: String? = null,
    val url: String? = null
)

sealed interface AgentTurn {
    data class Message(val text: String) : AgentTurn
    data class Action(val action: SystemAction) : AgentTurn
}
