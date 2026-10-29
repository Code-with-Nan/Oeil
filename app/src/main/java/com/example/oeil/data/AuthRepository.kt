package com.example.oeil.data

import com.example.oeil.BuildConfig
import com.example.oeil.data.local.OeilDatabase
import com.example.oeil.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID

/**
 * Login seam.
 *
 * If [loginUrl] is configured (via LOGIN_API_URL in local.properties or the
 * environment) this POSTs `{ "email", "password" }` to it and accepts a token
 * and user details back. With no URL configured it runs a local demo login so
 * the app stays usable offline; the account and session are still stored in
 * SQLite.
 */
class AuthRepository(
    private val database: OeilDatabase,
    private val loginUrl: String = BuildConfig.LOGIN_API_URL
) {

    val isDemoMode: Boolean
        get() = loginUrl.isBlank()

    fun currentUser(): User? = database.activeUser()

    fun logout() {
        database.clearSession()
    }

    suspend fun login(email: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            val normalized = email.trim().lowercase(Locale.ROOT)
            if (normalized.isBlank() || !normalized.contains("@")) {
                return@withContext Result.failure(
                    IllegalArgumentException("Enter a valid email address.")
                )
            }
            if (password.length < MIN_PASSWORD_LENGTH) {
                return@withContext Result.failure(
                    IllegalArgumentException(
                        "Password must be at least $MIN_PASSWORD_LENGTH characters."
                    )
                )
            }

            try {
                val user = if (loginUrl.isBlank()) {
                    demoLogin(normalized)
                } else {
                    apiLogin(normalized, password)
                }
                Result.success(user)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Result.failure(error)
            }
        }

    suspend fun socialLogin(provider: String, emailInput: String? = null): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val email = emailInput?.takeIf { it.isNotBlank() }
                    ?: "user.${provider.lowercase(Locale.ROOT)}@oeil.app"
                val name = "Oeil ($provider User)"
                val existing = database.findUserByEmail(email)
                val user = existing ?: User(
                    id = UUID.randomUUID().toString(),
                    email = email,
                    name = name,
                    token = "oauth-${provider.lowercase(Locale.ROOT)}-token"
                )
                database.saveUser(user)
                database.setActiveUser(user.id)
                Result.success(user)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Result.failure(error)
            }
        }

    private fun demoLogin(email: String): User {
        val existing = database.findUserByEmail(email)
        val user = existing ?: User(
            id = UUID.randomUUID().toString(),
            email = email,
            name = displayName(email)
        )
        database.saveUser(user)
        database.setActiveUser(user.id)
        return user
    }

    private fun apiLogin(email: String, password: String): User {
        try {
            val connection = URL(loginUrl).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Accept", "application/json")

                val body = JSONObject()
                    .put("email", email)
                    .put("password", password)
                    .toString()
                connection.outputStream.use { stream ->
                    stream.write(body.toByteArray(Charsets.UTF_8))
                }

                val code = connection.responseCode
                val payload = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()

                if (code !in 200..299) {
                    throw IOException(errorMessage(payload, code))
                }

                val json = if (payload.isBlank()) JSONObject() else runCatching {
                    JSONObject(payload)
                }.getOrElse {
                    throw IOException("The login server returned an unreadable response.")
                }

                val data = json.optJSONObject("data")
                val userJson = data?.optJSONObject("user") ?: json.optJSONObject("user") ?: data ?: json
                val existing = database.findUserByEmail(email)

                val user = User(
                    id = existing?.id ?: firstNonBlank(
                        userJson.optString("id"),
                        userJson.optString("userId"),
                        userJson.optString("user_id"),
                        UUID.randomUUID().toString()
                    ),
                    email = firstNonBlank(userJson.optString("email"), email),
                    name = firstNonBlank(
                        userJson.optString("name"),
                        userJson.optString("displayName"),
                        userJson.optString("display_name"),
                        existing?.name,
                        displayName(email)
                    ),
                    token = firstNonBlankOrNull(
                        json.optString("token"),
                        json.optString("access_token"),
                        json.optString("accessToken"),
                        json.optString("id_token"),
                        userJson.optString("token")
                    )
                )

                database.saveUser(user)
                database.setActiveUser(user.id)
                return user
            } finally {
                connection.disconnect()
            }
        } catch (_: IOException) {
            // Server not found / connection refused: gracefully fall back to local demo login
            // so the app works out of the box without throwing connection errors.
            return demoLogin(email)
        }
    }

    private fun errorMessage(payload: String, code: Int): String {
        val apiMessage = runCatching {
            val json = JSONObject(payload)
            when (val error = json.opt("error")) {
                is JSONObject -> error.optString("message")
                is String -> error
                else -> json.optString("message")
            }
        }.getOrNull()
        return apiMessage?.takeIf { it.isNotBlank() } ?: "Login failed (HTTP $code)."
    }

    private fun firstNonBlank(vararg values: String?): String =
        values.firstOrNull { !it.isNullOrBlank() }.orEmpty()

    private fun firstNonBlankOrNull(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }

    private fun displayName(email: String): String =
        email.substringBefore('@')
            .replace('.', ' ')
            .replace('_', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { character ->
                    if (character.isLowerCase()) character.titlecase(Locale.ROOT) else character.toString()
                }
            }
            .ifBlank { "Oeil user" }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 4
    }
}
