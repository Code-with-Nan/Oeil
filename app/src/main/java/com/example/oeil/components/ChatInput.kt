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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MicNone
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.oeil.model.Attachment
import com.example.oeil.ui.theme.BackgroundDark
import com.example.oeil.ui.theme.OutlineBright
import com.example.oeil.ui.theme.OutlineDark
import com.example.oeil.ui.theme.SurfaceElevated
import com.example.oeil.ui.theme.TextPrimary
import com.example.oeil.ui.theme.TextSecondary

private enum class TrailingControl {
    Stop,
    Send,
    Mic
}

/**
 * Rounded composer: attachment chip, multi-line text field and one trailing
 * control that answers to state — a quiet mic, a filled send disc once there is
 * text, or a stop square while a reply is in flight.
 *
 * Voice input and the photo picker are real system intents. This build has no
 * speech-to-text or vision backend behind them, so a transcript becomes draft
 * text and a picked image becomes an attachment label the mock can only echo.
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
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(26.dp)
    val inputInteraction = remember { MutableInteractionSource() }
    val focused by inputInteraction.collectIsFocusedAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onAttachmentPicked(Attachment(uri, displayName(context, uri)))
    }

    val voiceSession = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val transcript = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        when {
            result.resultCode == Activity.RESULT_OK && !transcript.isNullOrBlank() ->
                onVoiceTranscript(transcript)

            result.resultCode != Activity.RESULT_CANCELED ->
                onNotice("Didn't catch that. Try again.")
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (attachment != null) {
            AttachmentChip(attachment = attachment, onRemove = onAttachmentRemoved)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(shape)
                .background(SurfaceElevated)
                .border(
                    width = 1.dp,
                    color = if (focused) OutlineBright else OutlineDark,
                    shape = shape
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OeilTouch(
                onClick = {
                    runCatching {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }.onFailure { onNotice("No app on this device can pick an image.") }
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Attach an image",
                    tint = TextPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp)
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = SolidColor(TextPrimary),
                interactionSource = inputInteraction,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (canSend) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSend()
                        }
                    }
                ),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = "Ask Oeil anything",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.width(4.dp))

            Crossfade(targetState = trailingFor(canSend, isTyping), label = "composerAction") { control ->
                when (control) {
                    TrailingControl.Send -> SendDisc(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSend()
                        }
                    )

                    TrailingControl.Stop -> StopDisc(onClick = onStop)

                    TrailingControl.Mic -> OeilTouch(
                        onClick = {
                            val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Oeil")
                            }
                            runCatching { voiceSession.launch(request) }
                                .onFailure { onNotice("Voice input isn't available on this device.") }
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MicNone,
                            contentDescription = "Dictate a message",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun trailingFor(canSend: Boolean, isTyping: Boolean): TrailingControl = when {
    isTyping -> TrailingControl.Stop
    canSend -> TrailingControl.Send
    else -> TrailingControl.Mic
}

@Composable
private fun AttachmentChip(attachment: Attachment, onRemove: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(SurfaceElevated)
            .border(1.dp, OutlineDark, shape)
            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = attachment.label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 1
        )
        OeilTouch(onClick = onRemove, modifier = Modifier.size(26.dp)) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove attachment",
                tint = TextSecondary,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun SendDisc(onClick: () -> Unit) {
    OeilTouch(
        onClick = onClick,
        modifier = Modifier.size(38.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(TextPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Send,
                contentDescription = "Send",
                tint = BackgroundDark,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun StopDisc(onClick: () -> Unit) {
    OeilTouch(onClick = onClick, modifier = Modifier.size(38.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(SurfaceElevated)
                .border(1.dp, OutlineBright, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(TextPrimary)
            )
        }
    }
}

/** Human-readable name for a picked image, falling back when the provider hides it. */
private fun displayName(context: Context, uri: Uri): String {
    val queried = runCatching {
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull()

    return queried?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Image"
}
