package com.music.bitchord.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.math.roundToInt

/**
 * Dedicated Compose video surface component that decodes and renders
 * high-definition video frames synchronized to the master audio playhead.
 */
@Composable
internal fun DesktopVideoSurface(
    videoStream: DesktopVideoStream,
    isPlaying: Boolean,
    positionMs: Long,
    isTheater: Boolean = false,
    onToggleTheater: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var frameBitmap by remember(videoStream.url) { mutableStateOf<ImageBitmap?>(null) }
    var isBuffering by remember(videoStream.url) { mutableStateOf(true) }
    val currentPosition by rememberUpdatedState(positionMs)
    val currentlyPlaying by rememberUpdatedState(isPlaying)

    LaunchedEffect(videoStream.url) {
        val decoder = DesktopVideoDecoder()
        val clock = DesktopVideoSyncClock()
        val opened = withContext(Dispatchers.IO) {
            decoder.open(videoStream.url, videoStream.headers)
        }
        if (opened.isFailure) {
            DesktopTrackLog.log("video: failed to open stream ${opened.exceptionOrNull()?.message}")
            isBuffering = false
            withContext(Dispatchers.IO) { decoder.close() }
            return@LaunchedEffect
        }

        var lastAudioMs = currentPosition
        if (lastAudioMs > 1_000L) {
            withContext(Dispatchers.IO) { decoder.seekTo(lastAudioMs) }
        }

        try {
            val pixels = ByteArray(decoder.width * decoder.height * 4)
            val info = ImageInfo.makeN32(decoder.width, decoder.height, ColorAlphaType.OPAQUE)
            while (isActive) {
                // Pause handling: hold current frame without spinning CPU
                snapshotFlow { currentlyPlaying }.first { it }

                val audioMs = currentPosition
                if (clock.isDiscontinuous(lastAudioMs, audioMs)) {
                    isBuffering = true
                    withContext(Dispatchers.IO) { decoder.seekTo(audioMs) }
                    lastAudioMs = audioMs
                }

                val frameMeta = withContext(Dispatchers.IO) { decoder.nextFrame(pixels) }
                if (frameMeta == null) {
                    delay(20L)
                    continue
                }

                when (val action = clock.evaluate(frameMeta.ptsMs, audioMs)) {
                    is DesktopVideoSyncAction.DROP -> {
                        // Drop frame without Skia rasterizing to catch up with audio
                        continue
                    }
                    is DesktopVideoSyncAction.WAIT -> {
                        delay(action.delayMs.coerceIn(5L, 60L))
                        frameBitmap = Image.makeRaster(info, pixels.copyOf(), decoder.width * 4).toComposeImageBitmap()
                        isBuffering = false
                        lastAudioMs = audioMs
                    }
                    DesktopVideoSyncAction.RENDER -> {
                        frameBitmap = Image.makeRaster(info, pixels.copyOf(), decoder.width * 4).toComposeImageBitmap()
                        isBuffering = false
                        lastAudioMs = audioMs
                        val frameInterval = (1000L / decoder.fps).coerceIn(16L, 66L)
                        delay(frameInterval / 2)
                    }
                }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                decoder.close()
            }
        }
    }

    DisposableEffect(videoStream.url) {
        onDispose {
            frameBitmap = null
            isBuffering = true
        }
    }

    val streamAspect = videoStream.aspectRatio.takeIf { it > 0.5f } ?: (16f / 9f)

    Box(
        modifier = modifier
            .aspectRatio(streamAspect)
            .shadow(24.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onToggleTheater?.invoke() }
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val image = frameBitmap ?: return@Canvas
            val canvasAspect = size.width / size.height
            val (drawWidth, drawHeight) = if (streamAspect > canvasAspect) {
                size.width to size.width / streamAspect
            } else {
                size.height * streamAspect to size.height
            }
            val left = (size.width - drawWidth) / 2f
            val top = (size.height - drawHeight) / 2f

            drawImage(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(image.width, image.height),
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(drawWidth.roundToInt(), drawHeight.roundToInt()),
            )
        }

        AnimatedVisibility(
            visible = isBuffering,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            }
        }

        if (onToggleTheater != null) {
            IconButton(
                onClick = onToggleTheater,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
            ) {
                Icon(
                    imageVector = if (isTheater) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                    contentDescription = if (isTheater) "Exit theater mode" else "Theater mode",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
