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
import androidx.compose.foundation.layout.fillMaxHeight
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.math.roundToInt

/**
 * Dedicated Compose video surface component that decodes and renders
 * high-definition video frames synchronized to the master audio playhead.
 * Offloads all decoding and Skia image rasterization entirely onto Dispatchers.IO
 * so the main UI thread remains completely fluid.
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
        withContext(Dispatchers.IO) {
            val decoder = DesktopVideoDecoder()
            val clock = DesktopVideoSyncClock()
            val opened = decoder.open(videoStream.url, videoStream.headers)
            if (opened.isFailure) {
                DesktopTrackLog.log("video: failed to open stream ${opened.exceptionOrNull()?.message}")
                withContext(Dispatchers.Main) { isBuffering = false }
                decoder.close()
                return@withContext
            }

            var lastAudioMs = currentPosition
            if (lastAudioMs > 1_000L) {
                decoder.seekTo(lastAudioMs)
            }

            try {
                val pixels = ByteArray(decoder.width * decoder.height * 4)
                val info = ImageInfo.makeN32(decoder.width, decoder.height, ColorAlphaType.OPAQUE)
                while (isActive) {
                    // Pause handling: hold current frame without spinning CPU
                    if (!currentlyPlaying) {
                        delay(40L)
                        continue
                    }

                    val audioMs = currentPosition
                    if (clock.isDiscontinuous(lastAudioMs, audioMs)) {
                        withContext(Dispatchers.Main) { isBuffering = true }
                        decoder.seekTo(audioMs)
                        lastAudioMs = audioMs
                    }

                    val frameMeta = decoder.nextFrame(pixels)
                    if (frameMeta == null) {
                        delay(20L)
                        continue
                    }

                    when (val action = clock.evaluate(frameMeta.ptsMs, audioMs)) {
                        is DesktopVideoSyncAction.DROP -> {
                            yield()
                            continue
                        }
                        is DesktopVideoSyncAction.WAIT -> {
                            delay(action.delayMs.coerceIn(5L, 60L))
                            val newBitmap = Image.makeRaster(info, pixels.copyOf(), decoder.width * 4).toComposeImageBitmap()
                            withContext(Dispatchers.Main) {
                                frameBitmap = newBitmap
                                isBuffering = false
                            }
                            lastAudioMs = audioMs
                        }
                        DesktopVideoSyncAction.RENDER -> {
                            val newBitmap = Image.makeRaster(info, pixels.copyOf(), decoder.width * 4).toComposeImageBitmap()
                            withContext(Dispatchers.Main) {
                                frameBitmap = newBitmap
                                isBuffering = false
                            }
                            lastAudioMs = audioMs
                            val frameInterval = (1000L / decoder.fps).coerceIn(16L, 66L)
                            delay(frameInterval / 2)
                        }
                    }
                }
            } finally {
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
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = (if (isTheater) Modifier.fillMaxHeight() else Modifier)
                .aspectRatio(streamAspect, matchHeightConstraintsFirst = isTheater)
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
                val (drawWidth, drawHeight) = if (canvasAspect > streamAspect) {
                    size.height * streamAspect to size.height
                } else {
                    size.width to size.width / streamAspect
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
}
