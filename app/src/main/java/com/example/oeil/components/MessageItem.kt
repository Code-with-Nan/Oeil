package com.example.oeil.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.oeil.model.ChatMessage
import com.example.oeil.ui.theme.GradientEnd
import com.example.oeil.ui.theme.GradientStart
import com.example.oeil.ui.theme.GreenPrimary
import com.example.oeil.ui.theme.OutlineDark
import com.example.oeil.ui.theme.SurfaceDark
import com.example.oeil.ui.theme.SurfaceElevated
import com.example.oeil.ui.theme.TealAccent
import com.example.oeil.ui.theme.TextOnAccent
import com.example.oeil.ui.theme.TextPrimary
import com.example.oeil.ui.theme.TextSecondary

private val UserBubbleShape = RoundedCornerShape(
    topStart = 22.dp,
    topEnd = 6.dp,
    bottomStart = 22.dp,
    bottomEnd = 22.dp
)

private val AssistantBubbleShape = RoundedCornerShape(
    topStart = 6.dp,
    topEnd = 18.dp,
    bottomStart = 6.dp,
    bottomEnd = 18.dp
)

/**
 * Two visual treatments: user messages are gradient pills (familiar),
 * assistant answers are callout panels with a coloured accent rail.
 */
@Composable
fun MessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.isUser

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            AssistantAvatar()
            Spacer(modifier = Modifier.width(10.dp))
            AssistantBubble(text = message.content)
        } else {
            UserBubble(text = message.content)
            Spacer(modifier = Modifier.width(10.dp))
            UserAvatar()
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Box(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .clip(UserBubbleShape)
            .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = TextOnAccent,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AssistantBubble(text: String) {
    AccentPanel(shape = AssistantBubbleShape) {
        Text(
            text = text,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun AssistantAvatar() {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(GradientStart, TealAccent))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.RemoveRedEye,
            contentDescription = "Oeil",
            tint = TextOnAccent,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun UserAvatar() {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(SurfaceElevated)
            .border(1.dp, GreenPrimary.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "You",
            tint = TextSecondary,
            modifier = Modifier.size(17.dp)
        )
    }
}
