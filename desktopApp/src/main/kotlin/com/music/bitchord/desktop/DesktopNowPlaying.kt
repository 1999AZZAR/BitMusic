package com.music.bitchord.desktop

import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.icons.BitChordIcons
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import com.music.bitchord.data.AudioQualityBadgeFormatter
import com.music.bitchord.data.AudioQualityTier
import com.music.bitchord.data.NerdStats
import com.music.bitchord.sharedui.resources.Res as SharedRes
import com.music.bitchord.sharedui.resources.ic_lossless
import com.music.bitchord.sharedui.resources.ic_dolby_atmos
import org.jetbrains.compose.resources.painterResource
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

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
    /** Whether this track has lyrics at all; a track without them keeps the half folded. */
    lyricsAvailable: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    liked: Boolean,
    canRate: Boolean,
    isVideoMode: Boolean = false,
    videoStream: DesktopVideoStream? = null,
    onToggleVideoMode: () -> Unit = {},
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
    // The right half shows one of the two — the original player's split, with this player's
    // controls. Either button picks its panel; the button of whichever is showing puts the half
    // away again, for the times the record deserves the whole window.
    // It also folds on its own: a track with no words does not keep an empty lyric sheet up —
    // the half simply closes, and the lyrics button sits the verse out with it.
    var sidePanel by remember { mutableStateOf(DesktopSidePanel.LYRICS) }
    var panelShown by remember { mutableStateOf(lyricsAvailable) }
    var isTheater by remember { mutableStateOf(false) }
    LaunchedEffect(lyricsAvailable) {
        if (!lyricsAvailable) panelShown = false
    }
    LaunchedEffect(isVideoMode) {
        if (!isVideoMode) isTheater = false
    }
    val reduceMotion by DesktopPlayerSettings.reduceAnimation.collectAsState()
    val panelFraction by animateFloatAsState(
        targetValue = if (panelShown) 1f else 0f,
        animationSpec = if (reduceMotion) {
            snap()
        } else {
            spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
        },
        label = "playerPanelFraction",
    )
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
                val canToggleVideo = song.videoId.isNotBlank() && (song.localUri == null || videoStream != null)
                if (canToggleVideo && !isTheater) {
                    DesktopSongVideoToggle(
                        isVideoMode = isVideoMode,
                        onToggle = onToggleVideoMode,
                        tint = tint,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }

                if (isVideoMode) {
                    if (videoStream != null) {
                        DesktopVideoSurface(
                            videoStream = videoStream,
                            isPlaying = isPlaying,
                            positionMs = positionMs,
                            isTheater = isTheater,
                            onToggleTheater = {
                                isTheater = !isTheater
                                if (isTheater) panelShown = false
                            },
                            modifier = if (isTheater) {
                                Modifier.fillMaxWidth(0.95f).weight(1f).padding(bottom = 12.dp)
                            } else {
                                Modifier.width(520.dp)
                            },
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .width(520.dp)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.dp,
                                    color = tint,
                                )
                                Text(
                                    "Loading video...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DesktopSecondary,
                                )
                            }
                        }
                    }
                } else {
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
                                .size(324.dp)
                                .shadow(24.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
                            px = 720,
                        )
                    }
                }
                Spacer(Modifier.height(if (isVideoMode) 14.dp else 24.dp))
                val titleWidth = if (isTheater && isVideoMode) 640.dp else 500.dp
                Text(
                    song.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(titleWidth),
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = tint,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .width(titleWidth)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenArtist)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
                if (!song.albumName.isNullOrBlank() && !isTheater) {
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
                if (!isVideoMode) {
                    Spacer(Modifier.height(18.dp))
                    SpectrumPane(
                        tint = tint,
                        modifier = Modifier.width(420.dp).height(54.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                val controlWidth = if (isTheater && isVideoMode) 640.dp else 420.dp
                Row(
                    Modifier.width(controlWidth),
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
                Spacer(Modifier.height(16.dp))
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
                    DesktopToolbarButton(onClick = onPrevious, enabled = hasPrevious, size = 40.dp) {
                        Icon(
                            Icons.Rounded.FastRewind,
                            "Previous",
                            tint = if (hasPrevious) Color.White else DesktopSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(27.dp),
                        )
                    }
                    Box(
                        Modifier
                            .size(56.dp)
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
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }
                    DesktopToolbarButton(onClick = onNext, enabled = hasNext, size = 40.dp) {
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
                Spacer(Modifier.height(14.dp))
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
                        onClick = {
                            if (panelShown && sidePanel == DesktopSidePanel.LYRICS) {
                                panelShown = false
                            } else {
                                isTheater = false
                                sidePanel = DesktopSidePanel.LYRICS
                                panelShown = true
                            }
                        },
                        enabled = lyricsAvailable,
                        size = 36.dp,
                    ) {
                        Icon(
                            BitChordIcons.LyricsQuote,
                            "Lyrics",
                            tint = if (panelShown && sidePanel == DesktopSidePanel.LYRICS) {
                                tint
                            } else {
                                DesktopSecondary.copy(alpha = if (lyricsAvailable) 1f else 0.4f)
                            },
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    DesktopToolbarButton(
                        onClick = {
                            if (panelShown && sidePanel == DesktopSidePanel.QUEUE) {
                                panelShown = false
                            } else {
                                isTheater = false
                                sidePanel = DesktopSidePanel.QUEUE
                                panelShown = true
                            }
                        },
                        size = 36.dp,
                    ) {
                        Icon(
                            BitChordIcons.Queue,
                            "Queue",
                            tint = if (panelShown && sidePanel == DesktopSidePanel.QUEUE) tint else DesktopSecondary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
            // The other half of the window, as the original player split it: the lyric sheet
            // standing on the window itself, or the queue on the sidebar's glass. Its share of
            // the window animates, so putting it away hands the space back to the sleeve in
            // one motion rather than a jump.
            if (panelFraction > 0.01f) {
                Box(
                    Modifier
                        .weight(panelFraction)
                        .fillMaxHeight()
                        .graphicsLayer { alpha = panelFraction }
                        .padding(top = 12.dp, bottom = 12.dp, end = 20.dp),
                ) {
                    when (sidePanel) {
                        // The lyric sheet stands on the window itself, as the original's does.
                        DesktopSidePanel.LYRICS -> Box(Modifier.fillMaxSize()) { lyricsPanel() }
                        // The queue floats: a rounded card of lighter glass than the bars use —
                        // the mesh is meant to breathe through it — with a hairline edge and a
                        // fade that lets the last row dissolve instead of stopping mid-picture.
                        DesktopSidePanel.QUEUE -> Box(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .playerQueueGlass()
                                .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
                        ) {
                            queuePanel()
                            // The dissolve: the list's last rows wash out into the card rather
                            // than being cut by its rounded floor.
                            Box(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                                        ),
                                    ),
                            )
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

/**
 * The queue card's own glass: lighter than the bars' — a wide blur with a shallow tint, so the
 * mesh behind it colourises the card instead of a near-black slab sitting on it — and, when
 * dynamic blur is off, a translucent fill in the same spirit.
 */
@Composable
private fun Modifier.playerQueueGlass(): Modifier {
    val haze = LocalDesktopHaze.current
    val reduceDynamicBlur by DesktopAppearanceSettings.reduceDynamicBlur.collectAsState()
    val surface = MaterialTheme.colorScheme.surface
    return if (haze == null || reduceDynamicBlur) {
        background(surface.copy(alpha = 0.82f))
    } else {
        hazeEffect(state = haze) {
            blurEnabled = true
            backgroundColor = surface
            blurRadius = 40.dp
            tints = listOf(HazeTint(surface.copy(alpha = 0.30f)))
            noiseFactor = 0.03f
        }
    }
}

/**
 * The queue, drawn for a window rather than a phone column: artwork big enough to read,
 * a head that names the queue and counts what is left, the playing row in a pill of the
 * record's tint with the equalizer glyph beside it, and a remove that appears where the
 * pointer is.
 */
@Composable
internal fun PlayerQueuePane(
    queue: List<Song>,
    index: Int,
    onJumpTo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    tint: Color,
) {
    val upcoming = (queue.size - index - 1).coerceAtLeast(0)
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                DesktopStrings["queue", "Queue"],
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "$upcoming next",
                color = DesktopSecondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Spacer(Modifier.weight(1f))
            if (upcoming > 0) {
                Text(
                    DesktopStrings["clear", "Clear"],
                    color = DesktopSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            val current = queue.getOrNull(index)
            if (current != null) {
                item(key = "now-playing") {
                    QueueSectionLabel(DesktopStrings["now_playing", "Now playing"])
                    PlayerQueueRow(current, tint, isCurrent = true, onClick = {}, onRemove = null)
                }
            }
            if (upcoming > 0) {
                item(key = "next-up-label") {
                    QueueSectionLabel(DesktopStrings["next_up", "Next up"])
                }
                itemsIndexed(
                    queue.drop(index + 1),
                    key = { offset, song -> "${index + 1 + offset}-${song.videoId}" },
                ) { offset, song ->
                    PlayerQueueRow(
                        song,
                        tint,
                        isCurrent = false,
                        onClick = { onJumpTo(index + 1 + offset) },
                        onRemove = { onRemove(index + 1 + offset) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueSectionLabel(label: String) {
    Text(
        label,
        color = DesktopSecondary,
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 6.dp),
    )
}

@Composable
private fun PlayerQueueRow(
    song: Song,
    tint: Color,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    var hovered by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isCurrent -> tint.copy(alpha = 0.18f)
                    hovered -> Color.White.copy(alpha = 0.05f)
                    else -> Color.Transparent
                },
            )
            .clickable(onClick = onClick)
            .pointerInput(song.videoId) {
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            androidx.compose.ui.input.pointer.PointerEventType.Enter -> hovered = true
                            androidx.compose.ui.input.pointer.PointerEventType.Exit -> hovered = false
                        }
                    }
                }
            }
            .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesktopArtwork(
            song.thumbnailUrl,
            Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
            px = 96,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (isCurrent) tint else Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artist,
                color = DesktopSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isCurrent) {
            QueueQualityBadge(tint)
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
        } else if (hovered && onRemove != null) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = DesktopStrings["remove_from_queue", "Remove"],
                tint = DesktopSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove)
                    .padding(3.dp),
            )
        } else {
            Spacer(Modifier.size(16.dp))
        }
    }
}

@Composable
private fun QueueQualityBadge(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val stats by NerdStats.current.collectAsState()
    val badge = AudioQualityBadgeFormatter.formatBadge(stats) ?: return

    val isLossless = badge.isLossless
    val isAtmos = badge.tier == AudioQualityTier.DOLBY_ATMOS

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                when {
                    isLossless -> Color(0xFF26C6DA).copy(alpha = 0.22f)
                    isAtmos -> Color(0xFFAB47BC).copy(alpha = 0.22f)
                    else -> Color.White.copy(alpha = 0.08f)
                }
            )
            .border(
                0.5.dp,
                when {
                    isLossless -> Color(0xFF26C6DA).copy(alpha = 0.45f)
                    isAtmos -> Color(0xFFAB47BC).copy(alpha = 0.45f)
                    else -> Color.White.copy(alpha = 0.14f)
                },
                RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val iconColor = when {
            isLossless -> Color(0xFF80DEEA)
            isAtmos -> Color(0xFFCE93D8)
            else -> tint.copy(alpha = 0.85f)
        }

        when {
            isAtmos -> {
                Icon(
                    painter = painterResource(SharedRes.drawable.ic_dolby_atmos),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.height(9.dp),
                )
            }
            isLossless -> {
                Icon(
                    painter = painterResource(SharedRes.drawable.ic_lossless),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(11.dp),
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Rounded.Headphones,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(11.dp),
                )
            }
        }

        Text(
            text = badge.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
            ),
            color = if (isLossless) Color(0xFFE0F7FA) else Color.White.copy(alpha = 0.9f),
        )

        val details = badge.details
        if (!details.isNullOrBlank()) {
            Text(
                text = "·",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.35f),
            )
            Text(
                text = details,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                ),
                color = Color.White.copy(alpha = 0.65f),
            )
        }
    }
}

/**
 * Animated "Song / Video" segmented toggle pill.
 */
@Composable
private fun DesktopSongVideoToggle(
    isVideoMode: Boolean,
    onToggle: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (!isVideoMode) tint else Color.Transparent)
                .clickable(enabled = isVideoMode, onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Song",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (!isVideoMode) Color.Black else Color.White.copy(alpha = 0.7f),
            )
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isVideoMode) tint else Color.Transparent)
                .clickable(enabled = !isVideoMode, onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Video",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isVideoMode) Color.Black else Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

