package com.example.oeil.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.oeil.ui.theme.GreenPrimary
import com.example.oeil.ui.theme.OutlineDark
import com.example.oeil.ui.theme.SurfaceDark
import com.example.oeil.ui.theme.TealAccent

/**
 * Callout container used for everything Oeil says: surface background,
 * hairline outline and a 3dp gradient rail on the start edge.
 *
 * [IntrinsicSize.Min] makes the rail match the content height exactly.
 */
@Composable
internal fun AccentPanel(
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = modifier
            .widthIn(max = 300.dp)
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(SurfaceDark)
            .border(1.dp, OutlineDark, shape)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(Brush.verticalGradient(listOf(GreenPrimary, TealAccent)))
        )
        Column(
            modifier = Modifier.padding(start = 14.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            content = content
        )
    }
}
