package com.example.oeil.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Cool palette: deep teal-black with an emerald-to-cyan accent blend.
private val Emerald = Color(0xFF34D399)
private val Cyan = Color(0xFF22D3EE)
private val Night = Color(0xFF040B0A)
private val DeepTeal = Color(0xFF07110F)
private val Slate = Color(0xFF0B1D1A)
private val TealLine = Color(0xFF1A302B)
private val Frost = Color(0xFFE6FAF3)
private val Mist = Color(0xFF8FB0A8)
private val Ink = Color(0xFF03120E)

private val DarkScheme = darkColorScheme(
    primary = Emerald,
    onPrimary = Ink,
    primaryContainer = Color(0xFF0C3B31),
    onPrimaryContainer = Color(0xFFB6F5E2),
    secondary = Cyan,
    onSecondary = Color(0xFF032027),
    secondaryContainer = Color(0xFF0C2A28),
    onSecondaryContainer = Color(0xFFCFFAF1),
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF04211C),
    background = Night,
    onBackground = Frost,
    surface = DeepTeal,
    onSurface = Frost,
    surfaceVariant = Slate,
    onSurfaceVariant = Mist,
    outline = TealLine,
    outlineVariant = Color(0xFF28453E),
    surfaceContainerLowest = Color(0xFF020706),
    surfaceContainerLow = Color(0xFF081512),
    surfaceContainer = Slate,
    surfaceContainerHigh = Color(0xFF112824),
    surfaceContainerHighest = Color(0xFF17332E),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF047857),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF00251A),
    secondary = Color(0xFF0E7490),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF003640),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color.White,
    background = Color(0xFFF4FBF8),
    onBackground = Color(0xFF0B1714),
    surface = Color(0xFFF4FBF8),
    onSurface = Color(0xFF0B1714),
    surfaceVariant = Color(0xFFE0F2EC),
    onSurfaceVariant = Color(0xFF3F5A53),
    outline = Color(0xFF6E8B82),
    outlineVariant = Color(0xFFBBD6CD),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEEF8F4),
    surfaceContainer = Color(0xFFE5F4EE),
    surfaceContainerHigh = Color(0xFFDBEFE8),
    surfaceContainerHighest = Color(0xFFD1EAE1),
)

private val OeilTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.3.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
)

val AccentPrimary: Color @Composable get() = MaterialTheme.colorScheme.primary
val BackgroundDark: Color @Composable get() = MaterialTheme.colorScheme.background
val SurfaceElevated: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainer
val SurfaceGlass: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
val SurfaceDrawer: Color @Composable get() = MaterialTheme.colorScheme.surface
val OutlineDark: Color @Composable get() = MaterialTheme.colorScheme.outline
val OutlineBright: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
val TextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onBackground
val TextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val TextOnAccent: Color @Composable get() = MaterialTheme.colorScheme.onPrimary

/** Emerald to cyan, used for active controls and brand marks. */
val AccentGradient: Brush
    @Composable get() = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    )

/** Soft accent wash for selected rows and icon chips. */
val AccentGradientSoft: Brush
    @Composable get() = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
        )
    )

/** Deep emerald to cyan, used for user message bubbles. */
val UserBubbleGradient: Brush
    @Composable get() = Brush.linearGradient(
        listOf(Color(0xFF059669), Color(0xFF0E7490))
    )

/** Near-black teal wash behind the conversation. */
val ChatBackgroundGradient: Brush
    @Composable get() = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceContainerLowest,
        )
    )

/** Faint emerald glow that fades from the top of the chat. */
val TopGlowGradient: Brush
    @Composable get() = Brush.verticalGradient(
        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), Color.Transparent)
    )

/** Hairline divider that fades at both ends. */
val DividerGradient: Brush
    @Composable get() = Brush.horizontalGradient(
        listOf(
            Color.Transparent,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
            Color.Transparent,
        )
    )

@Composable
fun OeilTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = OeilTypography,
        content = content,
    )
}
