package com.example.oeil.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(30.dp)
    val canSend = enabled && value.isNotBlank()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(shape)
            .background(SurfaceDark)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        GreenPrimary.copy(alpha = 0.55f),
                        TealAccent.copy(alpha = 0.25f),
                        OutlineDark
                    )
                ),
                shape = shape
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = SolidColor(GreenPrimary),
                maxLines = 5,
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = "Ask me anything...",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        innerTextField()
                    }
                }
            )

            AnimatedVisibility(
                visible = canSend,
                enter = expandHorizontally(),
                exit = shrinkHorizontally()
            ) {
                SendPill(onClick = onSend)
            }

            AnimatedVisibility(
                visible = !canSend,
                enter = expandHorizontally(),
                exit = shrinkHorizontally()
            ) {
                SendIdleButton()
            }
        }
    }
}

/**
 * Ready state: a wide gradient button that expands out of the composer
 * as soon as there is something to send.
 */
@Composable
private fun SendPill(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Send",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = TextOnAccent
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = null,
            tint = TextOnAccent,
            modifier = Modifier.size(17.dp)
        )
    }
}

/** Idle state: a quiet circular button with no affordance to press. */
@Composable
private fun SendIdleButton() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(SurfaceElevated),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            tint = TextSecondary,
            modifier = Modifier.size(19.dp)
        )
    }
}
