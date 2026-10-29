package com.example.oeil.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oeil.model.ReplyMode
import com.example.oeil.ui.theme.OutlineDark
import com.example.oeil.ui.theme.SurfaceElevated
import com.example.oeil.ui.theme.TextPrimary
import com.example.oeil.ui.theme.TextSecondary

/**
 * Flat top bar: menu, wordmark, answer-style selector, new chat. Nothing here
 * reports status — a quiet row of plain controls is the whole design.
 */
@Composable
fun ChatHeader(
    onMenuClick: () -> Unit,
    onNewChat: () -> Unit,
    mode: ReplyMode,
    onModeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlainIconButton(
            icon = Icons.Outlined.Menu,
            contentDescription = "Open menu",
            onClick = onMenuClick
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = "Oeil",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.width(10.dp))

        ModeSelector(mode = mode, onClick = onModeClick)

        Spacer(modifier = Modifier.weight(1f))

        PlainIconButton(
            icon = Icons.Outlined.Edit,
            contentDescription = "Start a new chat",
            onClick = onNewChat
        )
    }
}

@Composable
private fun ModeSelector(mode: ReplyMode, onClick: () -> Unit) {
    OeilTouch(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        color = SurfaceElevated.copy(alpha = 0.65f),
        borderColor = OutlineDark
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mode.label,
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.UnfoldMore,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun PlainIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    OeilTouch(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextSecondary,
            modifier = Modifier.size(21.dp)
        )
    }
}
