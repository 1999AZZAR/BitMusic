package com.music.bitchord.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SoftHorizontalDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
) {
    Spacer(
        modifier.fillMaxWidth().height(0.5.dp).drawWithCache {
            val brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.15f to color,
                0.85f to color,
                1f to Color.Transparent,
            )
            onDrawBehind { drawRect(brush) }
        },
    )
}

@Composable
fun SoftVerticalDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
) {
    Spacer(
        modifier.fillMaxHeight().width(0.5.dp).drawWithCache {
            val brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.15f to color,
                0.85f to color,
                1f to Color.Transparent,
            )
            onDrawBehind { drawRect(brush) }
        },
    )
}
