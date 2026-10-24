package com.music.bitchord.desktop

import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.icons.BitChordIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The desktop's own full player. The phone's screen, shared, played the role until now; this is
 * the window's take on the same record — the sleeve on the mesh of itself, the visualizer at
 * full width in the record's dynamic tint, the transport in glass, and the lyrics or the queue
 * in the same panels the main window uses, beside it rather than beneath it.
 *
 * [lyricsPanel] and [queuePanel] arrive as slots so this file stays out of the lyrics plumbing;
 * whatever the side column shows, this frame draws it in.
 */
@Composable
internal fun DesktopNowPlaying(
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffle: Boolean,
    repeatMode: DesktopRepeatMode,
    /** An Automix blend is running: the scrubber carries its travelling sheen. */
    mixing: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    liked: Boolean,
    canRate: Boolean,
    onToggleLike: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenArtist: () -> Unit,
    onClose: () -> Unit,
    lyricsPanel: @Composable () -> Unit,
    queuePanel: @Composable () -> Unit,
) {
    var sidePanel by remember { mutableStateOf<DesktopSidePanel?>(null) }
    val tint = desktopPlayingTint()
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    // The knob follows the drag, not the playhead: a scrub that lags behind the hand feels broken.
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    Box(Modifier.fillMaxSize()) {
        // The record's own colours, behind everything: the mesh of the sleeve, a radial breath
        // of its dynamic tint behind where the sleeve will sit, and a scrim that deepens toward
        // the floor so the transport reads on near-black rather than competing with the picture.
        DesktopMesh(song.thumbnailUrl)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.38f), Color.Black.copy(alpha = 0.72f), Color.Black.copy(alpha = 0.92f)),
                ),
            ),
        )
        Row(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp)) {
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // The glow the sleeve seems to cast: the record's tint, wide and soft, from
                    // behind the artwork rather than around the window.
                    val glow = tint.copy(alpha = 0.22f)
                    Box(
                        Modifier
                            .matchParentSize()
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(glow, Color.Transparent),
                                        center = Offset(size.width / 2, size.height / 2),
                                        radius = size.minDimension * 0.85f,
                                    ),
                                )
                            },
                    )
                    DesktopArtwork(
                        song.thumbnailUrl,
                        Modifier
                            .size(360.dp)
                            .shadow(28.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
                        px = 720,
                    )
                }
                Spacer(Modifier.height(30.dp))
                Text(
                    song.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(500.dp),
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = tint,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .width(500.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenArtist)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
                if (!song.albumName.isNullOrBlank()) {
                    Text(
                        song.albumName.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = DesktopSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(500.dp),
                    )
                }
                Spacer(Modifier.height(22.dp))
                SpectrumPane(
                    tint = tint,
                    modifier = Modifier.width(440.dp).height(60.dp),
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.width(440.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(timeLabel(if (dragging) dragValue else fraction, durationMs), color = DesktopSecondary, style = MaterialTheme.typography.labelSmall)
                    DesktopThinSlider(
                        value = if (dragging) dragValue else fraction,
                        onValueChange = { dragValue = it; dragging = true },
                        onValueChangeFinished = {
                            dragging = false
                            onSeek((dragValue * durationMs).toLong())
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                        mixing = isPlaying && mixing,
                        idleHeight = 5.dp,
                        activeHeight = 9.dp,
                    )
                    Text(timeLabel(1f, durationMs), color = DesktopSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(22.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DesktopToolbarButton(onClick = onToggleShuffle, size = 40.dp) {
                        Icon(
                            BitChordIcons.Shuffle,
                            "Shuffle",
                            tint = if (shuffle) tint else DesktopSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DesktopToolbarButton(onClick = onPrevious, enabled = hasPrevious, size = 44.dp) {
                        Icon(
                            Icons.Rounded.FastRewind,
                            "Previous",
                            tint = if (hasPrevious) Color.White else DesktopSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Box(
                        Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(tint)
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        } else {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                    DesktopToolbarButton(onClick = onNext, enabled = hasNext, size = 44.dp) {
                        Icon(
                            Icons.Rounded.FastForward,
                            "Next",
                            tint = if (hasNext) Color.White else DesktopSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    DesktopToolbarButton(onClick = onCycleRepeat, size = 40.dp) {
                        if (repeatMode == DesktopRepeatMode.ONE) {
                            Text(
                                "1",
                                color = tint,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        } else {
                            Icon(
                                BitChordIcons.Repeat,
                                "Repeat",
                                tint = if (repeatMode != DesktopRepeatMode.OFF) tint else DesktopSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (canRate) {
                        DesktopToolbarButton(onClick = onToggleLike, size = 36.dp) {
                            Icon(
                                if (liked) BitChordIcons.HeartFilled else BitChordIcons.Heart,
                                if (liked) "Remove from liked songs" else "Like",
                                tint = if (liked) tint else DesktopSecondary,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                    DesktopToolbarButton(onClick = onOpenMenu, size = 36.dp) {
                        Icon(Icons.Rounded.MoreHoriz, "More options", tint = DesktopSecondary, modifier = Modifier.size(19.dp))
                    }
                    DesktopToolbarButton(
                        onClick = { sidePanel = if (sidePanel == DesktopSidePanel.LYRICS) null else DesktopSidePanel.LYRICS },
                        size = 36.dp,
                    ) {
                        Icon(
                            BitChordIcons.LyricsQuote,
                            "Lyrics",
                            tint = if (sidePanel == DesktopSidePanel.LYRICS) tint else DesktopSecondary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    DesktopToolbarButton(
                        onClick = { sidePanel = if (sidePanel == DesktopSidePanel.QUEUE) null else DesktopSidePanel.QUEUE },
                        size = 36.dp,
                    ) {
                        Icon(
                            BitChordIcons.Queue,
                            "Queue",
                            tint = if (sidePanel == DesktopSidePanel.QUEUE) tint else DesktopSecondary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
            if (sidePanel != null) {
                // No divider: the panels carry their own glass, and a rule here read as a
                // black line over the mesh.
                Row(
                    Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                        .padding(start = 18.dp),
                ) {
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        when (sidePanel) {
                            DesktopSidePanel.LYRICS -> lyricsPanel()
                            DesktopSidePanel.QUEUE -> queuePanel()
                            null -> {}
                        }
                    }
                }
            }
        }
        DesktopToolbarButton(
            onClick = onClose,
            size = 36.dp,
            modifier = Modifier.align(Alignment.TopEnd).padding(18.dp),
        ) {
            Icon(Icons.Rounded.Close, "Close", tint = DesktopSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

private fun timeLabel(fraction: Float, durationMs: Long): String {
    val seconds = ((fraction * durationMs) / 1000).toLong()
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
