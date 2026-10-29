package com.example.oeil.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.oeil.ui.theme.OutlineDark
import com.example.oeil.ui.theme.SurfaceDrawerItem
import com.example.oeil.ui.theme.SurfaceElevated
import com.example.oeil.ui.theme.TextPrimary
import com.example.oeil.ui.theme.TextSecondary
import java.util.Calendar

private data class Suggestion(
    val icon: ImageVector,
    val label: String,
    val prompt: String
)

private val suggestions = listOf(
    Suggestion(
        icon = Icons.Outlined.ChatBubbleOutline,
        label = "Start a topic",
        prompt = "Explain something I've always faked understanding"
    ),
    Suggestion(
        icon = Icons.Outlined.Lightbulb,
        label = "Brainstorm ideas",
        prompt = "Help me brainstorm ideas for"
    ),
    Suggestion(
        icon = Icons.Outlined.AutoAwesome,
        label = "Write code",
        prompt = "Write a Kotlin function that"
    )
)

/**
 * Landing view before the first message: one plain line of greeting and three
 * prompts that pre-fill the composer and raise the keyboard.
 */
@Composable
fun EmptyState(
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = greeting(),
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "What's on your mind?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Normal,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(28.dp))

        suggestions.forEach { suggestion ->
            SuggestionRow(
                suggestion = suggestion,
                onClick = { onPick(suggestion.prompt) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Long-press a message to copy it",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 5 -> "Still up"
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
}

@Composable
private fun SuggestionRow(
    suggestion: Suggestion,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    OeilTouch(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = shape,
        color = SurfaceElevated,
        borderColor = OutlineDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconChip(icon = suggestion.icon)

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = suggestion.label,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun IconChip(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(SurfaceDrawerItem),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}
