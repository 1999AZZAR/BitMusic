package com.music.bitchord.desktop

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.Structure

/**
 * The Linux desktop's own output: PulseAudio's blocking "simple" client.
 *
 * Java Sound under OpenJDK talks raw ALSA — it bypasses PipeWire entirely, steals the
 * device from every other stream and cannot open the one PulseAudio keeps busy. The
 * desktop session's sound runs through PulseAudio/PipeWire, so hand it interleaved
 * 32-bit float PCM and let the server mix, resample and route; the stream follows the
 * default sink, so speakers, headphones and volume keys all just work.
 */
internal object DesktopLinuxAudio {

    private val native: PulseSimple? by lazy {
        if (DesktopPlatform.isLinux) {
            runCatching { Native.load(LIBRARY, PulseSimple::class.java) }
                .onFailure {
                    DesktopTrackLog.log("PulseAudio unavailable; playback falls back to Java Sound: ${it.message}")
                }
                .getOrNull()
        } else {
            null
        }
    }

    private val available: Boolean get() = native != null

    /** The server-side handle, valid until [close]; non-null while this sink holds the output. */
    private var stream: Pointer? = null

    /** The stream's own format; latency arithmetic needs the rate and the channel width. */
    private var sampleRate = 0
    private var channels = 0

    /** Reused float buffer; the playback thread is the only writer. */
    private var staged = FloatArray(0)

    /** Frames handed to the server in total, the base of the honest playback position. */
    private var writtenFrames = 0L

    private var broken = false

    /**
     * Whether the server-side stream is dead after a failed write. The engine watches this and
     * reopens the output: without it, a PulseAudio/PipeWire restart mid-track left every later
     * write failing silently until the next track change or device switch happened to reopen.
     */
    val isBroken: Boolean get() = broken

    /** Opens the default sink for [requested]; on Linux the system route outranks per-device picks. */
    fun open(device: String, requested: DesktopPcmFormat): Result<Int> = runCatching {
        val bindings = available.let { native } ?: error("libpulse-simple is not installed")
        val recovering = broken
        close()
        val spec = PaSampleSpec().apply {
            rate = requested.sampleRate
            format = PA_SAMPLE_FLOAT32LE
            this.channels = requested.channels.toByte()
        }
        val error = intArrayOf(0)
        val opened = bindings.pa_simple_new(
            null,
            "BitMusic",
            PA_STREAM_PLAYBACK,
            null,
            "BitMusic desktop playback",
            spec,
            null,
            null,
            error,
        )
        checkNotNull(opened) { "PulseAudio could not open a playback stream: ${strerror(bindings, error[0])}" }
        stream = opened
        sampleRate = requested.sampleRate
        channels = requested.channels
        writtenFrames = 0
        lastQueuedFrames = 0
        broken = false
        if (recovering) DesktopTrackLog.log("PulseAudio stream re-opened after a failure")
        requested.byteRate / 5
    }

    /** Writes [count] interleaved samples, scaled by [gain]; blocks while the server digests the queue. */
    fun write(samples: FloatArray, count: Int, gain: Float): Int {
        val handle = stream
        val bindings = native
        if (broken || handle == null || bindings == null || count <= 0) return 0
        if (staged.size < count) staged = FloatArray(count)
        System.arraycopy(samples, 0, staged, 0, count)
        if (gain != 1f) {
            for (index in 0 until count) staged[index] *= gain
        }
        val error = intArrayOf(0)
        val result = runCatching {
            bindings.pa_simple_write(handle, staged, NativeLong(count.toLong() * 4), error)
        }
        if (result.getOrNull() != 0) {
            broken = true
            DesktopTrackLog.log(
                "PulseAudio write failed: ${strerror(bindings, error[0])} (${result.exceptionOrNull()?.message})",
            )
            return 0
        }
        writtenFrames += count / channels.coerceAtLeast(1)
        return count
    }

    /** The last latency reading that answered, held for the query's bad days; see [framesPlayed]. */
    private var lastQueuedFrames = 0L

    /**
     * Frames the server has actually finished — everything written, minus what it still holds.
     *
     * A failed query used to read as "nothing queued", which leapt the playhead a queue's length
     * ahead of the music. A query that fails while the stream itself still answers is a hiccup,
     * not a queue that emptied, so the last good reading is held instead.
     */
    fun framesPlayed(): Long {
        val handle = stream
        val bindings = native
        if (handle == null || bindings == null) return 0
        val error = intArrayOf(0)
        val usec = runCatching {
            bindings.pa_simple_get_latency(handle, error).takeIf { error[0] == 0 && it >= 0 }
        }.getOrNull()
        if (usec != null) lastQueuedFrames = usec * sampleRate / 1_000_000
        return (writtenFrames - lastQueuedFrames).coerceAtLeast(0)
    }

    /** What the server is still holding, in bytes — a live read, not a constant. */
    fun bufferedBytes(): Int {
        val frameBytes = 4 * channels.coerceAtLeast(1)
        val queued = (writtenFrames - framesPlayed()).coerceAtLeast(0)
        val bytes = queued * frameBytes
        return bytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun deviceName(): String = "PulseAudio default sink"

    /** The server still holds a mixed-in tail; drop it, so pausing is instant rather than an echo. */
    fun pause() = flush()

    fun resume() {}

    fun flush() {
        val handle = stream
        val bindings = native
        if (handle == null || bindings == null) return
        runCatching { bindings.pa_simple_flush(handle, intArrayOf(0)) }
    }

    fun drain() {
        val handle = stream
        val bindings = native
        if (handle == null || bindings == null) return
        runCatching { bindings.pa_simple_drain(handle, intArrayOf(0)) }
    }

    fun close() {
        val handle = stream
        val bindings = native
        stream = null
        writtenFrames = 0
        lastQueuedFrames = 0
        broken = false
        if (handle != null && bindings != null) runCatching { bindings.pa_simple_free(handle) }
    }

    private fun strerror(bindings: PulseSimple, code: Int): String =
        runCatching { bindings.pa_strerror(code) }.getOrNull() ?: "code $code"

    /** `pa_sample_spec` untouched: format first, then rate, then channels — as libpulse lays it out. */
    class PaSampleSpec : Structure() {
        @JvmField var format: Int = 0
        @JvmField var rate: Int = 0
        @JvmField var channels: Byte = 2

        override fun getFieldOrder(): List<String> = listOf("format", "rate", "channels")
    }

    private interface PulseSimple : Library {
        fun pa_simple_new(
            server: String?,
            name: String,
            direction: Int,
            device: String?,
            streamName: String,
            spec: PaSampleSpec,
            channelMap: Pointer?,
            properties: Pointer?,
            error: IntArray?,
        ): Pointer?

        fun pa_simple_write(handle: Pointer, data: FloatArray, bytes: NativeLong, error: IntArray?): Int
        fun pa_simple_drain(handle: Pointer, error: IntArray?): Int
        fun pa_simple_flush(handle: Pointer, error: IntArray?): Int
        fun pa_simple_get_latency(handle: Pointer, error: IntArray?): Long
        fun pa_simple_free(handle: Pointer)
        fun pa_strerror(code: Int): String
    }
}

/**
 * The full soname of the distribution's runtime library: the unversioned link only
 * exists under libpulse-dev, which a normal desktop install does not carry.
 */
private const val LIBRARY = "libpulse-simple.so.0"

private const val PA_STREAM_PLAYBACK = 1
private const val PA_SAMPLE_FLOAT32LE = 5
