package com.music.bitchord.desktop

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Live spectrum of what the speakers are playing, for the visualizer in the player bar.
 *
 * The engine's audio thread hands over every block it writes to the sink — the finished mix,
 * after every processor and the blend — and this is the only tap there is: no second decode, no
 * native callback, and nothing between the listener and the bars but the FFT below.
 *
 * The audio thread only copies into a ring buffer; the analysis runs on its own dispatcher at
 * frame rate, and only while somebody is actually collecting [bands] — an unobserved visualizer
 * costs nothing.
 */
internal object DesktopAudioSpectrum {

    const val BANDS = 64

    private const val FFT = 2_048
    private const val RING = 4_096
    private const val RING_MASK = RING - 1

    /** ~30 frames a second: quicker reads make the bars shiver rather than dance. */
    private const val FRAME_MS = 33L

    private val lock = Any()

    /** The newest samples last, as mono. */
    private val ring = FloatArray(RING)
    private var fed = 0L
    private var sampleRate = 44_100

    private val _bands = MutableStateFlow(FloatArray(BANDS))
    val bands: StateFlow<FloatArray> = _bands

    /** Per-band attack/release state, owned by the analyzer. */
    private val smoothed = FloatArray(BANDS)

    /** Auto-gain: the smoothed frame peak, so a quiet master still dances and a loud one stays under 1. */
    private var gain = 0.01f

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            while (true) {
                if (_bands.subscriptionCount.value == 0) {
                    delay(250)
                    continue
                }
                // Reduce animation settles the bars to nothing rather than freezing them
                // mid-dance — a frozen field reads as a glitch, an emptying one as a choice.
                if (!DesktopPlayerSettings.reduceAnimation.value) analyzeSnapshot() else decaySnapshot()
                delay(FRAME_MS)
            }
        }
    }

    /**
     * The audio thread's copy of one output block: [count] interleaved samples at [rate]. Mixed
     * to mono and copied straight into the ring — everything expensive happens off this thread.
     */
    fun offer(samples: FloatArray, count: Int, channels: Int, rate: Int) {
        if (count <= 0) return
        val ch = channels.coerceAtLeast(1)
        synchronized(lock) {
            if (rate > 0) sampleRate = rate
            var at = 0
            val frames = count / ch
            for (frame in 0 until frames) {
                var sum = 0f
                for (channel in 0 until ch) sum += samples[at + channel]
                ring[(fed++.toInt()) and RING_MASK] = sum / ch
                at += ch
            }
        }
    }

    /** Back to a never-played state: empty ring, dark bands, the gain at its floor. */
    internal fun reset() {
        synchronized(lock) {
            ring.fill(0f)
            fed = 0L
            sampleRate = 44_100
        }
        smoothed.fill(0f)
        gain = 0.01f
        _bands.value = FloatArray(BANDS)
    }

    /**
     * The reduce-animation answer: no transform, just the release curve running out, so the
     * field glides down to nothing and stays there until animation is allowed again.
     */
    private fun decaySnapshot(): FloatArray {
        val out = FloatArray(BANDS)
        for (band in 0 until BANDS) {
            smoothed[band] *= 0.80f
            out[band] = smoothed[band]
        }
        _bands.value = out
        return out
    }

    /**
     * One frame: window the last [FFT] samples, transform them, and fold the bins into [BANDS]
     * log-spaced bands. Internal only in public company: [analyzeSnapshot] is what the loop and
     * the tests call.
     */
    internal fun analyzeSnapshot(): FloatArray {
        val re = FloatArray(FFT)
        val im = FloatArray(FFT)
        val rate: Int
        synchronized(lock) {
            rate = sampleRate
            // Whatever the ring already holds; a just-started stream analyses its first
            // fractions of a second against silence, which the bars simply show as rising.
            val start = (fed - FFT).coerceAtLeast(0L)
            for (i in 0 until FFT) {
                val sample = if (start + i < fed) ring[((start + i).toInt()) and RING_MASK] else 0f
                val window = 0.5f - 0.5f * cos(2.0 * PI * i / FFT).toFloat()
                re[i] = sample * window
            }
        }
        fft(re, im)

        val bins = FFT / 2
        val binHz = rate.toFloat() / FFT
        // 40-ish Hz to 12-ish kHz: below is mostly rumble the mix rolled off anyway, above is
        // whatever a 44.1/48k master left of its encoding silence.
        val lowBin = 2f
        val highBin = (12_000f / binHz).coerceIn(lowBin + 1f, (bins - 1).toFloat())
        val logLow = ln(lowBin)
        val logHigh = ln(highBin)

        val magnitudes = FloatArray(BANDS)
        var framePeak = 1e-4f
        for (band in 0 until BANDS) {
            val from = exp(logLow + (logHigh - logLow) * band / BANDS).toInt()
            val to = (exp(logLow + (logHigh - logLow) * (band + 1) / BANDS).toInt())
                .coerceIn(from + 1, bins)
            var peak = 0f
            for (bin in from until to) {
                val magnitude = sqrt(re[bin] * re[bin] + im[bin] * im[bin])
                if (magnitude > peak) peak = magnitude
            }
            // Pink-noise tilt: music's energy falls ~3 dB an octave, so an honest spectrum is a
            // ski slope with all the life at the left. Lifting the top bands threefold (+9.5 dB
            // at the last octave pair) turns the slope into the field the ear thinks it hears.
            val tilt = 1f + 2f * band / (BANDS - 1)
            magnitudes[band] = ln(1f + peak * tilt)
            if (magnitudes[band] > framePeak) framePeak = magnitudes[band]
        }

        // The gain follows the music down slowly and jumps up at once, so a chorus arriving
        // never clips and a quiet passage is still visible while it fades.
        gain = maxOf(framePeak, gain * 0.94f)
        val normalizer = 1f / gain
        val out = FloatArray(BANDS)
        for (band in 0 until BANDS) {
            val level = (magnitudes[band] * normalizer).coerceIn(0f, 1f)
            smoothed[band] = if (level > smoothed[band]) level else smoothed[band] * 0.80f + level * 0.20f
            out[band] = smoothed[band]
        }
        _bands.value = out
        return out
    }

    /** Iterative radix-2 FFT in place; [FFT] is a power of two by construction. */
    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                val reSwap = re[i]; re[i] = re[j]; re[j] = reSwap
                val imSwap = im[i]; im[i] = im[j]; im[j] = imSwap
            }
        }
        var len = 2
        while (len <= n) {
            val angle = (-2.0 * PI / len).toFloat()
            val stepRe = cos(angle)
            val stepIm = kotlin.math.sin(angle)
            var base = 0
            while (base < n) {
                var twistRe = 1f
                var twistIm = 0f
                val half = len / 2
                for (k in 0 until half) {
                    val evenRe = re[base + k]
                    val evenIm = im[base + k]
                    val oddRe = re[base + k + half] * twistRe - im[base + k + half] * twistIm
                    val oddIm = re[base + k + half] * twistIm + im[base + k + half] * twistRe
                    re[base + k] = evenRe + oddRe
                    im[base + k] = evenIm + oddIm
                    re[base + k + half] = evenRe - oddRe
                    im[base + k + half] = evenIm - oddIm
                    val nextRe = twistRe * stepRe - twistIm * stepIm
                    twistIm = twistRe * stepIm + twistIm * stepRe
                    twistRe = nextRe
                }
                base += len
            }
            len = len shl 1
        }
    }
}
