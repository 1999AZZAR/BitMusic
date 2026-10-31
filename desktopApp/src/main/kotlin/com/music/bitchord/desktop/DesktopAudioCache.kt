package com.music.bitchord.desktop

import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest

/**
 * Disk cache for whole audio streams, so a track that was played once can be replayed offline and
 * does not have to be fetched again.
 *
 * YouTube streams are excluded: their URLs are signed and expire, so caching them would only ever
 * store dead links. Everything else — SoundCloud, JioSaavn, add-ons, remote/SMB/WebDAV files and
 * locally imported tracks that are already on disk — is eligible, and the cache is keyed by the
 * stream URL so the same track is never downloaded twice.
 *
 * It reuses [DesktopMediaCache]'s LRU eviction shape but keeps its own directory and budget, so
 * audio (megabytes per track) and motion clips (a few megabytes per loop) do not evict each other.
 */
internal object DesktopAudioCache {

    /** Where the files live, created on first use. */
    val directory: Path by lazy {
        val state = System.getenv("XDG_CACHE_HOME")?.takeIf(String::isNotBlank)
            ?: "${System.getProperty("user.home")}/.cache"
        Path.of(state, "bitchord", "audio").also { runCatching { Files.createDirectories(it) } }
    }

    private val known = java.util.concurrent.ConcurrentHashMap<String, Path>()

    /** True when [url] points at a stream worth keeping (http(s), non-YouTube). */
    fun isCacheable(url: String): Boolean {
        val lower = url.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false
        if (lower.contains("googlevideo.com") || lower.contains("youtube.com")) return false
        return true
    }

    /** Extension to store [url] under, taken from its path or, failing that, a neutral one. */
    fun extensionFor(url: String): String {
        val path = url.substringBefore('?').substringBefore('#')
        val ext = path.substringAfterLast('.', "").lowercase()
        return if (ext.length in 2..5 && ext.all { it.isLetterOrDigit() }) ext else "audio"
    }

    /** The cached file for [url], or null when nothing has been kept for it. */
    fun existing(url: String, extension: String = extensionFor(url)): Path? {
        val target = pathFor(url, extension)
        return target.takeIf { Files.isReadable(it) && runCatching { Files.size(it) > 0 }.getOrDefault(false) }
    }

    /** Where [url] would be written, whether or not it has been. */
    fun pathFor(url: String, extension: String = extensionFor(url)): Path =
        known.getOrPut("$url|$extension") { directory.resolve("${digest(url)}.$extension") }

    /**
     * Returns the cached file for [url], downloading it first when it is missing. Returns null on
     * any failure, so callers can fall back to the network stream without special-casing errors.
     */
    fun fileFor(
        url: String,
        headers: Map<String, String> = emptyMap(),
        extension: String = extensionFor(url),
    ): Path? {
        existing(url, extension)?.let { return it }
        if (!isCacheable(url)) return null
        return runCatching {
            val target = pathFor(url, extension)
            val connection = (URI.create(url).toURL().openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                headers.forEach { (name, value) -> setRequestProperty(name, value) }
                setRequestProperty("User-Agent", headers["User-Agent"] ?: "BitMusic/1.0")
            }
            connection.inputStream.use { input ->
                Files.newOutputStream(target).use { output -> input.copyTo(output) }
            }
            check(Files.size(target) > 0) { "audio came back empty" }
            trim()
            target
        }.onFailure { runCatching { Files.deleteIfExists(pathFor(url, extension)) } }.getOrNull()
    }

    /** What the cache is currently holding, in bytes. */
    fun sizeBytes(): Long = files().sumOf { runCatching { Files.size(it) }.getOrDefault(0L) }

    /** Everything in it, thrown away. Returns how many bytes were freed. */
    fun clear(): Long {
        val freed = sizeBytes()
        files().forEach { runCatching { Files.deleteIfExists(it) } }
        known.clear()
        return freed
    }

    fun trim() {
        val limit = limitBytes()
        var total = sizeBytes()
        if (total <= limit) return
        files()
            .sortedBy { path ->
                runCatching {
                    Files.readAttributes(path, BasicFileAttributes::class.java).lastAccessTime().toMillis()
                }.getOrDefault(0L)
            }
            .forEach { path ->
                if (total <= limit) return
                val size = runCatching { Files.size(path) }.getOrDefault(0L)
                if (runCatching { Files.deleteIfExists(path) }.getOrDefault(false)) total -= size
            }
    }

    fun limitBytes(): Long =
        DesktopPersistence().int(KEY_CACHE_LIMIT_MB, DEFAULT_LIMIT_MB).toLong() * 1024 * 1024

    fun setLimitMb(megabytes: Int) {
        DesktopPersistence().saveInt(KEY_CACHE_LIMIT_MB, megabytes.coerceIn(MIN_LIMIT_MB, MAX_LIMIT_MB))
        trim()
    }

    fun limitMb(): Int = DesktopPersistence().int(KEY_CACHE_LIMIT_MB, DEFAULT_LIMIT_MB)

    private fun files(): List<Path> = runCatching {
        Files.list(directory).use { stream ->
            stream.filter(Files::isRegularFile).toList()
        }
    }.getOrDefault(emptyList())

    private fun digest(url: String): String = MessageDigest.getInstance("SHA-256")
        .digest(url.toByteArray())
        .joinToString("") { "%02x".format(it) }
        .take(32)

    internal const val KEY_CACHE_LIMIT_MB = "audio_cache_limit_mb"

    /** Larger than the clip cache: a single track is already tens of megabytes. */
    internal const val DEFAULT_LIMIT_MB = 2048
    internal const val MIN_LIMIT_MB = 512
    internal const val MAX_LIMIT_MB = 20 * 1024

    internal const val WARNING_MB = 4096
}