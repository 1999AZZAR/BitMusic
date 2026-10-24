package com.music.bitchord.desktop

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The visualizer's analyzer, on signals with known spectrums.
 */
class DesktopAudioSpectrumTest {

    @Before
    fun cleanSlate() {
        DesktopAudioSpectrum.reset()
    }

    @Test
    fun `a 440 Hz tone peaks the band 440 Hz belongs to`() {
        val rate = 44_100
        val tone = FloatArray(rate) { sin(2.0 * PI * 440.0 * it / rate).toFloat() * 0.6f }
        // Enough to fill the ring well past one FFT window, interleaved as stereo.
        val stereo = FloatArray(tone.size * 2)
        for (i in tone.indices) {
            stereo[i * 2] = tone[i]
            stereo[i * 2 + 1] = tone[i]
        }
        DesktopAudioSpectrum.offer(stereo, stereo.size, channels = 2, rate = rate)

        val bands = DesktopAudioSpectrum.analyzeSnapshot()
        val loudest = bands.indices.maxBy { bands[it] }

        val expected = bandOf(440.0, rate)
        // Log bands near the bottom are narrow and the Hann window leaks a band either way.
        assertTrue(
            "440 Hz should peak band $expected ± 2 but peaked $loudest: ${bands.joinToString()}",
            kotlin.math.abs(loudest - expected) <= 2,
        )
        assertTrue("the peak band should be clearly lit, not noise", bands[loudest] > 0.3f)
        assertTrue("every band stays within 0..1", bands.all { it in 0f..1f })
    }

    @Test
    fun `an 8 kHz tone peaks the high end, tilt and all`() {
        val rate = 44_100
        val tone = FloatArray(rate) { sin(2.0 * PI * 8_000.0 * it / rate).toFloat() * 0.6f }
        DesktopAudioSpectrum.offer(tone, tone.size, channels = 1, rate = rate)

        val bands = DesktopAudioSpectrum.analyzeSnapshot()
        val loudest = bands.indices.maxBy { bands[it] }

        val expected = bandOf(8_000.0, rate)
        assertTrue(
            "8 kHz should peak band $expected ± 3 but peaked $loudest: ${bands.joinToString()}",
            kotlin.math.abs(loudest - expected) <= 3,
        )
        assertTrue("the peak band should be clearly lit, not noise", bands[loudest] > 0.3f)
    }

    @Test
    fun `silence keeps every band dark`() {
        val rate = 48_000
        DesktopAudioSpectrum.offer(FloatArray(4_096), 4_096, channels = 2, rate = rate)
        val bands = DesktopAudioSpectrum.analyzeSnapshot()
        assertTrue("silence should read flat: ${bands.joinToString()}", bands.all { it < 0.05f })
    }

    /** Which band index a frequency folds into, mirroring the analyzer's log mapping. */
    private fun bandOf(hz: Double, rate: Int): Int {
        val binHz = rate.toDouble() / 2_048
        val low = 2.0
        val high = (12_000.0 / binHz).coerceIn(3.0, 1_023.0)
        val bin = (hz / binHz).coerceIn(low, high)
        val fraction = (kotlin.math.ln(bin) - kotlin.math.ln(low)) / (kotlin.math.ln(high) - kotlin.math.ln(low))
        return (fraction * DesktopAudioSpectrum.BANDS).toInt().coerceIn(0, DesktopAudioSpectrum.BANDS - 1)
    }
}
