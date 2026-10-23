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
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sun.jna.Native
import java.awt.BorderLayout
import java.awt.Canvas
import java.awt.Color as AwtColor
import java.awt.Desktop
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.net.URI
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import javax.swing.JPanel

/**
 * Controller for mpv playback: embedded inside Compose Desktop window via AWT Canvas or floating PiP.
 * Zero-deadlock, hardware accelerated on Linux X11/Wayland with full IPC control.
 */
object DesktopMpvPlayer {
    private var currentProcess: Process? = null
    private var socketPath: String? = null
    private var startedAtEpochMs: Long = 0L
    private var startOffsetSec: Long = 0L
    private var isPaused: Boolean = false
    private var pausedPositionSec: Long = 0L

    private val ytDlpPath: String by lazy {
        listOf(
            "/home/azzar/.local/bin/yt-dlp",
            "/usr/local/bin/yt-dlp",
            "/usr/bin/yt-dlp",
        ).firstOrNull { File(it).canExecute() } ?: "yt-dlp"
    }

    fun play(
        videoId: String,
        startAtSec: Long = 0L,
        wid: Long? = null,
        title: String? = null,
        volume: Float = 1.0f,
        onStart: () -> Unit = {},
        onExit: () -> Unit = {},
    ) {
        stop()
        val sock = "/tmp/bitmusic-mpv-${System.currentTimeMillis()}.sock"
        socketPath = sock
        startOffsetSec = startAtSec
        startedAtEpochMs = System.currentTimeMillis()
        isPaused = false

        try {
            val cmd = mutableListOf(
                "mpv",
                "--no-terminal",
                "--vo=gpu",
                "--hwdec=auto",
                "--keep-open=yes",
                "--title=BitMusic MV - ${title ?: "Video"}",
                "--script-opts=ytdl_hook-ytdl_path=$ytDlpPath",
                "--input-ipc-server=$sock",
                "--volume=${(volume * 100).toInt().coerceIn(0, 100)}",
            )
            if (wid != null && wid > 0) {
                cmd.add("--wid=$wid")
            } else {
                cmd.add("--ontop")
                cmd.add("--geometry=640x360-24+24")
            }
            if (startAtSec > 0) {
                cmd.add("--start=$startAtSec")
            }
            cmd.add("https://www.youtube.com/watch?v=$videoId")

            val pb = ProcessBuilder(cmd)
            val env = pb.environment()
            val existingPath = env["PATH"] ?: System.getenv("PATH") ?: ""
            if (!existingPath.contains("/home/azzar/.local/bin")) {
                env["PATH"] = "/home/azzar/.local/bin:$existingPath"
            }
            val process = pb.start()
            currentProcess = process
            onStart()

            Thread {
                runCatching {
                    process.waitFor()
                    cleanupSocket(sock)
                    onExit()
                }
            }.start()
        } catch (e: Exception) {
            DesktopTrackLog.log("mpv launch failed: ${e.message}")
            cleanupSocket(sock)
            runCatching {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI("https://www.youtube.com/watch?v=$videoId&t=${startAtSec}s"))
                }
            }
        }
    }

    fun setPause(pause: Boolean) {
        isPaused = pause
        if (pause) {
            pausedPositionSec = (currentPositionMs() / 1000L).coerceAtLeast(0L)
        } else {
            startOffsetSec = pausedPositionSec
            startedAtEpochMs = System.currentTimeMillis()
        }
        sendIpcCommand("""{"command": ["set_property", "pause", $pause]}""")
    }

    fun setVolume(vol: Int) {
        sendIpcCommand("""{"command": ["set_property", "volume", ${vol.coerceIn(0, 100)}]}""")
    }

    fun seek(seconds: Long) {
        startOffsetSec = seconds
        startedAtEpochMs = System.currentTimeMillis()
        sendIpcCommand("""{"command": ["seek", $seconds, "absolute"]}""")
    }

    fun currentPositionMs(): Long {
        if (!isPlaying()) return startOffsetSec * 1000L
        if (isPaused) return pausedPositionSec * 1000L
        val elapsed = (System.currentTimeMillis() - startedAtEpochMs).coerceAtLeast(0L)
        return (startOffsetSec * 1000L) + elapsed
    }

    fun isPlaying(): Boolean = currentProcess?.isAlive == true

    fun stop() {
        val sock = socketPath
        runCatching {
            sendIpcCommand("""{"command": ["quit"]}""")
            currentProcess?.destroy()
            currentProcess = null
        }
        if (sock != null) {
            cleanupSocket(sock)
            socketPath = null
        }
    }

    private fun sendIpcCommand(json: String) {
        val sock = socketPath ?: return
        try {
            val addr = UnixDomainSocketAddress.of(sock)
            SocketChannel.open(addr).use { ch ->
                ch.write(ByteBuffer.wrap("$json\n".toByteArray(Charsets.UTF_8)))
            }
        } catch (_: Exception) {}
    }

    private fun cleanupSocket(path: String) {
        runCatching {
            File(path).delete()
        }
    }
}

