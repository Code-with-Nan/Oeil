package com.example.oeil.data

import com.example.oeil.model.AgentTurn
import com.example.oeil.model.ChatMessage
import com.example.oeil.model.InstalledApp
import com.example.oeil.model.ReplyMode
import com.example.oeil.model.ScreenElement
import com.example.oeil.model.SystemAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Calls the Oeil relay; provider credentials never leave the server. */
class BackendReplySource(backendUrl: String) : ConversationReplySource, AndroidAgentSource {

    private val baseUrl = backendUrl.trimEnd('/')
    private val chatUrl = "$baseUrl/chat"
    private val agentUrl = "$baseUrl/agent/step"

    override suspend fun nextStep(
        task: String,
        screen: List<ScreenElement>,
        apps: List<InstalledApp>,
        history: List<ChatMessage>,
        mode: ReplyMode
    ): AgentTurn = withContext(Dispatchers.IO) {
        requireConfigured()

        val elements = JSONArray()
        screen.forEach { element ->
            elements.put(
                JSONObject()
                    .put("id", element.id)
                    .put("label", element.label)
                    .put("clickable", element.clickable)
                    .put("editable", element.editable)
            )
        }
        val appList = JSONArray()
        apps.forEach { app ->
            appList.put(
                JSONObject()
                    .put("label", app.label)
                    .put("package_name", app.packageName)
            )
        }
        val messages = JSONArray()
        history.takeLast(MAX_HISTORY_MESSAGES).forEach { message ->
            if (message.content.isBlank()) return@forEach
            messages.put(
                JSONObject()
                    .put("role", if (message.isUser) "user" else "assistant")
                    .put("content", message.content)
            )
        }

        val body = JSONObject()
            .put("task", task)
            .put("mode", mode.name.uppercase(Locale.ROOT))
            .put("screen", elements)
            .put("apps", appList)
            .put("messages", messages)
            .toString()
        val response = post(agentUrl, body)
        when (response.optString("type")) {
            "message" -> response.optString("message").trim()
                .takeIf(String::isNotBlank)
                ?.let(AgentTurn::Message)
                ?: throw IOException("The Oeil service returned an empty agent message.")
            "action" -> {
                val action = response.optJSONObject("action")
                    ?: throw IOException("The Oeil service returned an invalid action.")
                AgentTurn.Action(
                    SystemAction(
                        type = action.optString("type"),
                        summary = action.optString("summary"),
                        targetId = action.stringOrNull("target_id"),
                        targetLabel = action.stringOrNull("target_label"),
                        text = action.stringOrNull("text"),
                        direction = action.stringOrNull("direction"),
                        packageName = action.stringOrNull("package_name"),
                        appName = action.stringOrNull("app_name"),
                        url = action.stringOrNull("url")
                    )
                )
            }
            else -> throw IOException("The Oeil service returned an unknown agent step.")
        }
    }

    override suspend fun reply(messages: List<ChatMessage>, mode: ReplyMode): String =
        withContext(Dispatchers.IO) {
            requireConfigured()
            val response = post(chatUrl, buildBody(messages, mode))
            response.optString("reply").trim()
                .takeIf(String::isNotBlank)
                ?: throw IOException("The Oeil service returned an empty reply.")
        }

    private fun requireConfigured() {
        if (baseUrl.isBlank()) {
            throw IllegalStateException("The Oeil service is not configured in this build.")
        }
    }

    private fun buildBody(messages: List<ChatMessage>, mode: ReplyMode): String {
        val payload = JSONArray()
        messages.takeLast(MAX_HISTORY_MESSAGES).forEach { message ->
            val text = buildString {
                message.attachmentLabel?.let {
                    append("[Attached image: ").append(it).append("]\n")
                }
                append(message.content)
            }
            if (text.isNotBlank()) {
                payload.put(
                    JSONObject()
                        .put("role", if (message.isUser) "user" else "assistant")
                        .put("content", text)
                )
            }
        }

        return JSONObject()
            .put("mode", mode.name.uppercase(Locale.ROOT))
            .put("messages", payload)
            .toString()
    }

    private fun post(url: String, body: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 30_000
            connection.readTimeout = 120_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")

            connection.outputStream.use { stream ->
                stream.write(body.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            if (code !in 200..299) {
                throw IOException(errorMessage(response, code))
            }

            return runCatching { JSONObject(response) }.getOrElse {
                throw IOException("The Oeil service returned an unreadable response.")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.stringOrNull(key: String): String? =
        optString(key).takeIf(String::isNotBlank)

    private fun errorMessage(payload: String, code: Int): String {
        val message = runCatching {
            val json = JSONObject(payload)
            when (val error = json.opt("error")) {
                is JSONObject -> error.optString("message")
                is String -> error
                else -> json.optString("detail").ifBlank { json.optString("message") }
            }
        }.getOrNull()
        return message?.takeIf { it.isNotBlank() } ?: "AI request failed (HTTP $code)."
    }

    private companion object {
        const val MAX_HISTORY_MESSAGES = 20
    }
}
