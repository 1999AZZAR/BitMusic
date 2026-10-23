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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Embedded Music Video (MV) player for YouTube Music videos.
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

            // HTML5 embedded YouTube player with YouTube IFrame API
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body, html {
                            margin: 0;
                            padding: 0;
                            width: 100%;
                            height: 100%;
                            overflow: hidden;
                            background-color: #000000;
                        }
                        iframe {
                            width: 100%;
                            height: 100%;
                            border: 0;
                        }
                    </style>
                </head>
                <body>
                    <iframe
                        id="ytplayer"
                        type="text/html"
                        src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&start=$startAtSec&enablejsapi=1&origin=https://music.youtube.com&controls=1&modestbranding=1&rel=0&iv_load_policy=3"
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                        allowfullscreen>
                    </iframe>
                </body>
                </html>
            """.trimIndent()

            webView.engine.loadContent(html)
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
            .shadow(16.dp, containerShape)
            .clip(containerShape)
            .border(1.dp, Color.White.copy(alpha = 0.12f), containerShape)
            .background(Color.Black),
    ) {
        // Embedded WebView video surface
        SwingPanel(
            factory = { jfxPanel },
            modifier = Modifier.fillMaxSize(),
        )

        // Subtle top overlay bar for MV title and controls
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFCC0000))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "MV",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Open in External Browser button
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable {
                            runCatching {
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().browse(URI("https://www.youtube.com/watch?v=$videoId"))
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = "Open in browser",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }

                if (onClose != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close video",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
