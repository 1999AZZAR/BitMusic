package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import java.awt.Desktop
import java.net.URI
import java.util.concurrent.CountDownLatch

object DesktopFxInitializer {
    private val started = CountDownLatch(1)
    private var isStarted = false

    fun ensureStarted() {
        if (isStarted) return
        try {
            Platform.startup {
                started.countDown()
            }
        } catch (_: IllegalStateException) {
            started.countDown()
        }
        Platform.setImplicitExit(false)
        started.await()
        isStarted = true
    }
}

/**
 * High-performance embedded Music Video (MV) player for YouTube Music.
 * Features synchronized audio playback via BitMusic engine, native PiP player launcher,
 * and direct WebKit integration.
 */
@Composable
fun DesktopMusicVideoView(
    videoId: String,
    title: String? = null,
    startAtSec: Long = 0L,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val jfxPanel = remember(videoId) {
        DesktopFxInitializer.ensureStarted()
        val panel = JFXPanel()
        Platform.runLater {
            val webView = WebView()
            webView.isContextMenuEnabled = false
            val engine = webView.engine

            // Spoof desktop Chrome UA so YouTube serves complete desktop embed without mobile restrictions
            engine.userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

            // Direct embed URL with mute=1 for autoplay bypass and sync with BitMusic master audio
            val embedUrl = "https://www.youtube.com/embed/$videoId?autoplay=1&start=$startAtSec&enablejsapi=1&origin=https://music.youtube.com&controls=1&modestbranding=1&rel=0&iv_load_policy=3&mute=1"
            engine.load(embedUrl)

            val scene = Scene(webView)
            panel.scene = scene
        }
        panel
    }

    DisposableEffect(videoId) {
        onDispose {
            Platform.runLater {
                jfxPanel.scene?.root = null
            }
        }
    }

    val containerShape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .shadow(20.dp, containerShape)
            .clip(containerShape)
            .border(1.dp, Color.White.copy(alpha = 0.14f), containerShape)
            .background(Color.Black),
    ) {
        // Embedded WebView video surface
        SwingPanel(
            factory = { jfxPanel },
            modifier = Modifier.fillMaxSize(),
        )

        // Top control overlay bar for MV metadata and PiP/browser actions
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE50914))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "MV",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Native Floating PiP Window (mpv)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable {
                            runCatching {
                                ProcessBuilder(
                                    "mpv",
                                    "--ontop",
                                    "--no-terminal",
                                    "--geometry=640x360-24+24",
                                    "--title=BitMusic MV - ${title ?: "Video"}",
                                    "--ytdl-path=/home/azzar/.local/bin/yt-dlp",
                                    "https://www.youtube.com/watch?v=$videoId",
                                    "--start=$startAtSec",
                                ).start()
                            }
                        },
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
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
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
    }
}
