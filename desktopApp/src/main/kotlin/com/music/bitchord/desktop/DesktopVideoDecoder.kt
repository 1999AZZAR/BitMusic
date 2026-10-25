package com.music.bitchord.desktop

import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avformat.AVIOContext
import org.bytedeco.ffmpeg.avformat.Read_packet_Pointer_BytePointer_int
import org.bytedeco.ffmpeg.avformat.Seek_Pointer_long_int
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.global.avcodec.AVDISCARD_ALL
import org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc
import org.bytedeco.ffmpeg.global.avcodec.av_packet_free
import org.bytedeco.ffmpeg.global.avcodec.av_packet_unref
import org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3
import org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder
import org.bytedeco.ffmpeg.global.avcodec.avcodec_flush_buffers
import org.bytedeco.ffmpeg.global.avcodec.avcodec_free_context
import org.bytedeco.ffmpeg.global.avcodec.avcodec_open2
import org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_to_context
import org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_frame
import org.bytedeco.ffmpeg.global.avcodec.avcodec_send_packet
import org.bytedeco.ffmpeg.global.avformat.AVSEEK_FLAG_BACKWARD
import org.bytedeco.ffmpeg.global.avformat.AVSEEK_SIZE
import org.bytedeco.ffmpeg.global.avformat.av_find_best_stream
import org.bytedeco.ffmpeg.global.avformat.av_read_frame
import org.bytedeco.ffmpeg.global.avformat.av_seek_frame
import org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context
import org.bytedeco.ffmpeg.global.avformat.avformat_close_input
import org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info
import org.bytedeco.ffmpeg.global.avformat.avformat_open_input
import org.bytedeco.ffmpeg.global.avformat.avio_alloc_context
import org.bytedeco.ffmpeg.global.avformat.avio_context_free
import org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF
import org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO
import org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE
import org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_BGRA
import org.bytedeco.ffmpeg.global.avutil.av_dict_set
import org.bytedeco.ffmpeg.global.avutil.av_frame_alloc
import org.bytedeco.ffmpeg.global.avutil.av_frame_free
import org.bytedeco.ffmpeg.global.avutil.av_free
import org.bytedeco.ffmpeg.global.avutil.av_image_fill_arrays
import org.bytedeco.ffmpeg.global.avutil.av_image_get_buffer_size
import org.bytedeco.ffmpeg.global.avutil.av_malloc
import org.bytedeco.ffmpeg.global.swscale.SWS_BILINEAR
import org.bytedeco.ffmpeg.global.swscale.sws_freeContext
import org.bytedeco.ffmpeg.global.swscale.sws_getContext
import org.bytedeco.ffmpeg.global.swscale.sws_scale
import org.bytedeco.ffmpeg.swscale.SwsContext
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.DoublePointer
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.PointerPointer

/**
 * Decoded video frame metadata.
 */
data class DesktopDecodedVideoFrame(
    val ptsMs: Long,
    val isKeyFrame: Boolean,
)

/**
 * Dedicated video decoder wrapping FFmpeg demuxer, H.264/video codec context,
 * and SwsContext colorspace converter for desktop video playback.
 * Leverages [DesktopRangeStream] to request bounded chunk windows via HTTP
 * with zero native stalls or 403 Forbidden errors.
 */
internal class DesktopVideoDecoder {

    private var format: AVFormatContext? = null
    private var codec: AVCodecContext? = null
    private var packet: AVPacket? = null
    private var frame: AVFrame? = null
    private var scaler: SwsContext? = null
    private var buffer: BytePointer? = null
    private var view: java.nio.ByteBuffer? = null
    private var planes: PointerPointer<BytePointer>? = null
    private var strides: IntPointer? = null
    private var rangeSource: DesktopRangeStream? = null
    private var readPacket: Read_packet_Pointer_BytePointer_int? = null
    private var seekPacket: Seek_Pointer_long_int? = null
    private var avio: AVIOContext? = null

    private var scalerFormat = -1
    private var sourceWidth = 0
    private var sourceHeight = 0
    private var streamIndex = -1
    private var timeBaseNum = 0
    private var timeBaseDen = 0
    private var lastPtsMs = 0L

    var width: Int = 0
        private set

    var height: Int = 0
        private set

    var fps: Int = 30
        private set

    val isOpened: Boolean
        get() = format != null && codec != null

