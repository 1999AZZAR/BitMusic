package com.music.bitchord.desktop

import com.music.bitchord.data.Http
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Serializable
private data class AudiusResponse<T>(val data: T)

@Serializable
private data class AudiusTrack(
    @SerialName("track_id") val trackId: Long = 0,
    val title: String = "",
    val duration: Int = 0,
    @SerialName("is_streamable") val isStreamable: Boolean = true,
    @SerialName("is_available") val isAvailable: Boolean = true,
    val artwork: AudiusArtwork? = null,
    val user: AudiusUser = AudiusUser(),
)

@Serializable
private data class AudiusArtwork(
    @SerialName("150x150") val small: String? = null,
    @SerialName("480x480") val medium: String? = null,
    @SerialName("1000x1000") val large: String? = null,
)

@Serializable
private data class AudiusUser(
    val name: String = "",
    val handle: String = "",
)

/** Audius catalogue and content-node adapter for source-aware desktop playback. */
internal object DesktopAudius {
    private const val BASE_URL = "https://api.audius.co/v1"
    private const val APP_NAME = "BitMusic"
    private const val TRACK_PREFIX = "audius:"
    private val userAgent: String
        get() = "BitMusic/v${System.getProperty("bitchord.version") ?: "1.3.17"}"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    suspend fun search(query: String, limit: Int = 20): Result<List<SearchResult>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        val payload = get("/tracks/search", mapOf("query" to query.trim(), "limit" to limit))
        json.decodeFromString<AudiusResponse<List<AudiusTrack>>>(payload)
            .data
            .filter(::playable)
            .take(limit)
            .map(::toSong)
            .map(SearchResult::Track)
    }

    suspend fun stream(trackKey: String): Result<DesktopStream?> = runCatching {
        val trackId = trackKey.removePrefix(TRACK_PREFIX).trim().toLongOrNull()
            ?: return@runCatching null
        val track = getTrack(trackId) ?: return@runCatching null
        if (!playable(track)) return@runCatching null
        DesktopStream(
            url = "$BASE_URL/tracks/$trackId/stream?app_name=$APP_NAME",
            format = DesktopStreamFormat(codec = "mp3", kbps = 320),
            sourceId = "audius",
            durationSec = track.duration.takeIf { it > 0 },
        )
    }

    suspend fun resolveUrl(rawUrl: String): Result<Song?> = runCatching {
        val trackId = extractTrackId(rawUrl) ?: return@runCatching null
        getTrack(trackId)?.takeIf(::playable)?.let(::toSong)
    }

    suspend fun matches(song: Song): List<Song> {
        if (song.title.isBlank() || song.isVideo) return emptyList()
        for (query in DesktopTrackMatcher.queries(song)) {
            val candidates = search(query, 20).getOrDefault(emptyList())
                .mapNotNull { (it as? SearchResult.Track)?.song }
            DesktopTrackMatcher.ranked(candidates, song).ifEmpty { null }?.let { return it }
        }
        return emptyList()
    }

    fun isAudiusUrl(input: String): Boolean {
        val candidate = input.trim().let {
            if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
        }
        val host = runCatching { URI(candidate).host?.lowercase() }.getOrNull() ?: return false
        return host == "audius.co" || host == "www.audius.co"
    }

    private fun toSong(track: AudiusTrack): Song = Song(
        videoId = TRACK_PREFIX + track.trackId,
        title = track.title.trim().ifBlank { "Untitled Audius track" },
        artist = track.user.name.ifBlank { track.user.handle }.ifBlank { "Unknown Artist" },
        thumbnailUrl = (track.artwork?.large ?: track.artwork?.medium ?: track.artwork?.small)
            ?.replace(Regex("^http://"), "https://"),
        durationText = track.duration.takeIf { it > 0 }?.let { seconds ->
            "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
        },
        sourceQuality = "HIGH",
        isExplicit = false,
    )

    private suspend fun getTrack(trackId: Long): AudiusTrack? = runCatching {
        val payload = get("/tracks/$trackId")
        json.decodeFromString<AudiusResponse<AudiusTrack>>(payload).data
    }.getOrNull()

    private fun playable(track: AudiusTrack): Boolean =
        track.trackId > 0 && track.title.isNotBlank() && track.isAvailable && track.isStreamable

    private fun get(path: String, parameters: Map<String, Any> = emptyMap()): String {
        val query = buildList {
            add("app_name=$APP_NAME")
            parameters.forEach { (key, value) ->
                add("${encode(key)}=${encode(value.toString())}")
            }
        }.joinToString("&")
        val request = Request.Builder()
            .url("$BASE_URL$path?$query")
            .header("Accept", "application/json")
            .header("User-Agent", userAgent)
            .get()
            .build()
        return Http.client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Audius returned HTTP ${response.code}" }
            response.body.string()
        }
    }

    private fun extractTrackId(rawUrl: String): Long? {
        if (!isAudiusUrl(rawUrl)) return null
        val path = runCatching {
            val candidate = rawUrl.trim().let {
                if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
            }
            URI(candidate).path.orEmpty()
        }.getOrNull() ?: return null
        return Regex("(?:-|/)(\\d+)(?:/)?$").find(path)?.groupValues?.getOrNull(1)?.toLongOrNull()
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
}
