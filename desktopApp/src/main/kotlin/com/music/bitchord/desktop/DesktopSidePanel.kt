package com.music.bitchord.desktop

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** What the column beside the page is showing. */
internal enum class DesktopSidePanel { LYRICS, QUEUE }

/**
 * Apple Music's right-hand sidebar: the lyrics or the queue in a column beside the page, while the
 * player itself stays closed. The page narrows to make room rather than being covered, so whatever
 * was being browsed stays usable next to the words.
 *
 * The opening is the sidebar's own collapse, run the other way: one animated width that the
 * column's content and the page beside it both follow, so the whole window is resized by a single
 * curve — lyrics lines and queue rows reflow as the column grows, rather than a finished panel
 * being unclipped over a page that has already been squeezed.
 */
@Composable
internal fun DesktopSidePanelColumn(
    panel: DesktopSidePanel?,
    onClose: () -> Unit,
    content: @Composable (DesktopSidePanel) -> Unit,
) {
    // Held through the exit, so the column shrinks away with what it had in it rather than empty.
    var shown by remember { mutableStateOf(panel) }
    if (panel != null) shown = panel
    val reduceAnimation by DesktopPlayerSettings.reduceAnimation.collectAsState()
    val width by animateDpAsState(
        targetValue = if (panel != null) SIDE_PANEL_WIDTH else 0.dp,
        animationSpec = if (reduceAnimation) {
            snap()
        } else {
            spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
        },
        label = "sidePanelWidth",
    )
    // A whisper of fade over the reflow: the words settle in as the column finds its width, and
    // leave before the shrinking starts eating them.
    val contentAlpha by animateFloatAsState(
        targetValue = if (panel != null) 1f else 0f,
        animationSpec = if (reduceAnimation) snap() else tween(CROSSFADE_MS),
        label = "sidePanelAlpha",
    )
    if (width > 0.dp && shown != null) {
        Row(Modifier.width(width).fillMaxHeight()) {
            Box(Modifier.width(1.dp).fillMaxHeight().background(DesktopDivider))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer { alpha = contentAlpha },
            ) {
                Crossfade(
                    targetState = shown ?: return@Box,
                    animationSpec = tween(CROSSFADE_MS),
                    label = "sidePanel",
                ) { which ->
                    Box(Modifier.fillMaxSize()) {
                        // The lyrics stand on the window itself, as in Apple Music; the queue is a list
                        // and takes the sidebar's glass.
                        if (which == DesktopSidePanel.QUEUE) {
                            Box(Modifier.fillMaxSize().desktopChromeGlass())
                        }
                        Column(Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (which == DesktopSidePanel.LYRICS) HEADER_HEIGHT else QUEUE_HEADER_HEIGHT)
                                    .padding(start = 18.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // The queue heads itself ("Queue", then its sections); only the lyrics
                                // need a name over them.
                                Text(
                                    text = if (which == DesktopSidePanel.LYRICS) DesktopStrings["lyrics", "Lyrics"] else "",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                DesktopToolbarButton(onClick = onClose, size = 28.dp) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        DesktopStrings["close", "Close"],
                                        tint = DesktopSecondary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(
                                        start = if (which == DesktopSidePanel.LYRICS) 18.dp else 14.dp,
                                        end = if (which == DesktopSidePanel.LYRICS) 18.dp else 4.dp,
                                        bottom = 14.dp,
                                    ),
                            ) {
                                content(which)
                            }
                        }
                    }
                }
            }
        }
    }
}

private val SIDE_PANEL_WIDTH = 360.dp
private val HEADER_HEIGHT = 46.dp
private val QUEUE_HEADER_HEIGHT = 30.dp
private const val CROSSFADE_MS = 180
