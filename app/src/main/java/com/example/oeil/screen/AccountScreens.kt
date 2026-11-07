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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.oeil.model.User
import com.example.oeil.ui.theme.BackgroundDark
import com.example.oeil.ui.theme.AccentPrimary
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
        Spacer(Modifier.height(8.dp))
        Text("Settings", color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.headlineMedium)
        Text(
            "Make Oeil feel like yours and manage your account.",
            modifier = Modifier.padding(top = 6.dp),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )

        Section("Appearance", "Choose how Oeil looks on this device.") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                themeOptions.forEach { option ->
                    if (themeMode == option) {
                        Button(onClick = { onThemeModeChange(option) }, modifier = Modifier.weight(1f)) { Text(option) }
                    } else {
                        OutlinedButton(onClick = { onThemeModeChange(option) }, modifier = Modifier.weight(1f)) { Text(option) }
                    }
                }
            }
        }

        Section("System control", "Let Oeil read the screen and perform actions you approve.") {
            Text(
                if (accessibilityConnected) "Android control is ready." else "Android control is off.",
                color = if (accessibilityConnected) AccentPrimary else TextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (accessibilityConnected) {
                    "Ask Oeil to open apps, tap controls, type text, scroll, or navigate."
                } else {
                    "Turn on Oeil in Android Accessibility settings. Every action is approved in a system overlay before it runs."
                },
                modifier = Modifier.padding(top = 4.dp),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(14.dp))
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

        Section("Account", "Your account keeps your profile ready across sessions.") {
            Text(user.email ?: "Using Oeil on this device", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(
                if (user.email == null) "Sign in to connect an account." else "Signed in as ${user.name}",
                modifier = Modifier.padding(top = 4.dp),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            accountError?.let {
                Text(
                    it,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(14.dp))
            if (user.email == null) {
                Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) { Text("Sign in or create account") }
            } else {
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
            }
        }

        Section("AI service", "Oeil connects to its service automatically; no API key or URL is needed.") {
            Text(
                if (serviceConfigured) "Connected build configuration" else "Not configured",
                color = if (serviceConfigured) AccentPrimary else TextSecondary,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (serviceConfigured) {
                    "The provider key stays on the Oeil service and is never on this device."
                } else {
                    "This build was compiled without a service address. Reinstall a build that includes the Oeil service, or use a developer build."
                },
                modifier = Modifier.padding(top = 4.dp),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 24.dp)) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleLarge)
        Text(
            subtitle,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
fun AuthScreen(
    busy: Boolean,
    externalError: String?,
    onBack: () -> Unit,
    onSubmit: (name: String, email: String, password: String, signUp: Boolean) -> Unit,
    onOAuth: (provider: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var signUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        TextButton(onClick = onBack, enabled = !busy) { Text("Back to settings", color = TextSecondary) }
        Spacer(Modifier.height(30.dp))
        Text(
            if (signUp) "Create your account" else "Welcome back",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            if (signUp) "One account for your Oeil experience." else "Sign in to your Oeil account.",
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )

        if (signUp) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true,
                enabled = !busy,
            )
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
            enabled = !busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            enabled = !busy,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Spacer(Modifier.height(16.dp))
        externalError?.let {
            Text(
                it,
                modifier = Modifier.padding(bottom = 12.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Button(
            onClick = { onSubmit(name.trim(), email.trim(), password, signUp) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy && email.isNotBlank() && password.length >= 8 && (!signUp || name.isNotBlank()),
        ) {
            if (busy) {
                CircularProgressIndicator(color = AccentPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Text(if (signUp) "Create account" else "Sign in")
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
            Text(
                if (signUp) "Already have an account?" else "New to Oeil?",
                color = TextSecondary,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = { signUp = !signUp }, enabled = !busy) {
                Text(if (signUp) "Sign in" else "Create account")
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "OR CONTINUE WITH",
            modifier = Modifier.padding(bottom = 12.dp),
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
        listOf("Google" to "google", "Microsoft" to "microsoft", "GitHub" to "github").forEach { (label, provider) ->
            OutlinedButton(
                onClick = { onOAuth(provider) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                enabled = !busy,
            ) {
                Text("Continue with $label")
            }
        }
    }
}