    fun open(url: String, headers: Map<String, String> = emptyMap()): Result<Unit> = runCatching {
        DesktopAudioDecoder.ensureNetworkReady()
        val isHttp = url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)

        val opened = if (isHttp) {
            windowedContext(url, headers)
        } else {
            val options = AVDictionary(null)
            if (headers.isNotEmpty()) {
                val block = headers.entries.joinToString("") { (k, v) -> "$k: $v\r\n" }
                av_dict_set(options, "headers", block, 0)
                headers["User-Agent"]?.let { av_dict_set(options, "user_agent", it, 0) }
            }
            av_dict_set(options, "rw_timeout", "15000000", 0)
            val ctx = AVFormatContext(null)
            check(avformat_open_input(ctx, url, null, options) >= 0) { "could not open video stream" }
            ctx
        }
        format = opened

        if (isHttp) {
            check(avformat_open_input(opened, null as String?, null, null as AVDictionary?) >= 0) {
                "could not open windowed video stream"
            }
        }

        check(avformat_find_stream_info(opened, null as AVDictionary?) >= 0) { "no video stream info" }
        streamIndex = av_find_best_stream(
            opened, AVMEDIA_TYPE_VIDEO, -1, -1, null as org.bytedeco.ffmpeg.avcodec.AVCodec?, 0,
        )
        check(streamIndex >= 0) { "no video track in stream" }

        // Discard non-video streams so FFmpeg doesn't demux or buffer them
        for (i in 0 until opened.nb_streams()) {
            if (i != streamIndex) opened.streams(i).discard(AVDISCARD_ALL)
        }

        val stream = opened.streams(streamIndex)
        val parameters = stream.codecpar()
        val decoder = avcodec_find_decoder(parameters.codec_id()) ?: error("no video decoder available")
        val context = avcodec_alloc_context3(decoder)
        check(avcodec_parameters_to_context(context, parameters) >= 0) { "bad video codec parameters" }
        check(avcodec_open2(context, decoder, null as AVDictionary?) >= 0) { "video decoder failed to open" }
        codec = context

        sourceWidth = context.width()
        sourceHeight = context.height()
        check(sourceWidth > 0 && sourceHeight > 0) { "video stream has invalid dimensions" }

        // Bounded resolution for optimal performance (e.g. 720p HD: 1280x720)
        val scale = minOf(1.0, minOf(MAX_WIDTH.toDouble() / sourceWidth, MAX_HEIGHT.toDouble() / sourceHeight))
        width = ((sourceWidth * scale).toInt() / 2) * 2
        height = ((sourceHeight * scale).toInt() / 2) * 2

        val rate = stream.avg_frame_rate()
        if (rate.num() > 0 && rate.den() > 0) {
            fps = (rate.num() / rate.den()).coerceIn(15, 60)
        }

        val tb = stream.time_base()
        timeBaseNum = tb.num()
        timeBaseDen = tb.den()

