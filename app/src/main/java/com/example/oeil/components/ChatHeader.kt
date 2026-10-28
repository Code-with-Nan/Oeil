package com.example.oeil.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

private const val BRAND_NAME = "Oeil"
private const val BRAND_TAGLINE = "Make it do what you want"
private const val STATUS_LABEL = "Ready"

/**
 * Solid header band: brand, live status pill, and quick actions.
 * Sits above a hairline gradient so it reads as a chrome layer,
 * distinct from the gradient conversation canvas below.
 */
@Composable
fun ChatHeader(
    onNewChat: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandLogo()

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = BRAND_NAME,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))

                }
                Text(
                    text = BRAND_TAGLINE,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            HeaderAction(
                icon = Icons.Outlined.AddComment,
                contentDescription = "Start a new chat",
                onClick = onNewChat
            )
            Spacer(modifier = Modifier.width(6.dp))
            HeaderAction(
                icon = Icons.Default.Person,
                contentDescription = "Profile",
                onClick = onProfile
            )
        }

        HairlineDivider()
    }
}

@Composable
private fun BrandLogo() {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(GradientStart, GradientEnd))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.RemoveRedEye,
            contentDescription = "Oeil",
            tint = TextOnAccent,
            modifier = Modifier.size(22.dp)
        )
    }
}
@Composable
private fun HeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(SurfaceElevated)
            .border(1.dp, OutlineDark, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextSecondary,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun HairlineDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        GreenPrimary.copy(alpha = 0.45f),
                        TealAccent.copy(alpha = 0.25f),
                        Color.Transparent
                    )
                )
            )
    )
}
