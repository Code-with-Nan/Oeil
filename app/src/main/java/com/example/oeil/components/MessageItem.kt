package com.example.oeil.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.oeil.model.ChatMessage
import com.example.oeil.ui.theme.SurfaceElevated
import com.example.oeil.ui.theme.TextPrimary
import com.example.oeil.ui.theme.TextSecondary

private val UserBubbleShape = RoundedCornerShape(20.dp)

/**
 * Flat treatment: user turns are right-aligned neutral bubbles, assistant
 * turns are plain left-aligned text with no chrome.
 * Long-press either one to copy it.
 */
@Composable
fun MessageItem(
    message: ChatMessage,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val copyable = Modifier.combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = LocalIndication.current,
        onLongClick = { onCopy(message.content) },
        onClick = {}
    )

    if (message.isUser) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End
        ) {
            UserBubble(text = message.content, attachmentLabel = message.attachmentLabel, extra = copyable)
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .then(copyable)
        ) {
            Text(
                text = message.content,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun UserBubble(
    text: String,
    attachmentLabel: String?,
    extra: Modifier
) {
    Box(
        modifier = Modifier
            .widthIn(max = 320.dp)
            .clip(UserBubbleShape)
            .background(SurfaceElevated)
            .then(extra)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        BubbleContent(text = text, attachmentLabel = attachmentLabel)
    }
}

/** Attachment label rides above the text so an image-only message still reads. */
@Composable
private fun BubbleContent(text: String, attachmentLabel: String?) {
    val body = text.takeIf { it.isNotBlank() && it != attachmentLabel }

    Column {
        if (attachmentLabel != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = attachmentLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            if (body != null) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
        if (body != null) {
            Text(
                text = body,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
