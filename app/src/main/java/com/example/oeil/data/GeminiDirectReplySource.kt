package com.example.oeil.data

import com.example.oeil.model.ChatMessage
import com.example.oeil.model.ReplyMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Direct Gemini API client for Android. Calls the Gemini REST endpoint
 * directly using HttpURLConnection, making Oeil a fully standalone chat app
 * without requiring a separate Node backend server when a Gemini API key is provided.
 */
class GeminiDirectReplySource(
    private val apiKey: String,
    private val model: String = "gemini-2.5-flash"
) : ConversationReplySource {

    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/models"

    private val systemPrompts = mapOf(
        ReplyMode.Auto to "You are Oeil, a helpful, warm and clear AI assistant. Answer the user's latest message using the conversation context. Use plain text with short paragraphs. Never claim to be Gemini.",
        ReplyMode.Precise to "You are Oeil. Give direct, factual and concise answers. Prefer the shortest response that fully answers the question, and avoid filler.",
        ReplyMode.Creative to "You are Oeil, an imaginative assistant. Offer fresh angles, vivid examples and creative possibilities while still being useful and accurate."
    )

    private val temperatures = mapOf(
        ReplyMode.Auto to 0.8,
        ReplyMode.Precise to 0.3,
        ReplyMode.Creative to 1.1
    )

    override suspend fun reply(messages: List<ChatMessage>, mode: ReplyMode): String =
        withContext(Dispatchers.IO) {
            val trimmedKey = apiKey.trim()
            if (trimmedKey.isBlank()) {
                throw IllegalStateException(
                    "Gemini API key is not configured. Please enter your Gemini API key in settings or login."
                )
            }

            val contents = JSONArray()
            messages.takeLast(50).forEach { message ->
                val text = buildString {
                    message.attachmentLabel?.let {
                        append("[Attached image: ").append(it).append("]\n")
                    }
                    append(message.content)
                }
                if (text.isNotBlank()) {
                    contents.put(
                        JSONObject()
                            .put("role", if (message.isUser) "user" else "model")
                            .put(
                                "parts",
                                JSONArray().put(JSONObject().put("text", text.trim().take(8000)))
                            )
                    )
                }
            }

            if (contents.length() == 0) {
                throw IllegalStateException("There is no message to send.")
            }

            val cleanModel = model.replace(Regex("^models/"), "")
            val encodedModel = URLEncoder.encode(cleanModel, "UTF-8")
            val encodedKey = URLEncoder.encode(trimmedKey, "UTF-8")
            val urlString = "$endpoint/$encodedModel:generateContent?key=$encodedKey"

            val connection = URL(urlString).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 30_000
                connection.readTimeout = 90_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Accept", "application/json")

                val requestBody = JSONObject()
                    .put("contents", contents)
                    .put(
                        "systemInstruction",
                        JSONObject().put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put(
                                    "text",
                                    systemPrompts[mode] ?: systemPrompts[ReplyMode.Auto]
                                )
                            )
                        )
                    )
                    .put(
                        "generationConfig",
                        JSONObject()
                            .put("temperature", temperatures[mode] ?: 0.8)
                            .put("maxOutputTokens", 1024)
                    )
                    .toString()

                connection.outputStream.use { stream ->
                    stream.write(requestBody.toByteArray(Charsets.UTF_8))
                }

                val code = connection.responseCode
                val responseString = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()

                if (code !in 200..299) {
                    val errorMsg = runCatching {
                        JSONObject(responseString).optJSONObject("error")?.optString("message")
                    }.getOrNull() ?: "Gemini API request failed (HTTP $code)."
                    throw IOException(errorMsg)
                }

                val json = JSONObject(responseString)
                val candidates = json.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    val blockReason = json.optJSONObject("promptFeedback")?.optString("blockReason")
                    return@withContext if (!blockReason.isNullOrBlank()) {
                        "I can't help with that request ($blockReason)."
                    } else {
                        "Gemini didn't return a response. Please try again."
                    }
                }

                val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                val textResult = buildString {
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            append(parts.optJSONObject(i)?.optString("text").orEmpty())
                        }
                    }
                }.trim()

                if (textResult.isBlank()) {
                    "Gemini returned an empty response. Please try again."
                } else {
                    textResult
                }
            } finally {
                connection.disconnect()
            }
        }
}
