package com.example.oeil.data

import com.example.oeil.model.AgentTurn
import com.example.oeil.model.ChatMessage
import com.example.oeil.model.InstalledApp
import com.example.oeil.model.ReplyMode
import com.example.oeil.model.ScreenElement
import com.example.oeil.model.SystemAction
import com.example.oeil.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** Produces an assistant reply for a conversation. */
fun interface ConversationReplySource {
    suspend fun reply(messages: List<ChatMessage>, mode: ReplyMode): String
}

/** Proposes the next Android action, or a final message. */
interface AndroidAgentSource {
    suspend fun nextStep(
        task: String,
        screen: List<ScreenElement>,
        apps: List<InstalledApp>,
        history: List<ChatMessage>,
        mode: ReplyMode,
    ): AgentTurn
}

/** Performs approved Android actions on the device. */
interface AndroidActionExecutor {
    suspend fun screenElements(): List<ScreenElement>
    suspend fun installedApps(): List<InstalledApp> = emptyList()

    /** Returns a short result description, or null when the user denies the action. */
    suspend fun approveAndPerform(action: SystemAction): String?

    /** Shows a progress banner while Oeil works in another app; null clears it. */
    fun updateTaskStatus(detail: String?) = Unit
}

/** Normalizes a backend address and strips a trailing /chat endpoint. */
fun normalizeBackendUrl(value: String): String {
    val input = value.trim().trimEnd('/')
    if (input.isBlank()) return ""

    val uri = try {
        URI(input)
    } catch (error: URISyntaxException) {
        throw IllegalArgumentException("Enter a valid backend URL.", error)
    }
    require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
        "Backend URL must start with http:// or https://."
    }
    require(!uri.host.isNullOrBlank() && uri.rawUserInfo == null) {
        "Enter a backend URL with a host and no embedded credentials."
    }
    require(uri.rawQuery == null && uri.rawFragment == null) {
        "Backend URL must not include a query or fragment."
    }

    val path = uri.rawPath.orEmpty().trimEnd('/').removeSuffix("/chat")
    return "${uri.scheme.lowercase()}://${requireNotNull(uri.rawAuthority)}$path".trimEnd('/')
}

/** Calls the Oeil relay. Provider credentials never leave the server. */
class BackendReplySource(backendUrl: String) : ConversationReplySource, AndroidAgentSource {

    private val baseUrl = backendUrl.trimEnd('/')

    override suspend fun reply(messages: List<ChatMessage>, mode: ReplyMode): String =
        withContext(Dispatchers.IO) {
            requireConfigured()
            val body = JSONObject()
                .put("mode", mode.name.uppercase(Locale.ROOT))
                .put("messages", messageArray(messages, withAttachments = true))
            val response = post("$baseUrl/chat", body.toString())
            response.optString("reply").trim().takeIf(String::isNotBlank)
                ?: throw IOException("The Oeil service returned an empty reply.")
        }

    override suspend fun nextStep(
        task: String,
        screen: List<ScreenElement>,
        apps: List<InstalledApp>,
        history: List<ChatMessage>,
        mode: ReplyMode,
    ): AgentTurn = withContext(Dispatchers.IO) {
        requireConfigured()
        val body = JSONObject()
            .put("task", task)
            .put("mode", mode.name.uppercase(Locale.ROOT))
            .put("screen", JSONArray().apply {
                screen.forEach {
                    put(
                        JSONObject()
                            .put("id", it.id)
                            .put("label", it.label)
                            .put("clickable", it.clickable)
                            .put("editable", it.editable)
                    )
                }
            })
            .put("apps", JSONArray().apply {
                apps.forEach { put(JSONObject().put("label", it.label).put("package_name", it.packageName)) }
            })
            .put("messages", messageArray(history))

        val response = post("$baseUrl/agent/step", body.toString())
        when (response.optString("type")) {
            "message" -> response.optString("message").trim().takeIf(String::isNotBlank)
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
                        url = action.stringOrNull("url"),
                    )
                )
            }

            else -> throw IOException("The Oeil service returned an unknown agent step.")
        }
    }

    private fun requireConfigured() {
        check(baseUrl.isNotBlank()) { "The Oeil service is not configured in this build." }
    }

    private fun post(url: String, body: String): JSONObject =
        httpJson(url, body, readTimeoutMs = 120_000)

    private fun messageArray(
        messages: List<ChatMessage>,
        withAttachments: Boolean = false,
    ): JSONArray = JSONArray().apply {
        messages.takeLast(MAX_HISTORY).forEach { message ->
            val text = if (withAttachments) {
                listOfNotNull(message.attachmentLabel?.let { "[Attached image: $it]" }, message.content)
                    .joinToString("\n")
            } else {
                message.content
            }
            if (text.isNotBlank()) {
                put(
                    JSONObject()
                        .put("role", if (message.isUser) "user" else "assistant")
                        .put("content", text)
                )
            }
        }
    }

    private fun JSONObject.stringOrNull(key: String): String? =
        optString(key).takeIf(String::isNotBlank)

    private companion object {
        const val MAX_HISTORY = 20
    }
}

