package com.example.oeil.data

import com.example.oeil.model.AgentTurn
import com.example.oeil.model.ChatMessage
import com.example.oeil.model.InstalledApp
import com.example.oeil.model.ReplyMode
import com.example.oeil.model.ScreenElement

interface AndroidAgentSource {
    suspend fun nextStep(
        task: String,
        screen: List<ScreenElement>,
        apps: List<InstalledApp>,
        history: List<ChatMessage>,
        mode: ReplyMode
    ): AgentTurn
}

interface AndroidActionExecutor {
    suspend fun screenElements(): List<ScreenElement>

    /** Apps that can be launched from the device home screen. */
    suspend fun installedApps(): List<InstalledApp> = emptyList()

    /** Returns a short result description, or null when the user denies the action. */
    suspend fun approveAndPerform(action: com.example.oeil.model.SystemAction): String?

    /** Shows a small progress banner while Oeil works in another app; null clears it. */
    fun updateTaskStatus(detail: String?) = Unit
}
