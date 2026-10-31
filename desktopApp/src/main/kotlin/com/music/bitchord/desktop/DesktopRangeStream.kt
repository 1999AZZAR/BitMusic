package com.music.bitchord.desktop

import com.music.bitchord.data.Http
import com.music.bitchord.data.innertube.StreamResolver
import okhttp3.Request

/** Reads a remote file in bounded windows rather than as one open-ended request. */
internal class DesktopRangeStream(
    private val url: String,
    private val headers: Map<String, String> = emptyMap(),
    private val windowBytes: Int = DEFAULT_WINDOW,
) {

    /** Total length in bytes, or -1 until the first response has stated one. */
    var length: Long = -1L
        private set

    private var position = 0L
    private var window = ByteArray(0)

    /** Where [window] starts in the file; -1 when nothing is held. */
    private var windowStart = -1L

    /** Where the next read will come from. */
    fun position(): Long = position

    fun seek(offset: Long) {
        position = offset.coerceAtLeast(0)
    }

    /**
     * Fills up to [count] bytes at the current position, returning how many — or -1 at the end of
     * the file.
     */
    fun read(into: ByteArray, count: Int): Int {
        if (count <= 0) return 0
        if (length >= 0 && position >= length) return -1
        if (!holds(position) && !fetchWindowFor(position)) return -1
        val offset = (position - windowStart).toInt()
        val available = window.size - offset
        if (available <= 0) return -1
        val taken = minOf(count, available)
        System.arraycopy(window, offset, into, 0, taken)
        position += taken
        return taken
    }

    private fun holds(at: Long): Boolean =
        windowStart >= 0 && at >= windowStart && at < windowStart + window.size

    /** Fetches the window containing [at], with automatic retry and error handling. */
    private fun fetchWindowFor(at: Long): Boolean {
        val start = at - (at % windowBytes)
        val end = start + windowBytes - 1
        val builder = Request.Builder()
            .url(url)
            .header("Range", "bytes=$start-$end")
            .header("Accept", "*/*")
            .header("Accept-Encoding", "identity")
        headers.forEach { (name, value) -> builder.header(name, value) }
        val request = builder.build()

        var attempts = 0
        val maxAttempts = 4
        while (attempts < maxAttempts) {
            attempts++
            try {
                Http.client.newCall(request).execute().use { response ->
                    val code = response.code
                    if (code == 403 || code == 404 || code == 410) {
                        StreamResolver.onPlaybackRefused(url, code)
                        return false
                    }
                    if (code !in 200..299) {
                        if (attempts < maxAttempts && (code in 500..599 || code == 429)) {
                            Thread.sleep(350L * attempts)
                            return@use
                        }
                        StreamResolver.onPlaybackRefused(url, code)
                        return false
                    }

                    val body = response.body.bytes()
                    val stated = response.header("Content-Range")
                    if (stated != null) {
                        windowStart = stated.substringAfter("bytes ")
                            .substringBefore('-')
                            .trim()
                            .toLongOrNull() ?: start
                        length = stated.substringAfterLast('/')
                            .trim()
                            .toLongOrNull() ?: length
                    } else {
                        windowStart = 0
                        length = body.size.toLong()
                    }
                    window = body
                    if (length < 0) length = windowStart + body.size
                    return body.isNotEmpty()
                }
            } catch (e: Exception) {
                Http.client.connectionPool.evictAll()
                if (attempts >= maxAttempts) {
                    DesktopTrackLog.log("DesktopRangeStream fetch failed at byte $start after $attempts attempts: ${e.message}")
                    return false
                }
                try {
                    Thread.sleep(400L * attempts)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return false
                }
            }
        }
        return false
    }

    private companion object {
        /** Comfortably inside what Google will serve. */
        const val DEFAULT_WINDOW = 512 * 1024
    }
}