/**
 * YouTube Music-style Music Video player for Linux desktop.
 * Embeds hardware-accelerated video directly into the view using AWT Canvas & MPV --wid.
 */
@Composable
fun DesktopMusicVideoView(
    videoId: String,
    title: String? = null,
    startAtSec: Long = 0L,
    isPlaying: Boolean = true,
    volume: Float = 1.0f,
    onStartVideo: (() -> Unit)? = null,
    onExitVideo: (() -> Unit)? = null,
    onTogglePlayPause: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var isFloatingPip by remember { mutableStateOf(false) }
    var isMpvActive by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying) {
        if (isMpvActive) {
            DesktopMpvPlayer.setPause(!isPlaying)
        }
    }

    LaunchedEffect(volume) {
        if (isMpvActive) {
            DesktopMpvPlayer.setVolume((volume * 100).toInt())
        }
    }

    DisposableEffect(videoId) {
        onDispose {
            DesktopMpvPlayer.stop()
        }
    }

    val containerShape = RoundedCornerShape(16.dp)
    val thumbUrl = "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"

    Column(
        modifier = modifier
            .shadow(24.dp, containerShape)
            .clip(containerShape)
            .border(1.dp, Color.White.copy(alpha = 0.15f), containerShape)
            .background(Color.Black),
    ) {
        // Top Header Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(Color(0xFF161616))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE50914))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "MV",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
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
                // Floating PiP Mode toggle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isFloatingPip) Color(0xFF1DB954) else Color.White.copy(alpha = 0.15f))
                        .clickable {
                            val currentPos = (DesktopMpvPlayer.currentPositionMs() / 1000L).coerceAtLeast(0L)
                            if (!isFloatingPip) {
                                isFloatingPip = true
                                DesktopMpvPlayer.play(
                                    videoId = videoId,
                                    startAtSec = currentPos,
                                    wid = null,
                                    title = title,
                                    volume = volume,
                                    onStart = {
                                        isMpvActive = true
                                        onStartVideo?.invoke()
                                    },
                                    onExit = {
                                        isMpvActive = false
                                        isFloatingPip = false
                                        onExitVideo?.invoke()
                                    },
                                )
                            } else {
                                isFloatingPip = false
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureInPictureAlt,
                        contentDescription = "Float PiP video player",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Open in External Browser
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable {
                            runCatching {
                                val sec = (DesktopMpvPlayer.currentPositionMs() / 1000L).coerceAtLeast(0L)
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().browse(URI("https://www.youtube.com/watch?v=$videoId&t=${sec}s"))
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "Open in browser",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }

                if (onClose != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
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

        // Video Stage Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
        ) {
            if (!isFloatingPip) {
                // Background thumbnail + loading spinner while mpv resolves and hooks window
                if (!isMpvActive) {
                    DesktopArtwork(
                        url = thumbUrl,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Loading Music Video...",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                // Native Hardware-Accelerated Video Surface
                SwingPanel(
                    factory = {
                        val panel = JPanel(BorderLayout()).apply {
                            background = AwtColor.BLACK
                        }
                        val canvas = Canvas().apply {
                            background = AwtColor.BLACK
                        }
                        panel.add(canvas, BorderLayout.CENTER)

                        canvas.addMouseListener(object : MouseAdapter() {
                            override fun mouseClicked(e: MouseEvent) {
                                if (e.clickCount == 1) {
                                    onTogglePlayPause?.invoke()
                                }
                            }
                        })

                        canvas.addHierarchyListener(object : HierarchyListener {
                            override fun hierarchyChanged(e: HierarchyEvent) {
                                if ((e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong()) != 0L && canvas.isDisplayable) {
                                    val wid = runCatching { Native.getComponentID(canvas) }.getOrNull()
                                    if (wid != null && wid > 0) {
                                        DesktopMpvPlayer.play(
                                            videoId = videoId,
                                            startAtSec = startAtSec,
                                            wid = wid,
                                            title = title,
                                            volume = volume,
                                            onStart = {
                                                isMpvActive = true
                                                onStartVideo?.invoke()
                                            },
                                            onExit = {
                                                isMpvActive = false
                                                onExitVideo?.invoke()
                                            },
                                        )
                                    }
                                }
                            }
                        })
                        panel
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // Floating PiP Status Overlay
                DesktopArtwork(
                    url = thumbUrl,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.PictureInPictureAlt,
                            contentDescription = null,
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Playing in Floating PiP Window",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Click to re-embed video in BitMusic",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clickable {
                                    isFloatingPip = false
                                }
                                .padding(6.dp),
                        )
                    }
                }
            }
        }
    }
}
