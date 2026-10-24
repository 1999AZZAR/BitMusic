package com.music.bitchord.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.music.bitchord.ui.graphics.ColorUtils
import com.music.bitchord.ui.theme.ArtworkPalette
import com.music.bitchord.ui.theme.rememberArtworkPalette

/**
 * Material You on the desktop: the chrome follows the record that is playing.
 *
 * The app's own scheme is Spotify's near-black with a white accent — calm, but the same whatever
 * is on. This takes the sleeve's colours, the same read the artwork pages already use, and maps
 * them onto Material 3's tonal roles: the sleeve's brightest note becomes the primary the play
 * disc and the sidebar's pill are drawn in, and the surfaces keep Spotify's darkness while
 * breathing the record's hue instead of flat grey. YouTube Music does exactly this with its
 * dynamic palette; Apple Music does it per page. A grey sleeve keeps its hue to itself — the
 * hue of a grey is quantisation noise, not a colour anyone can see.
 */
@Composable
internal fun rememberDynamicDesktopColorScheme(artworkUrl: String?): ColorScheme {
    val base = desktopColorScheme()
    val dynamic by DesktopAppearanceSettings.dynamicColor.collectAsState()
    if (!dynamic) return base
    val palette = rememberArtworkPalette(artworkUrl)
    val target = dynamicScheme(palette, base)
    val reduceAnimation by DesktopPlayerSettings.reduceAnimation.collectAsState()
    val spec: AnimationSpec<Color> = if (reduceAnimation) snap() else tween(CHROME_FADE_MS)
    return base.copy(
        primary = animateColorAsState(target.primary, spec, label = "dynamicPrimary").value,
        onPrimary = target.onPrimary,
        primaryContainer = animateColorAsState(target.primaryContainer, spec, label = "dynamicPrimaryContainer").value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, spec, label = "dynamicOnPrimaryContainer").value,
        background = animateColorAsState(target.background, spec, label = "dynamicBackground").value,
        surface = animateColorAsState(target.surface, spec, label = "dynamicSurface").value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, spec, label = "dynamicSurfaceVariant").value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, spec, label = "dynamicOnSurfaceVariant").value,
        outline = animateColorAsState(target.outline, spec, label = "dynamicOutline").value,
        outlineVariant = animateColorAsState(target.outlineVariant, spec, label = "dynamicOutlineVariant").value,
        surfaceTint = animateColorAsState(target.surfaceTint, spec, label = "dynamicSurfaceTint").value,
    )
}

/**
 * Spotify's green, or the record's own colour while Dynamic colour is on — the one signal that
 * reads "this is playing" follows whatever the scheme is wearing.
 */
@Composable
internal fun desktopPlayingTint(): Color {
    val dynamic by DesktopAppearanceSettings.dynamicColor.collectAsState()
    return if (dynamic) MaterialTheme.colorScheme.primary else DesktopPlaying
}

/**
 * The tonal mapping itself. Every colour is the sleeve's hue at a Material tone: the primary at
 * M3's dark-mode tone 80, the containers around 30 and 90, the surfaces between tone 6 and tone
 * 16 — dark enough that white text never has to fight them, tinted enough that the app is
 * recognisably *this* record's colour.
 */
private fun dynamicScheme(palette: ArtworkPalette, base: ColorScheme): ColorScheme {
    val seed = palette.accent
    val hsl = FloatArray(3).also { ColorUtils.colorToHSL(seed.toArgb(), it) }
    if (hsl[1] < CHROMATIC_SATURATION) return base
    val hue = hsl[0]
    fun tone(saturation: Float, lightness: Float): Color = Color(
        ColorUtils.HSLToColor(
            floatArrayOf(hue, saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f)),
        ),
    )
    return base.copy(
        primary = tone(0.60f, 0.80f),
        onPrimary = Color.Black,
        primaryContainer = tone(0.46f, 0.28f),
        onPrimaryContainer = tone(0.38f, 0.92f),
        background = tone(0.16f, 0.055f),
        surface = tone(0.16f, 0.075f),
        surfaceVariant = tone(0.18f, 0.155f),
        // Secondary text lifted pale in the page's own hue, the way the artwork pages already
        // treat their second lines.
        onSurfaceVariant = tone(0.14f, 0.66f),
        outline = tone(0.12f, 0.22f),
        outlineVariant = tone(0.12f, 0.16f),
        surfaceTint = tone(0.60f, 0.80f),
    )
}

/** Below this a sleeve is black, white or grey to the eye, whatever hue HSL assigns it. */
private const val CHROMATIC_SATURATION = 0.12f

/** The whole window settling into the next record's colours, not an effect in itself. */
private const val CHROME_FADE_MS = 600
