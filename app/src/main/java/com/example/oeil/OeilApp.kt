package com.example.oeil

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.oeil.data.AuthRepository
import com.example.oeil.data.BackendReplySource
import com.example.oeil.data.MockConversationReplySource
import com.example.oeil.data.local.OeilDatabase
import com.example.oeil.model.User
import com.example.oeil.screen.LoginScreen
import com.example.oeil.screen.OeilScreen
import com.example.oeil.ui.theme.BackgroundDark
import com.example.oeil.ui.theme.GreenPrimary
import com.example.oeil.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Composition root: owns the SQLite store and decides between the sign-in gate
 * and the chat screen. A saved session skips login on the next launch.
 */
@Composable
fun OeilApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val database = remember { OeilDatabase(context) }
    val auth = remember { AuthRepository(database) }
    val scope = rememberCoroutineScope()

    var booting by remember { mutableStateOf(true) }
    var user by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        user = withContext(Dispatchers.IO) { auth.currentUser() }
        booting = false
    }

    when {
        booting -> BootScreen(modifier = modifier)

        user == null -> LoginScreen(
            authRepository = auth,
            onLoggedIn = { loggedIn -> user = loggedIn },
            modifier = modifier
        )

        else -> {
            val activeUser = user!!
            val replySource = remember(activeUser.id, activeUser.token) {
                if (BuildConfig.CHAT_API_URL.isNotBlank()) {
                    BackendReplySource(
                        chatUrl = BuildConfig.CHAT_API_URL,
                        token = activeUser.token
                    )
                } else {
                    MockConversationReplySource()
                }
            }
            OeilScreen(
                user = activeUser,
                replySource = replySource,
                store = database,
                onLogout = {
                    scope.launch {
                        withContext(Dispatchers.IO) { auth.logout() }
                        user = null
                    }
                },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun BootScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Oeil",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(20.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = GreenPrimary,
                strokeWidth = 2.dp
            )
        }
    }
}
