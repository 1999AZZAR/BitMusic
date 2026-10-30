package com.music.bitchord.ui.tv.player

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.PlaybackPosition
import com.music.bitchord.ui.player.SyncedLyricsPanel
import com.music.bitchord.ui.tv.components.tvFocusBorder
import com.music.bitchord.ui.tv.drawer.TvDrawerTab
import com.music.bitchord.ui.tv.drawer.TvNowPlayingDrawerState
import com.music.bitchord.ui.tv.theme.TvGlass
import com.music.bitchord.ui.tv.theme.TvType
import kotlinx.coroutines.delay

/**
 * Slide-in Queue and Synchronized Lyrics Drawer for 10-foot TV experience.
 * Provides instant access to upcoming tracks and synced lyrics during full-screen playback
 * without interrupting background audio.
 */
@Composable
fun TvNowPlayingDrawer(
    drawerState: TvNowPlayingDrawerState,
    queue: List<Song>,
    currentIndex: Int,
    lyrics: List<LyricLine>?,
    position: PlaybackPosition,
    onJumpToTrack: (Int) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (drawerState.isOpen) {
        BackHandler {
            drawerState.close()
        }
    }

    val tabFocus = remember { FocusRequester() }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            delay(80)
            runCatching { tabFocus.requestFocus() }
        }
    }

    AnimatedVisibility(
        visible = drawerState.isOpen,
        enter = fadeIn(tween(220)) + slideInHorizontally(tween(280)) { it },
        exit = fadeOut(tween(200)) + slideOutHorizontally(tween(240)) { it },
        modifier = modifier.fillMaxHeight(),
    ) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .fillMaxHeight()
                .background(Color(0xE6101014))
                .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.12f))
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            drawerState.previousTab()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            drawerState.nextTab()
                            true
                        }
                        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                            drawerState.close()
                            true
                        }
                        else -> false
                    }
                },
        ) {
            Column(Modifier.fillMaxSize()) {
                // Drawer Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(tabFocus)
                        .focusable(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TvDrawerTabHeader(
                        title = "Queue",
                        selected = drawerState.selectedTab == TvDrawerTab.QUEUE,
                        onClick = { drawerState.selectTab(TvDrawerTab.QUEUE) },
                        modifier = Modifier.weight(1f),
                    )
                    TvDrawerTabHeader(
                        title = "Lyrics",
                        selected = drawerState.selectedTab == TvDrawerTab.LYRICS,
                        onClick = { drawerState.selectTab(TvDrawerTab.LYRICS) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Content Area
                when (drawerState.selectedTab) {
                    TvDrawerTab.QUEUE -> {
                        val upcoming = remember(queue, currentIndex) {
                            queue.drop((currentIndex + 1).coerceAtLeast(0))
                        }
                        if (upcoming.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "No upcoming songs queued",
                                    style = TvType.Callout,
                                    color = TvGlass.TextSecondary,
                                )
                            }
                        } else {
                            val listState = rememberLazyListState()
                            LazyColumn(
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            ) {
                                itemsIndexed(upcoming, key = { _, s -> s.videoId }) { offset, song ->
                                    val targetIndex = currentIndex + 1 + offset
                                    TvDrawerQueueRow(
                                        song = song,
                                        onClick = { onJumpToTrack(targetIndex) },
                                    )
                                }
                            }
                        }
                    }
                    TvDrawerTab.LYRICS -> {
                        if (lyrics.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Lyrics unavailable for this track",
                                    style = TvType.Callout,
                                    color = TvGlass.TextSecondary,
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            ) {
                                SyncedLyricsPanel(
                                    lines = lyrics,
                                    position = position,
                                    onSeek = onSeek,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvDrawerTabHeader(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val bg = when {
        focused -> TvGlass.Platter
        selected -> TvGlass.FillSelected
        else -> TvGlass.Fill
    }
    val fg = when {
        focused -> TvGlass.OnPlatter
        selected -> TvGlass.TextPrimary
        else -> TvGlass.TextSecondary
    }

    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .tvFocusBorder(
                interactionSource = interaction,
                shape = RoundedCornerShape(10.dp),
                focusedScale = 1.04f,
                elevation = 8.dp,
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = TvType.Headline.copy(fontSize = 14.sp),
            color = fg,
        )
    }
}

@Composable
private fun TvDrawerQueueRow(
    song: Song,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) TvGlass.Platter else TvGlass.Fill)
            .tvFocusBorder(
                interactionSource = interaction,
                shape = RoundedCornerShape(8.dp),
                focusedScale = 1.03f,
                elevation = 8.dp,
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = TvType.Body,
                color = if (focused) TvGlass.OnPlatter else TvGlass.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = TvType.Caption,
                color = if (focused) TvGlass.OnPlatterSecondary else TvGlass.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
