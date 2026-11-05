package com.music.bitchord.desktop

import com.sun.jna.*
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference

/** Single-owner asynchronous PulseAudio output with bounded server waits. */
internal object DesktopLinuxAudio {
    private val native: Pulse? by lazy {
        if (DesktopPlatform.isLinux)
            runCatching { Native.load("libpulse.so.0", Pulse::class.java) }.getOrNull()
        else null
    }
    private var loop: Pointer? = null
    private var context: Pointer? = null
    private var stream: Pointer? = null
    private var rate = 0
    private var channels = 0
    private var written = 0L
    private var played = 0L
    private var nextTiming = 0L
    private var buffer: Memory? = null
    private var staged = FloatArray(0)
    private val latency = LongByReference()
    private val negative = IntByReference()
    private var broken = false
    val isBroken
        get() = broken

    fun open(device: String, requested: DesktopPcmFormat): Result<Int> = runCatching {
        close()
        val api = native ?: error("libpulse unavailable")
        try {
            val mainloop = checkNotNull(api.pa_mainloop_new())
            loop = mainloop
            val client =
                checkNotNull(api.pa_context_new(api.pa_mainloop_get_api(mainloop), "BitMusic"))
            context = client
            check(api.pa_context_connect(client, null, 1, null) >= 0)
            awaitReady { api.pa_context_get_state(client).also { check(it !in 5..6) } == 4 }
            val spec =
                PaSampleSpec().apply {
                    rate = requested.sampleRate
                    channels = requested.channels.toByte()
                }
            val output =
                checkNotNull(api.pa_stream_new(client, "BitMusic desktop playback", spec, null))
            stream = output
            rate = requested.sampleRate
            channels = requested.channels
            val bytesPerSecond = rate * channels * 4
            val attr =
                PaBufferAttr().apply {
                    tlength = bytesPerSecond / 5
                    minreq = bytesPerSecond / 50
                }
            check(
                api.pa_stream_connect_playback(output, null, attr, 2 or 8 or 8192, null, null) >= 0
            )
            awaitReady { api.pa_stream_get_state(output).also { check(it !in 3..4) } == 2 }
            bytesPerSecond / 5
        } catch (failure: Throwable) {
            close()
            throw failure
        }
    }

    private fun pump() {
        check(native!!.pa_mainloop_iterate(loop!!, 0, null) >= 0) { "PulseAudio event loop failed" }
    }

    private fun awaitReady(ready: () -> Boolean) {
        val deadline = System.nanoTime() + 1_500_000_000L
        while (true) {
            pump()
            if (ready()) return
            check(System.nanoTime() < deadline) { "PulseAudio response timed out" }
            Thread.sleep(4)
        }
    }

    fun write(samples: FloatArray, count: Int, gain: Float): Int {
        if (broken || stream == null || count <= 0) return 0
        var accepted = 0
        runCatching {
                val api = native!!
                val output = stream!!
                val bytes = count * 4L
                if (buffer == null || buffer!!.size() < bytes) {
                    buffer?.close()
                    buffer = Memory(bytes)
                }
                if (gain == 1f) buffer!!.write(0, samples, 0, count)
                else {
                    if (staged.size < count) staged = FloatArray(count)
                    for (i in 0 until count) staged[i] = samples[i] * gain
                    buffer!!.write(0, staged, 0, count)
                }
                val deadline = System.nanoTime() + 1_500_000_000L
                while (accepted < count) {
                    pump()
                    check(
                        api.pa_stream_get_state(output) == 2 &&
                            api.pa_context_get_state(context!!) == 4
                    ) {
                        "PulseAudio disconnected"
                    }
                    val available = api.pa_stream_writable_size(output).toLong()
                    check(available >= 0)
                    val frameBytes = channels * 4L
                    val chunk = minOf(available, (count - accepted) * 4L) / frameBytes * frameBytes
                    if (chunk > 0) {
                        check(
                            api.pa_stream_write(
                                output,
                                buffer!!.share(accepted * 4L),
                                NativeLong(chunk),
                                null,
                                0,
                                0,
                            ) >= 0
                        )
                        accepted += (chunk / 4).toInt()
                        written += chunk / frameBytes
                    } else {
                        check(System.nanoTime() < deadline) {
                            "PulseAudio stopped consuming samples"
                        }
                        Thread.sleep(4)
                    }
                }
            }
            .onFailure {
                broken = true
                DesktopTrackLog.log("PulseAudio output interrupted: ${it.message}")
            }
        return accepted
    }

