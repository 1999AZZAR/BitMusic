package com.music.bitchord.desktop

import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopVideoBitmapTest {

    @Test
    fun testBitmapDoubleBufferInstallPixels() {
        val width = 100
        val height = 100
        val info = ImageInfo.makeN32(width, height, ColorAlphaType.OPAQUE)
        val bufferA = ByteArray(width * height * 4)
        val bufferB = ByteArray(width * height * 4)

        val bitmapA = Bitmap()
        val composeA = bitmapA.asComposeImageBitmap()

        val bitmapB = Bitmap()
        val composeB = bitmapB.asComposeImageBitmap()

        // Fill bufferA with red
        for (i in 0 until (width * height)) {
            bufferA[i * 4 + 0] = 0 // B
            bufferA[i * 4 + 1] = 0 // G
            bufferA[i * 4 + 2] = 255.toByte() // R
            bufferA[i * 4 + 3] = 255.toByte() // A
        }
        bitmapA.installPixels(info, bufferA, width * 4)

        // Verify pixel from composeA
        assertEquals(width, composeA.width)
        assertEquals(height, composeA.height)
        val pixel = bitmapA.getColor(50, 50)
        // Check red channel is 255
        val red = (pixel shr 16) and 0xFF
        assertEquals(255, red)

        bitmapA.close()
        bitmapB.close()
    }
}
