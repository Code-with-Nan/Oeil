package com.example.oeil.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.oeil.model.User
import com.example.oeil.ui.theme.BackgroundDark
import com.example.oeil.ui.theme.GreenPrimary
import com.example.oeil.ui.theme.TextPrimary
import com.example.oeil.ui.theme.TextSecondary

private val themeOptions = listOf("System", "Light", "Dark")

@Composable
fun SettingsScreen(
    user: User,
    themeMode: String,
    serviceConfigured: Boolean,
    accessibilityConnected: Boolean,
    onBack: () -> Unit,
    onEnableAccessibility: () -> Unit,
    onThemeModeChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    accountError: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back", color = TextSecondary) }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Settings",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Make Oeil feel like yours and manage your account.",
            modifier = Modifier.padding(top = 6.dp),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )

        SettingsSection(title = "Appearance", subtitle = "Choose how Oeil looks on this device.") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                themeOptions.forEach { option ->
                    if (themeMode == option) {
                        Button(
                            onClick = { onThemeModeChange(option) },
                            modifier = Modifier.weight(1f),
                        ) { Text(option) }
                    } else {
                        OutlinedButton(
                            onClick = { onThemeModeChange(option) },
                            modifier = Modifier.weight(1f),
                        ) { Text(option) }
                    }
                }
            }
        }

        SettingsSection(
            title = "System control",
            subtitle = "Let Oeil read the screen and perform actions you approve.",
        ) {
            Text(
                text = if (accessibilityConnected) {
                    "Android control is ready."
                } else {
                    "Android control is off."
                },
                color = if (accessibilityConnected) GreenPrimary else TextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (accessibilityConnected) {
                    "Ask Oeil to open apps, tap controls, type text, scroll, or navigate."
                } else {
                    "Turn on Oeil in Android Accessibility settings. Every action is " +
                        "approved in a system overlay before it runs."
                },
                modifier = Modifier.padding(top = 4.dp),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (accessibilityConnected) {
                OutlinedButton(onClick = onEnableAccessibility, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Android accessibility settings")
                }
            } else {
                Button(onClick = onEnableAccessibility, modifier = Modifier.fillMaxWidth()) {
                    Text("Enable Android control")
                }
            }
        }

        SettingsSection(title = "Account", subtitle = "Your account keeps your profile ready across sessions.") {
            Text(
                text = user.email ?: "Using Oeil on this device",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (user.email == null) {
                    "Sign in to connect an account."
                } else {
                    "Signed in as ${user.name}"
                },
                modifier = Modifier.padding(top = 4.dp),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            accountError?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            if (user.email == null) {
                Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign in or create account")
                }
            } else {
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign out")
                }
            }
        }

        SettingsSection(
            title = "AI service",
            subtitle = "Oeil connects to its service automatically; no API key or URL is needed.",
        ) {
            Text(
                text = if (serviceConfigured) "Connected build configuration" else "Not configured",
                color = if (serviceConfigured) GreenPrimary else TextSecondary,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (serviceConfigured) {
                    "The provider key stays on the Oeil service and is never on this device."
                } else {
                    "This build was compiled without a service address. Reinstall a build " +
                        "that includes the Oeil service, or use a developer build."
                },
                modifier = Modifier.padding(top = 4.dp),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(
            text = title,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = subtitle,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}