data class AuthSession(val user: User, val token: String)

/** Calls the account endpoints; OAuth provider secrets stay on the backend. */
class AuthClient(backendUrl: String) {
    private val baseUrl = backendUrl.trimEnd('/')

    suspend fun signUp(name: String, email: String, password: String): AuthSession =
        withContext(Dispatchers.IO) {
            call(
                "/auth/signup",
                JSONObject().put("name", name).put("email", email).put("password", password),
            ).toSession()
        }

    suspend fun signIn(email: String, password: String): AuthSession =
        withContext(Dispatchers.IO) {
            call("/auth/login", JSONObject().put("email", email).put("password", password)).toSession()
        }

    suspend fun currentUser(token: String): User = withContext(Dispatchers.IO) {
        call("/auth/me", token = token).toUser()
    }

    suspend fun signOut(token: String) = withContext(Dispatchers.IO) {
        call("/auth/logout", JSONObject(), token)
    }

    fun oauthStartUrl(provider: String): String {
        require(provider in SUPPORTED_PROVIDERS) { "Unsupported sign-in provider." }
        require(baseUrl.isNotBlank()) { "This build does not include an Oeil service address." }
        return "$baseUrl/auth/oauth/$provider/start?redirect_uri=$APP_REDIRECT_URI"
    }

    private fun call(path: String, body: JSONObject? = null, token: String? = null): JSONObject {
        if (baseUrl.isBlank()) {
            throw IOException("This build does not include an Oeil service address.")
        }
        return httpJson("$baseUrl$path", body?.toString(), token)
    }

    private fun JSONObject.toSession(): AuthSession {
        val token = optString("token").takeIf(String::isNotBlank)
            ?: throw IOException("The account server did not return a session.")
        return AuthSession(getJSONObject("user").toUser(), token)
    }

    private fun JSONObject.toUser(): User {
        val id = optString("id").takeIf(String::isNotBlank)
            ?: throw IOException("The account server returned an invalid account.")
        return User(
            id = id,
            name = optString("name").ifBlank { "Oeil account" },
            email = optString("email").ifBlank { null },
        )
    }

    private companion object {
        val SUPPORTED_PROVIDERS = setOf("google", "microsoft", "github")
        val APP_REDIRECT_URI = URLEncoder.encode("oeil://auth/callback", Charsets.UTF_8.name())
    }
}

/** One JSON request shared by the chat and account clients. */
private fun httpJson(
    url: String,
    body: String? = null,
    token: String? = null,
    readTimeoutMs: Int = 15_000,
): JSONObject {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = if (body == null) "GET" else "POST"
        connection.connectTimeout = 15_000
        connection.readTimeout = readTimeoutMs
        connection.setRequestProperty("Accept", "application/json")
        token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }

        val code = connection.responseCode
        val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()

        if (code !in 200..299) throw IOException(errorText(response, code))
        return runCatching { JSONObject(response) }.getOrElse {
            throw IOException("The Oeil service returned an unreadable response.")
        }
    } finally {
        connection.disconnect()
    }
}

private fun errorText(payload: String, code: Int): String {
    val message = runCatching {
        val json = JSONObject(payload)
        when (val error = json.opt("error")) {
            is JSONObject -> error.optString("message")
            is String -> error
            else -> json.optString("detail").ifBlank { json.optString("message") }
        }
    }.getOrNull()

    return message?.takeIf(String::isNotBlank) ?: "Oeil request failed (HTTP $code)."
}