    fun framesPlayed(): Long {
        if (broken || stream == null) return played
        runCatching {
                pump()
                val now = System.nanoTime()
                if (now >= nextTiming) {
                    nextTiming = now + 50_000_000L
                    native!!.pa_stream_update_timing_info(stream!!, null, null)?.let {
                        native!!.pa_operation_unref(it)
                    }
                }
                if (native!!.pa_stream_get_latency(stream!!, latency, negative) >= 0) {
                    val queued =
                        if (negative.value != 0) 0L
                        else latency.value.coerceAtLeast(0) * rate / 1_000_000
                    played = maxOf(played, (written - queued).coerceIn(0, written))
                }
            }
            .onFailure { broken = true }
        return played
    }


    fun deviceName() = "PulseAudio default sink"

    fun pause() = flush()

    fun resume() {}

    fun flush() =
        operation { api, output -> api.pa_stream_flush(output, null, null) }
            .also { played = written }

    fun drain() = operation { api, output -> api.pa_stream_drain(output, null, null) }

    private fun operation(start: (Pulse, Pointer) -> Pointer?) {
        if (broken || stream == null) return
        runCatching {
                start(native!!, stream!!)?.let { operation ->
                    try {
                        awaitReady { native!!.pa_operation_get_state(operation) != 0 }
                    } finally {
                        native!!.pa_operation_unref(operation)
                    }
                }
            }
            .onFailure { broken = true }
    }

    fun close() {
        val api = native
        stream?.let {
            api?.pa_stream_disconnect(it)
            api?.pa_stream_unref(it)
        }
        stream = null
        context?.let {
            api?.pa_context_disconnect(it)
            api?.pa_context_unref(it)
        }
        context = null
        loop?.let { api?.pa_mainloop_free(it) }
        loop = null
        buffer?.close()
        buffer = null
        staged = FloatArray(0)
        written = 0
        played = 0
        nextTiming = 0
        broken = false
    }

    class PaSampleSpec : Structure() {
        @JvmField var format = 5
        @JvmField var rate = 44_100
        @JvmField var channels: Byte = 2

        override fun getFieldOrder() = listOf("format", "rate", "channels")
    }

    class PaBufferAttr : Structure() {
        @JvmField var maxlength = -1
        @JvmField var tlength = -1
        @JvmField var prebuf = -1
        @JvmField var minreq = -1
        @JvmField var fragsize = -1

        override fun getFieldOrder() =
            listOf("maxlength", "tlength", "prebuf", "minreq", "fragsize")
    }

    private interface Pulse : Library {
        fun pa_mainloop_new(): Pointer?

        fun pa_mainloop_get_api(loop: Pointer): Pointer

        fun pa_mainloop_iterate(loop: Pointer, block: Int, result: Pointer?): Int

        fun pa_mainloop_free(loop: Pointer)

        fun pa_context_new(api: Pointer, name: String): Pointer?

        fun pa_context_connect(context: Pointer, server: String?, flags: Int, api: Pointer?): Int

        fun pa_context_get_state(context: Pointer): Int

        fun pa_context_disconnect(context: Pointer)

        fun pa_context_unref(context: Pointer)

        fun pa_stream_new(
            context: Pointer,
            name: String,
            spec: PaSampleSpec,
            map: Pointer?,
        ): Pointer?

        fun pa_stream_connect_playback(
            stream: Pointer,
            device: String?,
            attr: PaBufferAttr,
            flags: Int,
            volume: Pointer?,
            sync: Pointer?,
        ): Int

        fun pa_stream_get_state(stream: Pointer): Int

        fun pa_stream_writable_size(stream: Pointer): NativeLong

        fun pa_stream_write(
            stream: Pointer,
            data: Pointer,
            bytes: NativeLong,
            free: Pointer?,
            offset: Long,
            seek: Int,
        ): Int

        fun pa_stream_get_latency(
            stream: Pointer,
            latency: LongByReference,
            negative: IntByReference,
        ): Int

        fun pa_stream_update_timing_info(
            stream: Pointer,
            callback: Pointer?,
            data: Pointer?,
        ): Pointer?

        fun pa_stream_flush(stream: Pointer, callback: Pointer?, data: Pointer?): Pointer?

        fun pa_stream_drain(stream: Pointer, callback: Pointer?, data: Pointer?): Pointer?

        fun pa_stream_disconnect(stream: Pointer): Int

        fun pa_stream_unref(stream: Pointer)

        fun pa_operation_get_state(operation: Pointer): Int

        fun pa_operation_unref(operation: Pointer)
    }
}
