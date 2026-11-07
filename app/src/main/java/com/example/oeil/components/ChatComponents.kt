package com.example.oeil.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.oeil.model.ChatMessage
import com.example.oeil.ui.theme.AccentGradient
import com.example.oeil.ui.theme.AccentGradientSoft
import com.example.oeil.ui.theme.AccentPrimary
import com.example.oeil.ui.theme.OutlineDark
import com.example.oeil.ui.theme.SurfaceGlass
import com.example.oeil.ui.theme.SurfaceElevated
import com.example.oeil.ui.theme.TextPrimary
import com.example.oeil.ui.theme.TextSecondary
import com.example.oeil.ui.theme.UserBubbleGradient
import java.util.Calendar

/** Tappable surface with a ripple, spring squeeze and slight dim. */
@Composable
fun OeilTouch(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    color: Color = Color.Transparent,
    brush: Brush? = null,
    borderColor: Color = Color.Transparent,
    borderBrush: Brush? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "press",
    )
    val touched = if (onLongClick == null) {
        Modifier.clickable(interaction, LocalIndication.current, onClick = onClick)
    } else {
        Modifier.combinedClickable(interaction, LocalIndication.current, onLongClick = onLongClick, onClick = onClick)
    }
    val fill = if (brush != null) Modifier.background(brush) else Modifier.background(color)
    val edge = when {
        borderBrush != null -> Modifier.border(1.dp, borderBrush, shape)
        borderColor != Color.Transparent -> Modifier.border(1.dp, borderColor, shape)
        else -> Modifier
    }

    Box(
        modifier = modifier
            .scale(1f - 0.06f * press)
            .alpha(1f - 0.12f * press)
            .clip(shape)
            .then(fill)
            .then(edge)
            .then(touched),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

private val UserShape = RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp)
private val AssistantShape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)

/** User turns get a cold gradient bubble; assistant turns sit on frosted glass. Long-press to copy. */
@Composable
fun MessageItem(message: ChatMessage, onCopy: (String) -> Unit, modifier: Modifier = Modifier) {
    val copyable = Modifier.combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = LocalIndication.current,
        onLongClick = { onCopy(message.content) },
        onClick = {},
    )
    val isUser = message.isUser

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 336.dp)
                .clip(if (isUser) UserShape else AssistantShape)
                .then(
                    if (isUser) {
                        Modifier.background(UserBubbleGradient)
                    } else {
                        Modifier
                            .background(SurfaceGlass.copy(alpha = 0.62f))
                            .border(1.dp, OutlineDark, AssistantShape)
                    }
                )
                .then(copyable)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            message.attachmentLabel?.let { label ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Description,
                        null,
                        tint = if (isUser) Color.White.copy(alpha = 0.82f) else TextSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isUser) Color.White.copy(alpha = 0.82f) else TextSecondary,
                        maxLines = 1,
                    )
                }
                if (message.content.isNotBlank() && message.content != label) Spacer(Modifier.height(6.dp))
            }
            if (message.content.isNotBlank() && message.content != message.attachmentLabel) {
                Text(
                    message.content,
                    color = if (isUser) Color.White else TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

/** Pending-reply row: a frosted pill with three pulsing accent dots. */
@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    val pillShape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        Row(
            modifier = Modifier
                .clip(pillShape)
                .background(SurfaceGlass.copy(alpha = 0.62f))
                .border(1.dp, OutlineDark, pillShape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { index ->
                val alpha by transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = index * 150, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "dot_$index",
                )
                Box(
                    Modifier
                        .size(6.dp)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(AccentGradient)
                )
            }
        }
    }
}

private data class Suggestion(val icon: ImageVector, val label: String, val prompt: String)

private val suggestions = listOf(
    Suggestion(Icons.Outlined.ChatBubbleOutline, "Start a topic", "Explain something I've always faked understanding"),
    Suggestion(Icons.Outlined.Lightbulb, "Brainstorm ideas", "Help me brainstorm ideas for"),
    Suggestion(Icons.Outlined.AutoAwesome, "Write code", "Write a Kotlin function that"),
)

/** Landing view before the first message: greeting and three starter prompts. */
@Composable
fun EmptyState(onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(AccentGradientSoft)
                .border(1.dp, OutlineDark, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = AccentPrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(greeting(), style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        Text(
            "What's on your mind?",
            style = MaterialTheme.typography.headlineSmall.copy(brush = AccentGradient),
            fontWeight = FontWeight.Normal,
        )
        Spacer(Modifier.height(28.dp))
        suggestions.forEach { SuggestionRow(it) { onPick(it.prompt) } }
        Spacer(Modifier.weight(1f))
        Text(
            "Long-press a message to copy it",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp),
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
private fun SuggestionRow(suggestion: Suggestion, onClick: () -> Unit) {
    OeilTouch(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceElevated.copy(alpha = 0.55f),
        borderColor = OutlineDark,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(AccentGradientSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(suggestion.icon, null, tint = AccentPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(suggestion.label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                null,
                tint = AccentPrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
