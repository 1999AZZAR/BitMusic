package com.music.bitchord.desktop

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Desktop
import java.net.URI

/**
 * Controller for spawning floating Picture-in-Picture (PiP) video windows via mpv.
 * Zero-deadlock, hardware accelerated on Linux X11/Wayland.
 */
object DesktopMpvPlayer {
    private var currentProcess: Process? = null

    fun play(
        videoId: String,
        startAtSec: Long,
        title: String? = null,
        onStart: () -> Unit = {},
        onExit: () -> Unit = {},
    ) {
        stop()
        try {
            val process = ProcessBuilder(
                "mpv",
                "--ontop",
                "--no-terminal",
                "--geometry=640x360-24+24",
                "--title=BitMusic MV - ${title ?: "Video"}",
                "--ytdl-path=/home/azzar/.local/bin/yt-dlp",
                "https://www.youtube.com/watch?v=$videoId",
                "--start=$startAtSec",
            ).start()
            currentProcess = process
            onStart()
            Thread {
                runCatching {
                    process.waitFor()
                    onExit()
                }
            }.start()
        } catch (e: Exception) {
            DesktopTrackLog.log("mpv launch failed: ${e.message}")
            runCatching {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI("https://www.youtube.com/watch?v=$videoId&t=${startAtSec}s"))
                }
            }
        }
    }

    fun isPlaying(): Boolean = currentProcess?.isAlive == true

    fun stop() {
        runCatching {
            currentProcess?.destroy()
            currentProcess = null
        }
    }
}

/**
 * Pure Compose Desktop Music Video stage.
 * Never blocks the AWT/Skiko rendering thread, eliminating UI freeze issues completely.
 */
@Composable
fun DesktopMusicVideoView(
    videoId: String,
    title: String? = null,
    startAtSec: Long = 0L,
    isPlaying: Boolean = true,
    onLaunchPip: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val containerShape = RoundedCornerShape(16.dp)
    val thumbUrl = "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    Box(
        modifier = modifier
            .shadow(24.dp, containerShape)
            .clip(containerShape)
            .border(1.dp, Color.White.copy(alpha = 0.15f), containerShape)
            .background(Color.Black),
    ) {
        // High-resolution Music Video stage backdrop
        DesktopArtwork(
            url = thumbUrl,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Vignette and gradient scrims
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.65f),
                        0.3f to Color.Black.copy(alpha = 0.20f),
                        0.65f to Color.Black.copy(alpha = 0.35f),
                        1.0f to Color.Black.copy(alpha = 0.85f),
                    ),
                ),
        )

        // Center Action: Launch Native Video / PiP
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .clickable { onLaunchPip?.invoke() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .scale(pulseScale)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE50914))
                        .shadow(12.dp, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Watch MV",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp),
                    )
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.PictureInPictureAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Play in Floating PiP",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        // Top Overlay Header: MV badge, title, browser and close buttons
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.70f), Color.Transparent),
                    ),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE50914))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "MV",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(10.dp))
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Floating PiP Button
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable { onLaunchPip?.invoke() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureInPictureAlt,
                        contentDescription = "Float PiP video player",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Open in External Browser
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable {
                            runCatching {
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().browse(URI("https://www.youtube.com/watch?v=$videoId&t=${startAtSec}s"))
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "Open in browser",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }

                if (onClose != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close video",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }

        // Bottom Overlay: Live audio sync indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = if (isPlaying) Color(0xFF1DB954) else Color.White.copy(alpha = 0.60f),
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isPlaying) "Playing Music Video Audio" else "Paused",
                    color = Color.White.copy(alpha = 0.80f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
