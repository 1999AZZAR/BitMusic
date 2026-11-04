package com.music.bitchord.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.icons.BitChordIcons
import java.text.Bidi

internal val FLOATING_PLAYER_GUTTER = 16.dp
private val PLAYER_HEIGHT = 78.dp
private val METADATA_HEIGHT = 68.dp
private val PANEL_GAP = 10.dp
private val SPLIT_WIDTH = 880.dp

internal fun playerTitleDirection(title: String): LayoutDirection =
    if (Bidi(title, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT).baseIsLeftToRight()) {
        LayoutDirection.Ltr
    } else {
        LayoutDirection.Rtl
    }

internal fun floatingPlayerClearance(contentWidth: Dp): Dp {
    val height =
        if (contentWidth - FLOATING_PLAYER_GUTTER * 2 < SPLIT_WIDTH) {
            METADATA_HEIGHT + PANEL_GAP + PLAYER_HEIGHT
        } else {
            PLAYER_HEIGHT
        }
    return height + FLOATING_PLAYER_GUTTER * 2
}

/** Background input barrier; interactive controls are separate siblings above it. */
internal fun Modifier.desktopPlayerPointerShield(): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                event.changes.forEach { it.consume() }
            }
        }
    }

@Composable
private fun PlayerPanel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier.shadow(12.dp, shape, clip = false).clip(shape).desktopPlayerGlass(),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.matchParentSize().desktopPlayerPointerShield())
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DesktopFloatingPlayer(
    active: Boolean = true,
    song: Song?,
    isPlaying: Boolean,
    isLoading: Boolean,
    previousEnabled: Boolean,
    positionMs: Long,
    durationMs: Long,
    progress: Float,
    onSeek: (Long) -> Unit,
    volume: Float,
    shuffle: Boolean,
    repeatMode: DesktopRepeatMode,
    /** The now-playing song's own row — the like and the options the full player has. */
    liked: Boolean,
    /** An account to record a like against, and a YouTube identity to rate. */
    canRate: Boolean,
    onToggleLike: () -> Unit,
    onOpenMenu: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onRepeatModeChange: (DesktopRepeatMode) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenAudioOutput: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenRelated: () -> Unit,
    /** Which of the three the column beside the page is showing, lit on its button. */
    sidePanel: DesktopSidePanel?,
    accountAvatar: String?,
    onOpenAccounts: () -> Unit,
    /** The app's one way back — every page's own arrow was folded into this. */
    canGoBack: Boolean,
    onBack: () -> Unit,
) {

    val playingTint by rememberUpdatedState(desktopPlayingTint())
    val safeProgress = progress.coerceIn(0f, 1f)
    val visible by DesktopWindowVisibility.renderingActive.collectAsState()
    val reduceAnimation by DesktopPlayerSettings.reduceAnimation.collectAsState()
    val marqueeEnabled = active && visible && !reduceAnimation
    val metadata: @Composable (Modifier, Boolean) -> Unit = { modifier, coverOnly ->
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            DesktopToolbarButton(onClick = onBack, enabled = canGoBack, size = 32.dp) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    DesktopStrings["back", "Back"],
                    tint = if (canGoBack) Color.White else DesktopSecondary,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            if (song != null) {
                Row(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenNowPlaying)
                        .then(
                            if (coverOnly)
                                Modifier.semantics {
                                    contentDescription = "${song.title} — ${song.artist}"
                                }
                            else Modifier
                        )
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DesktopArtwork(
                        song.thumbnailUrl,
                        Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)),
                        px = ROW_ART_PX,
                    )
                    if (!coverOnly) {
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            val titleDirection =
                                remember(song.title) { playerTitleDirection(song.title) }
                            CompositionLocalProvider(LocalLayoutDirection provides titleDirection) {
                                Text(
                                    song.title,
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .then(
                                                if (marqueeEnabled)
                                                    Modifier.basicMarquee(
                                                        iterations = Int.MAX_VALUE,
                                                        initialDelayMillis = 800,
                                                        repeatDelayMillis = 1200,
                                                        velocity = 24.dp,
                                                    )
                                                else Modifier
                                            ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow =
                                        if (marqueeEnabled) TextOverflow.Clip
                                        else TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                            }
                            Text(
                                song.artist,
                                color = DesktopSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
                if (canRate)
                    DesktopToolbarButton(onClick = onToggleLike, size = 30.dp) {
                        Icon(
                            if (liked) BitChordIcons.HeartFilled else BitChordIcons.Heart,
                            DesktopStrings["like", "Like"],
                            tint = if (liked) playingTint else DesktopSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                DesktopToolbarButton(onClick = onOpenMenu, size = 30.dp) {
                    Icon(
                        Icons.Rounded.MoreHoriz,
                        DesktopStrings["more_options", "More options"],
                        tint = DesktopSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
    val transport: @Composable (Modifier, Boolean) -> Unit = { modifier, showVisualizer ->
        Column(
            modifier = modifier.padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(38.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (showVisualizer)
                    SpectrumPane(
                        tint = playingTint,
                        section = SpectrumSection.LEFT_HALF,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DesktopToolbarButton(onClick = { onShuffleChange(!shuffle) }, size = 30.dp) {
                        Icon(
                            BitChordIcons.Shuffle,
                            DesktopStrings["shuffle", "Shuffle"],
                            tint = if (shuffle) playingTint else DesktopSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    DesktopToolbarButton(
                        onClick = onPrevious,
                        enabled = previousEnabled,
                        size = 30.dp,
                    ) {
                        Icon(
                            Icons.Rounded.FastRewind,
                            DesktopStrings["widget_previous", "Previous"],
                            tint =
                                if (previousEnabled) Color.White
                                else DesktopSecondary.copy(alpha = 0.40f),
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    DesktopToolbarButton(onClick = onPlayPause, size = 38.dp) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    if (isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                    DesktopToolbarButton(onClick = onNext, size = 30.dp) {
                        Icon(
                            Icons.Rounded.FastForward,
                            DesktopStrings["widget_next", "Next"],
                            tint = Color.White,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    DesktopToolbarButton(
                        onClick = { onRepeatModeChange(repeatMode.next()) },
                        size = 30.dp,
                    ) {
                        val tint =
                            if (repeatMode != DesktopRepeatMode.OFF) playingTint
                            else DesktopSecondary
                        if (repeatMode == DesktopRepeatMode.ONE) {
                            Text(
                                "1",
                                color = tint,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.semantics { contentDescription = "Repeat one" },
                            )
                        } else {
                            Icon(
                                BitChordIcons.Repeat,
                                "Repeat ${repeatMode.label()}",
                                tint = tint,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                if (showVisualizer)
                    SpectrumPane(
                        tint = playingTint,
                        section = SpectrumSection.RIGHT_HALF,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = formatPlayerTimestamp(positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = DesktopSecondary,
                )
                Box(
                    modifier =
                        Modifier.weight(1f).height(12.dp).pointerInput(durationMs) {
                            if (durationMs <= 0L) return@pointerInput
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: continue
                                    if (change.pressed && size.width > 0) {
                                        val fraction =
                                            (change.position.x / size.width.toFloat()).coerceIn(
                                                0f,
                                                1f,
                                            )
                                        onSeek((fraction * durationMs).toLong())
                                        change.consume()
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Canvas(Modifier.fillMaxWidth().height(3.dp)) {
                        val trackRadius = CornerRadius(size.height / 2f)
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.16f),
                            size = size,
                            cornerRadius = trackRadius,
                        )
                        val activeWidth = size.width * safeProgress
                        if (activeWidth > 0f) {
                            drawRoundRect(
                                color = playingTint,
                                size = Size(activeWidth, size.height),
                                cornerRadius = trackRadius,
                            )
                        }
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = Offset(activeWidth.coerceIn(0f, size.width), size.height / 2f),
                        )
                    }
                }
                Text(
                    text = formatPlayerTimestamp(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = DesktopSecondary,
                )
            }
        }
    }
    val actions: @Composable (Modifier, Boolean) -> Unit = { modifier, compactActions ->
        val showVolume = !compactActions
        val actionSize = if (compactActions) 28.dp else 32.dp
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showVolume) {
                Icon(
                    Icons.AutoMirrored.Rounded.VolumeUp,
                    DesktopStrings["d_volume", "Volume"],
                    tint = DesktopSecondary,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(8.dp))
                DesktopThinSlider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    idleHeight = 4.dp,
                    activeHeight = 8.dp,
                    modifier = Modifier.width(82.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            DesktopToolbarButton(onClick = onOpenAudioOutput, size = actionSize) {
                Icon(
                    Icons.Rounded.Headphones,
                    DesktopStrings["audio_output", "Audio output"],
                    tint = DesktopSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            DesktopToolbarButton(onClick = onOpenLyrics, size = actionSize) {
                Icon(
                    BitChordIcons.LyricsQuote,
                    DesktopStrings["lyrics", "Lyrics"],
                    tint =
                        if (sidePanel == DesktopSidePanel.LYRICS) Color.White else DesktopSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            DesktopToolbarButton(onClick = onOpenQueue, size = actionSize) {
                Icon(
                    BitChordIcons.Queue,
                    DesktopStrings["queue", "Queue"],
                    tint =
                        if (sidePanel == DesktopSidePanel.QUEUE) Color.White else DesktopSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            DesktopToolbarButton(onClick = onOpenRelated, size = actionSize) {
                Icon(
                    BitChordIcons.Explore,
                    DesktopStrings["related", "Related"],
                    tint =
                        if (sidePanel == DesktopSidePanel.RELATED) Color.White
                        else DesktopSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            DesktopAccountButton(avatar = accountAvatar, onClick = onOpenAccounts)
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val showVisualizer = active && maxWidth >= 1000.dp
        if (maxWidth < SPLIT_WIDTH) {
            val coverOnly = maxWidth < 560.dp
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PANEL_GAP)) {
                PlayerPanel(Modifier.fillMaxWidth().height(METADATA_HEIGHT)) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        metadata(Modifier.weight(1f), coverOnly)
                        actions(Modifier.width(156.dp), true)
                    }
                }
                PlayerPanel(Modifier.fillMaxWidth().height(PLAYER_HEIGHT)) {
                    transport(Modifier.fillMaxSize(), false)
                }
            }
        } else {
            PlayerPanel(Modifier.fillMaxWidth().height(PLAYER_HEIGHT)) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    metadata(Modifier.weight(0.4f), false)
                    transport(Modifier.weight(0.6f).fillMaxHeight(), showVisualizer)
                    actions(Modifier.width(284.dp), false)
                }
            }
        }
    }
}

private fun formatPlayerTimestamp(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
