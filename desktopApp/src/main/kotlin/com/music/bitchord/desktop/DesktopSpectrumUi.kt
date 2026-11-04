package com.music.bitchord.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

internal enum class SpectrumSection { FULL, LEFT_HALF, RIGHT_HALF }

/** Shared live spectrum; the bottom player's two panes divide the same bands around its controls. */
@Composable
internal fun SpectrumPane(
    tint: Color,
    modifier: Modifier = Modifier,
    section: SpectrumSection = SpectrumSection.FULL,
) {
    val enabled by DesktopAppearanceSettings.visualizer.collectAsState()
    if (!enabled) return
    val bands = DesktopAudioSpectrum.bands.collectAsState()
    Canvas(modifier) {
        val levels = bands.value
        val midpoint = levels.size / 2
        val start = if (section == SpectrumSection.RIGHT_HALF) midpoint else 0
        val end = if (section == SpectrumSection.LEFT_HALF) midpoint else levels.size
        val count = end - start
        if (count == 0 || size.width <= 0f || size.height <= 0f) return@Canvas
        val step = size.width / count
        val barWidth = step * 0.62f
        val margin = (step - barWidth) / 2f
        val maxHeight = size.height * 0.86f
        for (index in 0 until count) {
            val level = levels[start + index]
            if (level <= 0.005f) continue
            val height = maxHeight * level
            val top = size.height - height
            val alpha = 0.10f + 0.42f * level
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(tint.copy(alpha = alpha * 0.25f), tint.copy(alpha = alpha)),
                    startY = top,
                    endY = size.height,
                ),
                topLeft = Offset(index * step + margin, top),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}
