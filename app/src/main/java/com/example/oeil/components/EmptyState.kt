package com.example.oeil.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.oeil.ui.theme.GreenPrimary
import com.example.oeil.ui.theme.OutlineDark
import com.example.oeil.ui.theme.SurfaceDark
import com.example.oeil.ui.theme.TextPrimary
import com.example.oeil.ui.theme.TextSecondary
import com.example.oeil.ui.theme.TealAccent
import java.util.Calendar

private data class Suggestion(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val prompt: String
)

private val suggestions = listOf(
    Suggestion(
        icon = Icons.Default.Image,
        title = "Analyze an image",
        subtitle = "Describe or explain what's in it",
        prompt = "Analyze this image for me"
    ),
    Suggestion(
        icon = Icons.Default.Code,
        title = "Write code",
        subtitle = "Build something new",
        prompt = "Write a Kotlin function that..."
    ),
    Suggestion(
        icon = Icons.Default.Lightbulb,
        title = "Brainstorm",
        subtitle = "Ideas and concepts",
        prompt = "Help me brainstorm ideas for..."
    ),
    Suggestion(
        icon = Icons.Default.Mic,
        title = "Voice chat",
        subtitle = "Speak naturally",
        prompt = "Let's have a voice conversation"
    )
)

/**
 * Landing view before the first message: a left-aligned, time-aware headline
 * followed by a horizontally scrolling strip of prompt cards.
 */
@Composable
fun EmptyState(
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = timeGreeting(),
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "What are we",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "exploring today?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Popular starting points",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        SuggestionStrip(onPick = onPick)
    }
}

@Composable
private fun SuggestionStrip(
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(suggestions) { suggestion ->
            SuggestionCard(
                suggestion = suggestion,
                onClick = { onPick(suggestion.prompt) }
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: Suggestion,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .width(172.dp)
            .clip(shape)
            .background(SurfaceDark)
            .border(1.dp, OutlineDark, shape)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            GreenPrimary.copy(alpha = 0.18f),
                            TealAccent.copy(alpha = 0.12f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = suggestion.icon,
                contentDescription = null,
                tint = GreenPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = suggestion.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = suggestion.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

private fun timeGreeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}
