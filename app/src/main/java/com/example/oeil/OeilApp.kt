package com.example.oeil

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.oeil.data.AuthClient
import com.example.oeil.data.AuthSession
import com.example.oeil.data.BackendReplySource
import com.example.oeil.data.local.OeilDatabase
import com.example.oeil.data.normalizeBackendUrl
import com.example.oeil.model.User
import com.example.oeil.screen.AuthScreen
import com.example.oeil.screen.OeilScreen
import com.example.oeil.screen.SettingsScreen
import com.example.oeil.system.OeilAccessibility
import com.example.oeil.ui.theme.BackgroundDark
import com.example.oeil.ui.theme.GreenPrimary
import com.example.oeil.ui.theme.OeilTheme
import com.example.oeil.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.view.WindowCompat
import java.io.IOException

/** Composition root for local state, account settings and the chat screen. */
@Composable
fun OeilApp(
    authCallbackUri: String?,
    onAuthCallbackHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val database = remember { OeilDatabase(context) }
    val scope = rememberCoroutineScope()

    var booting by remember { mutableStateOf(true) }
    var user by remember { mutableStateOf<User?>(null) }
    val backendUrl = remember { configuredBackendUrl() }
    var authToken by remember { mutableStateOf<String?>(null) }
    var themeMode by remember { mutableStateOf("Dark") }
    var showSettings by remember { mutableStateOf(false) }
    var showAuth by remember { mutableStateOf(false) }
    var authBusy by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }
    val accessibilityConnected by OeilAccessibility.connected.collectAsState()
    val darkTheme = themeMode == "Dark" ||
        (themeMode == "System" && androidx.compose.foundation.isSystemInDarkTheme())
    val view = LocalView.current

    SideEffect {
        val activity = view.context as? Activity
        if (activity != null) {
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    LaunchedEffect(Unit) {
        val savedState = withContext(Dispatchers.IO) {
            Triple(
                database.localUser(),
                database.getSetting(THEME_MODE_SETTING) ?: "Dark",
                database.getSetting(AUTH_TOKEN_SETTING),
            )
        }
        user = savedState.first
        themeMode = savedState.second
        authToken = savedState.third
        booting = false
    }

    LaunchedEffect(authCallbackUri, backendUrl, booting) {
        if (authCallbackUri == null || booting) return@LaunchedEffect
        showAuth = true
        authError = null
        if (backendUrl.isBlank()) {
            authError = "This build does not include an Oeil service address."
            onAuthCallbackHandled()
            return@LaunchedEffect
        }
        val callback = Uri.parse(authCallbackUri)
        val token = callback.fragment
            ?.let { Uri.parse("oeil://auth/callback?$it").getQueryParameter("token") }
        val error = callback.getQueryParameter("error")
        try {
            if (token != null) {
                val account = withContext(Dispatchers.IO) {
                    AuthClient(backendUrl).currentUser(token)
                }
                withContext(Dispatchers.IO) {
                    database.saveUser(account)
                    database.setActiveUser(account.id)
                    database.setSetting(AUTH_TOKEN_SETTING, token)
                }
                user = account
                authToken = token
                showAuth = false
                showSettings = true
            } else if (error != null) {
                authError = error
            } else {
                authError = "Sign-in did not complete. Please try again."
            }
        } catch (requestError: IOException) {
            authError = requestError.message ?: "Could not finish sign-in."
        } finally {
            onAuthCallbackHandled()
        }
    }

    OeilTheme(darkTheme = darkTheme) {
        when {
            booting -> BootScreen(modifier = modifier)
            showAuth -> AuthScreen(
                busy = authBusy,
                externalError = authError,
                onBack = {
                    showAuth = false
                    authError = null
                },
                onSubmit = { name, email, password, signUp ->
                    authError = null
                    authBusy = true
                    scope.launch {
                        try {
                            val session = withContext(Dispatchers.IO) {
                                val client = AuthClient(backendUrl)
                                if (signUp) client.signUp(name, email, password)
                                else client.signIn(email, password)
                            }
                            acceptSession(session, database) { account, token ->
                                user = account
                                authToken = token
                                showAuth = false
                                showSettings = true
                            }
                        } catch (requestError: IOException) {
                            authError = requestError.message ?: "Could not sign in."
                        } finally {
                            authBusy = false
                        }
                    }
                },
                onOAuth = { provider ->
                    authError = null
                    try {
                        val url = AuthClient(backendUrl).oauthStartUrl(provider)
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (error: ActivityNotFoundException) {
                        authError = "No browser is available to continue sign-in."
                    } catch (error: IllegalArgumentException) {
                        authError = error.message ?: "This build does not include an Oeil service address."
                    }
                },
                modifier = modifier,
            )
            showSettings -> SettingsScreen(
                user = requireNotNull(user),
                themeMode = themeMode,
                serviceConfigured = backendUrl.isNotBlank(),
                accessibilityConnected = accessibilityConnected,
                onBack = { showSettings = false },
                onEnableAccessibility = {
                    context.startActivity(OeilAccessibility.accessibilitySettingsIntent())
                },
                onThemeModeChange = { selectedMode ->
                    themeMode = selectedMode
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            database.setSetting(THEME_MODE_SETTING, selectedMode)
                        }
                    }
                },
                onSignIn = {
                    authError = null
                    showAuth = true
                },
                onSignOut = {
                    val token = authToken
                    scope.launch {
                        val revokeError = try {
                            if (token != null) {
                                withContext(Dispatchers.IO) { AuthClient(backendUrl).signOut(token) }
                            }
                            null
                        } catch (requestError: IOException) {
                            requestError
                        }
                        val localUser = withContext(Dispatchers.IO) {
                            database.setSetting(AUTH_TOKEN_SETTING, "")
                            database.clearSession()
                            database.localUser()
                        }
                        user = localUser
                        authToken = null
                        accountError = revokeError?.let {
                            "Signed out on this device, but the server could not revoke the session: " +
                                (it.message ?: "network error")
                        }
                    }
                },
                accountError = accountError,
                modifier = modifier,
            )
            else -> {
                val activeUser = requireNotNull(user)
                val replySource = remember(backendUrl) { BackendReplySource(backendUrl) }
                OeilScreen(
                    user = activeUser,
                    replySource = replySource,
                    store = database,
                    androidAgentSource = replySource,
                    androidActionExecutor = OeilAccessibility,
                    onOpenSettings = { showSettings = true },
                    modifier = modifier,
                )
            }
        }
    }
}

private suspend fun acceptSession(
    session: AuthSession,
    database: OeilDatabase,
    onAccepted: (User, String) -> Unit,
) {
    withContext(Dispatchers.IO) {
        database.saveUser(session.user)
        database.setActiveUser(session.user.id)
        database.setSetting(AUTH_TOKEN_SETTING, session.token)
    }
    onAccepted(session.user, session.token)
}

@Composable
private fun BootScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Oeil",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(20.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = GreenPrimary,
                strokeWidth = 2.dp,
            )
        }
    }
}

private fun configuredBackendUrl(): String {
    val raw = BuildConfig.OEIL_BACKEND_URL.trim()
    if (raw.isBlank()) return ""
    return runCatching { normalizeBackendUrl(raw) }.getOrDefault(raw.trimEnd('/'))
}

private const val THEME_MODE_SETTING = "theme_mode"
private const val AUTH_TOKEN_SETTING = "auth_token"
