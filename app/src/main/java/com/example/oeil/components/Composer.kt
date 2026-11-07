package com.example.oeil.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oeil.model.Attachment
import com.example.oeil.model.ReplyMode
import com.example.oeil.ui.theme.BackgroundDark
import com.example.oeil.ui.theme.AccentGradient
import com.example.oeil.ui.theme.AccentGradientSoft
import com.example.oeil.ui.theme.AccentPrimary
import com.example.oeil.ui.theme.DividerGradient
import com.example.oeil.ui.theme.OutlineBright
import com.example.oeil.ui.theme.OutlineDark
import com.example.oeil.ui.theme.SurfaceElevated
import com.example.oeil.ui.theme.SurfaceGlass
import com.example.oeil.ui.theme.TextPrimary
import com.example.oeil.ui.theme.TextSecondary

/** Flat top bar: menu, wordmark, answer style, Android control and new chat. */
@Composable
fun ChatHeader(
    onMenuClick: () -> Unit,
    onNewChat: () -> Unit,
    mode: ReplyMode,
    onModeClick: () -> Unit,
    androidMode: Boolean,
    accessibilityConnected: Boolean,
    onAndroidClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlainIcon(Icons.Outlined.Menu, "Open menu", onMenuClick)
            Spacer(Modifier.width(6.dp))
            Text(
                "Oeil",
                style = MaterialTheme.typography.titleMedium.copy(
                    brush = AccentGradient,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                ),
            )
            Spacer(Modifier.width(10.dp))
            ModeSelector(mode, onModeClick)
            Spacer(Modifier.width(4.dp))
            OeilTouch(
                onClick = onAndroidClick,
                modifier = Modifier.size(36.dp),
                color = if (androidMode) Color.Transparent else SurfaceElevated.copy(alpha = 0.5f),
                brush = if (androidMode) AccentGradientSoft else null,
                borderColor = if (androidMode) AccentPrimary.copy(alpha = 0.45f) else OutlineDark,
                borderBrush = null,
            ) {
                Icon(
                    Icons.Outlined.PhoneAndroid,
                    when {
                        !accessibilityConnected -> "Enable Android control"
                        androidMode -> "Turn Android control off"
                        else -> "Turn Android control on"
                    },
                    tint = if (androidMode) AccentPrimary else TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            PlainIcon(Icons.Outlined.Edit, "Start a new chat", onNewChat)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(DividerGradient))
    }
}

@Composable
private fun ModeSelector(mode: ReplyMode, onClick: () -> Unit) {
    OeilTouch(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        color = SurfaceElevated.copy(alpha = 0.65f),
        borderColor = OutlineDark,
    ) {
        Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(mode.label, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.UnfoldMore, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun PlainIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    OeilTouch(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(icon, description, tint = TextSecondary, modifier = Modifier.size(21.dp))
    }
}

private enum class Trailing { Stop, Send, Mic }

/**
 * Composer with an attachment chip, multi-line field and one state-aware trailing
 * control: mic, send, or stop while a reply is in flight.
 */
@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    canSend: Boolean,
    isTyping: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    attachment: Attachment?,
    onAttachmentPicked: (Attachment) -> Unit,
    onAttachmentRemoved: () -> Unit,
    onVoiceTranscript: (String) -> Unit,
    onNotice: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(26.dp)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onAttachmentPicked(Attachment(uri, displayName(context, uri)))
    }
    val voiceSession = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val transcript = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        when {
            result.resultCode == Activity.RESULT_OK && !transcript.isNullOrBlank() -> onVoiceTranscript(transcript)
            result.resultCode != Activity.RESULT_CANCELED -> onNotice("Didn't catch that. Try again.")
        }
    }

    Column(modifier.fillMaxWidth()) {
        if (attachment != null) {
            AttachmentChip(attachment, onAttachmentRemoved)
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(shape)
                .background(if (focused) SurfaceGlass.copy(alpha = 0.9f) else SurfaceElevated)
                .border(1.dp, if (focused) AccentPrimary.copy(alpha = 0.55f) else OutlineDark, shape)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OeilTouch(
                onClick = {
                    runCatching {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }.onFailure { onNotice("No app on this device can pick an image.") }
                },
                modifier = Modifier.size(38.dp),
            ) {
                Icon(Icons.Outlined.Add, "Attach an image", tint = TextPrimary.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester).padding(horizontal = 8.dp, vertical = 8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = SolidColor(TextPrimary),
                interactionSource = interaction,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (canSend) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSend()
                        }
                    }
                ),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text("Ask Oeil anything", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
                        }
                        inner()
                    }
                },
            )

            Spacer(Modifier.width(4.dp))

            Crossfade(
                targetState = when {
                    isTyping -> Trailing.Stop
                    canSend -> Trailing.Send
                    else -> Trailing.Mic
                },
                label = "composer",
            ) { control ->
                when (control) {
                    Trailing.Send -> SendDisc {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSend()
                    }

                    Trailing.Stop -> StopDisc(onStop)

                    Trailing.Mic -> OeilTouch(
                        onClick = {
                            val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Oeil")
                            }
                            runCatching { voiceSession.launch(request) }
                                .onFailure { onNotice("Voice input isn't available on this device.") }
                        },
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(Icons.Outlined.MicNone, "Dictate a message", tint = TextSecondary, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentChip(attachment: Attachment, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceGlass.copy(alpha = 0.7f))
            .border(1.dp, OutlineDark, RoundedCornerShape(16.dp))
            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.Description, null, tint = TextSecondary, modifier = Modifier.size(15.dp))
        Text(attachment.label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1)
        OeilTouch(onClick = onRemove, modifier = Modifier.size(26.dp)) {
            Icon(Icons.Outlined.Close, "Remove attachment", tint = TextSecondary, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun SendDisc(onClick: () -> Unit) {
    OeilTouch(onClick = onClick, modifier = Modifier.size(38.dp)) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(AccentGradient), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.Send, "Send", tint = BackgroundDark, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun StopDisc(onClick: () -> Unit) {
    OeilTouch(onClick = onClick, modifier = Modifier.size(38.dp)) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(SurfaceElevated).border(1.dp, OutlineBright, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(TextPrimary))
        }
    }
}

private fun displayName(context: Context, uri: Uri): String {
    val queried = runCatching {
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull()
    return queried?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Image"
}
