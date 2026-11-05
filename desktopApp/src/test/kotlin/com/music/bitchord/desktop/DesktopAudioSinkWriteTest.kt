package com.music.bitchord.desktop

import java.lang.reflect.Proxy
import javax.sound.sampled.SourceDataLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DesktopAudioSinkWriteTest {
    private fun sink(write: () -> Int): DesktopAudioSink {
        val line = Proxy.newProxyInstance(
            SourceDataLine::class.java.classLoader,
            arrayOf(SourceDataLine::class.java),
        ) { _, method, _ -> if (method.name == "write") write() else null } as SourceDataLine
        return DesktopAudioSink().also {
            DesktopAudioSink::class.java.getDeclaredField("line").apply { isAccessible = true }.set(it, line)
        }
    }

    @Test fun acceptedBytesAreConvertedToSamples() {
        assertEquals(512, sink { 1024 }.write(FloatArray(1024), 1024))
    }

    @Test fun failedLineBecomesRecoverableWithoutCountingUnwrittenSamples() {
        val sink = sink { throw IllegalStateException("Disconnected") }
        assertEquals(0, sink.write(FloatArray(1024), 1024))
        assertFalse(sink.isOpen)
    }
}