        val size = av_image_get_buffer_size(AV_PIX_FMT_BGRA, width, height, 1)
        buffer = BytePointer(size.toLong())
        view = buffer?.asByteBuffer()
        planes = PointerPointer<BytePointer>(4)
        strides = IntPointer(4L)
        check(
            av_image_fill_arrays(planes, strides, buffer, AV_PIX_FMT_BGRA, width, height, 1) >= 0,
        ) { "could not allocate video frame buffer" }
        packet = av_packet_alloc()
        frame = av_frame_alloc()
    }.onFailure { close() }

    private fun windowedContext(url: String, headers: Map<String, String>): AVFormatContext {
        val source = DesktopRangeStream(url, headers, windowBytes = 1024 * 1024)
        val read = object : Read_packet_Pointer_BytePointer_int() {
            override fun call(opaque: Pointer?, buf: BytePointer?, count: Int): Int {
                if (buf == null || count <= 0) return 0
                val block = ByteArray(count)
                val taken = source.read(block, count)
                if (taken <= 0) return AVERROR_EOF
                buf.put(block, 0, taken)
                return taken
            }
        }
        val seek = object : Seek_Pointer_long_int() {
            override fun call(opaque: Pointer?, offset: Long, whence: Int): Long {
                if (whence == AVSEEK_SIZE) return source.length
                val target = when (whence) {
                    SEEK_CUR -> source.position() + offset
                    SEEK_END -> source.length + offset
                    else -> offset
                }
                source.seek(target)
                return target
            }
        }
        val ioBuf = BytePointer(av_malloc(IO_BUFFER_BYTES.toLong()))
        val io = avio_alloc_context(ioBuf, IO_BUFFER_BYTES, 0, null, read, null, seek)
        val context = avformat_alloc_context()
        context.pb(io)
        rangeSource = source
        readPacket = read
        seekPacket = seek
        avio = io
        return context
    }

    fun seekTo(targetMs: Long): Boolean {
        val container = format ?: return false
        val context = codec ?: return false
        if (timeBaseNum <= 0 || timeBaseDen <= 0) return false
        val targetPts = (targetMs * timeBaseDen) / (timeBaseNum * 1000L)
        val res = av_seek_frame(container, streamIndex, targetPts, AVSEEK_FLAG_BACKWARD)
        avcodec_flush_buffers(context)
        lastPtsMs = targetMs
        return res >= 0
    }

    /**
     * Decodes the next available frame into [into] (BGRA bytes).
     * Returns frame PTS and metadata, or null at EOF/error.
     */
    fun nextFrame(into: ByteArray): DesktopDecodedVideoFrame? {
        val container = format ?: return null
        val context = codec ?: return null
        val pkt = packet ?: return null
        val decoded = frame ?: return null

        while (true) {
            if (avcodec_receive_frame(context, decoded) == 0) {
                val converter = scalerFor(decoded.format()) ?: return null
                sws_scale(converter, decoded.data(), decoded.linesize(), 0, sourceHeight, planes, strides)
                view?.let { pixels ->
                    pixels.rewind()
                    pixels.get(into, 0, minOf(into.size, width * height * 4))
                }
                val stamp = decoded.best_effort_timestamp().takeIf { it != AV_NOPTS_VALUE } ?: decoded.pts()
                val ptsMs = if (timeBaseNum > 0 && timeBaseDen > 0 && stamp != AV_NOPTS_VALUE) {
                    (stamp * timeBaseNum * 1000L) / timeBaseDen
                } else {
                    lastPtsMs + (1000L / fps)
                }
                lastPtsMs = ptsMs
                @Suppress("DEPRECATION")
                val isKey = decoded.key_frame() != 0
                return DesktopDecodedVideoFrame(ptsMs = ptsMs, isKeyFrame = isKey)
            }
            val readRes = av_read_frame(container, pkt)
            if (readRes < 0) return null
            if (pkt.stream_index() == streamIndex) {
                avcodec_send_packet(context, pkt)
            }
            av_packet_unref(pkt)
        }
    }

    private fun scalerFor(pixelFormat: Int): SwsContext? {
        scaler?.let { if (pixelFormat == scalerFormat) return it }
        if (pixelFormat < 0) return null
        scaler?.let { sws_freeContext(it) }
        val built = sws_getContext(
            sourceWidth, sourceHeight, pixelFormat,
            width, height, AV_PIX_FMT_BGRA,
            SWS_BILINEAR, null, null, null as DoublePointer?,
        )
        if (built == null) {
            DesktopTrackLog.log("video: no scaler for pixel format $pixelFormat")
            scaler = null
            return null
        }
        scaler = built
        scalerFormat = pixelFormat
        return built
    }

    fun close() {
        frame?.let { av_frame_free(it) }
        packet?.let { av_packet_free(it) }
        scaler?.let { sws_freeContext(it) }
        codec?.let { avcodec_free_context(it) }
        format?.let { avformat_close_input(it) }
        buffer?.deallocate()
        strides?.deallocate()
        avio?.let {
            av_free(it.buffer())
            it.buffer(null as BytePointer?)
            avio_context_free(it)
        }
        readPacket?.deallocate()
        seekPacket?.deallocate()
        readPacket = null
        seekPacket = null
        avio = null
        rangeSource = null
        frame = null
        packet = null
        scaler = null
        scalerFormat = -1
        codec = null
        format = null
        buffer = null
        view = null
        planes = null
        strides = null
        streamIndex = -1
        sourceWidth = 0
        sourceHeight = 0
        timeBaseNum = 0
        timeBaseDen = 0
        lastPtsMs = 0L
    }

    companion object {
        const val MAX_WIDTH = 1280
        const val MAX_HEIGHT = 720
        const val IO_BUFFER_BYTES = 64 * 1024
        const val SEEK_CUR = 1
        const val SEEK_END = 2
    }
}
