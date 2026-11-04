package com.music.bitchord.desktop

import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DesktopResourcePolicyTest {
    @Test
    fun playheadTicksDoNotChangePageStateButTransportChangesDo() {
        val playing = DesktopPlaybackState(isPlaying = true, durationMs = 60_000)
        val tick = playing.copy(positionMs = 20_000, positionSampledAtNanos = 123)
        assertEquals(playing.withoutPlayhead(), tick.withoutPlayhead())
        assertNotEquals(playing.withoutPlayhead(), tick.copy(isPlaying = false).withoutPlayhead())
        assertNotEquals(playing.withoutPlayhead(), tick.copy(seeks = 1).withoutPlayhead())
    }

    @Test
    fun canvasFrameOwnsItsPixelsWhenDecoderReusesTheInputBuffer() {
        val pixels = byteArrayOf(0, 0, -1, -1)
        val frame = canvasFrame(ImageInfo.makeN32(1, 1, ColorAlphaType.OPAQUE), pixels)
        try {
            pixels.fill(0)
            val output = IntArray(1)
            frame.readPixels(output)
            assertEquals(0xffff0000.toInt(), output[0])
        } finally {
            frame.asSkiaBitmap().close()
        }
    }
}
