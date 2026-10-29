package com.example.oeil.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.oeil.components.OeilTouch
import com.example.oeil.data.AuthRepository
import com.example.oeil.model.User
import com.example.oeil.ui.theme.BackgroundDark
import com.example.oeil.ui.theme.GreenPrimary
import com.example.oeil.ui.theme.OutlineDark
import com.example.oeil.ui.theme.SurfaceElevated
import com.example.oeil.ui.theme.TextOnAccent
import com.example.oeil.ui.theme.TextPrimary
import com.example.oeil.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private val ErrorRed = Color(0xFFFF7A7A)

/**
 * Sign-in and Sign-up gate supporting email/password as well as
 * official Google, Microsoft, and GitHub web OAuth redirects.
 */
@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLoggedIn: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSignUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val canSubmit = email.isNotBlank() && password.isNotBlank() && (!isSignUp || name.isNotBlank()) && !loading

    val submit: () -> Unit = {
        if (canSubmit) {
            error = null
            loading = true
            scope.launch {
                authRepository.login(email, password)
                    .onSuccess { user ->
                        loading = false
                        onLoggedIn(user)
                    }
                    .onFailure { failure ->
                        loading = false
                        error = failure.message?.takeIf { it.isNotBlank() }
                            ?: "Authentication failed. Please try again."
                    }
            }
        }
    }

    val launchOfficialOAuth: (String) -> Unit = { provider ->
        if (!loading) {
            error = null
            loading = true
            scope.launch {
                val redirectUri = "oeil://auth/callback"
                val url = when (provider) {
                    "Google" -> "https://accounts.google.com/o/oauth2/v2/auth?client_id=oeil-app&response_type=token&redirect_uri=$redirectUri&scope=email%20profile"
                    "Microsoft" -> "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=oeil-app&response_type=token&redirect_uri=$redirectUri&scope=user.read"
                    else -> "https://github.com/login/oauth/authorize?client_id=oeil-app&redirect_uri=$redirectUri&scope=user:email"
                }

                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }

                authRepository.socialLogin(provider, null)
                    .onSuccess { user ->
                        loading = false
                        onLoggedIn(user)
                    }
                    .onFailure { failure ->
                        loading = false
                        error = failure.message?.takeIf { it.isNotBlank() }
                            ?: "$provider sign in failed. Please try again."
                    }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated)
                        .border(1.dp, OutlineDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveRedEye,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isSignUp) "Create your account" else "Welcome back",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isSignUp) {
                        "Sign up to start chatting with Oeil."
                    } else {
                        "Sign in to keep your conversations with Oeil."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Mode Selector Tab (Sign In / Sign Up)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, OutlineDark, RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    TabButton(
                        text = "Sign in",
                        selected = !isSignUp,
                        onClick = { isSignUp = false },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = "Sign up",
                        selected = isSignUp,
                        onClick = { isSignUp = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isSignUp) {
                    AuthField(
                        label = "Full Name",
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "Alex Johnson",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                AuthField(
                    label = "Email",
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "you@example.com",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                AuthField(
                    label = "Password",
                    value = password,
                    onValueChange = { password = it },
                    placeholder = if (isSignUp) "At least 4 characters" else "Your password",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailing = {
                        OeilTouch(
                            onClick = { showPassword = !showPassword },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (showPassword) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = if (showPassword) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                },
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorRed
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                OeilTouch(
                    onClick = submit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (canSubmit) GreenPrimary else SurfaceElevated,
                    borderColor = if (canSubmit) Color.Transparent else OutlineDark
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = TextOnAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isSignUp) "Create account" else "Sign in",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (canSubmit) TextOnAccent else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(OutlineDark)
                    )
                    Text(
                        text = "Or continue with",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(OutlineDark)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Official OAuth Web Redirects (Google, Microsoft, GitHub)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SocialButton(
                        provider = "Google",
                        badge = "G",
                        badgeColor = Color(0xFFEA4335),
                        onClick = { launchOfficialOAuth("Google") },
                        modifier = Modifier.weight(1f)
                    )
                    SocialButton(
                        provider = "Microsoft",
                        badge = "M",
                        badgeColor = Color(0xFF00A4EF),
                        onClick = { launchOfficialOAuth("Microsoft") },
                        modifier = Modifier.weight(1f)
                    )
                    SocialButton(
                        provider = "GitHub",
                        badge = "GH",
                        badgeColor = Color(0xFF24292E),
                        onClick = { launchOfficialOAuth("GitHub") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (authRepository.isDemoMode) {
                        "Demo mode active: offline login and web OAuth sign-in work out of the box."
                    } else {
                        "Securely connected to backend auth."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    OeilTouch(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = shape,
        color = if (selected) GreenPrimary else Color.Transparent
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (selected) TextOnAccent else TextSecondary
            )
        }
    }
}

@Composable
private fun SocialButton(
    provider: String,
    badge: String,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    OeilTouch(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = shape,
        color = SurfaceElevated,
        borderColor = OutlineDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = provider,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(SurfaceElevated)
                .border(1.dp, OutlineDark, shape)
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 15.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = SolidColor(TextPrimary),
                singleLine = true,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                        innerTextField()
                    }
                }
            )

            trailing?.invoke()
        }
    }
}
