package com.music.bitchord.data.soundcloud

import com.music.bitchord.data.Http
import com.music.bitchord.data.model.Song
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SoundCloudUser(
    val id: Long = 0,
    val username: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class SoundCloudFormat(
    val protocol: String = "",
    @SerialName("mime_type") val mimeType: String = "",
)

@Serializable
data class SoundCloudTranscoding(
    val url: String = "",
    val duration: Long = 0,
    val format: SoundCloudFormat = SoundCloudFormat(),
)

@Serializable
data class SoundCloudMedia(
    val transcodings: List<SoundCloudTranscoding> = emptyList(),
)

@Serializable
data class SoundCloudTrack(
    val id: Long = 0,
    val title: String = "",
    val duration: Long = 0,
    @SerialName("artwork_url") val artworkUrl: String? = null,
    val policy: String? = null,
    val user: SoundCloudUser = SoundCloudUser(),
    val media: SoundCloudMedia = SoundCloudMedia(),
)

@Serializable
private data class SoundCloudSearchResponse(
    val collection: List<SoundCloudTrack> = emptyList(),
    @SerialName("total_results") val totalResults: Int? = null,
)

@Serializable
private data class SoundCloudStreamResponse(
    val url: String = "",
)

data class SoundCloudStream(
    val url: String,
    val codec: String,
    val kbps: Int,
    val isHls: Boolean,
)

/**
 * Direct SoundCloud API v2 adapter.
 *
 * Supports zero-authentication dynamic client ID extraction, catalogue searching,
 * and HLS/progressive stream URL resolution.
 */
object SoundCloudService {
    private const val BASE_URL = "https://api-v2.soundcloud.com"
    private const val WEB_URL = "https://soundcloud.com"
    private const val FALLBACK_CLIENT_ID = "3GKD969NJWD1eB6gvQJN0GnueYbcWszv"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client = HttpClient(OkHttp) {
        engine { preconfigured = Http.client }
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
    }

    private val clientIdMutex = Mutex()

    @Volatile
    private var currentClientId: String = FALLBACK_CLIENT_ID

    fun getClientId(): String = currentClientId

    suspend fun refreshClientId(): String = clientIdMutex.withLock {
        runCatching {
            val html = client.get(WEB_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
            }.bodyAsText()

            val scriptUrls = Regex("""<script[^>]+src=["'](https://a-v2\.sndcdn\.com/assets/[^"']+\.js)["']""")
                .findAll(html)
                .map { it.groupValues[1] }
                .toList()

            for (scriptUrl in scriptUrls.reversed()) {
                val js = client.get(scriptUrl) {
                    header(HttpHeaders.UserAgent, USER_AGENT)
                }.bodyAsText()

                val m = Regex("""client_id[:=]["']([a-zA-Z0-9]{32})["']""").find(js)
                if (m != null) {
                    val extracted = m.groupValues[1]
                    currentClientId = extracted
                    return@withLock extracted
                }
            }
        }
        currentClientId
    }

    suspend fun searchTracks(query: String, limit: Int = 20): Result<List<SoundCloudTrack>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        var clientId = currentClientId
        var response = executeSearch(query, limit, clientId)

        if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
            clientId = refreshClientId()
            response = executeSearch(query, limit, clientId)
        }

        check(response.status == HttpStatusCode.OK) {
            "SoundCloud search returned HTTP ${response.status.value}"
        }

        val parsed = json.decodeFromString<SoundCloudSearchResponse>(response.bodyAsText())
        parsed.collection.filter { it.policy != "BLOCK" }
    }

    private suspend fun executeSearch(query: String, limit: Int, clientId: String) =
        client.get("$BASE_URL/search/tracks") {
            parameter("q", query.trim())
            parameter("client_id", clientId)
            parameter("limit", limit)
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
        }

    private suspend fun getTrack(trackId: String, clientId: String): SoundCloudTrack? = runCatching {
        val response = client.get("$BASE_URL/tracks/$trackId") {
            parameter("client_id", clientId)
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
        }
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<SoundCloudTrack>(response.bodyAsText())
        } else {
            null
        }
    }.getOrNull()

    private suspend fun fetchStreamUrl(endpointUrl: String): SoundCloudStreamResponse? = runCatching {
        val response = client.get(endpointUrl) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
        }
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<SoundCloudStreamResponse>(response.bodyAsText())
        } else {
            null
        }
    }.getOrNull()

    suspend fun resolveStream(trackId: String): Result<SoundCloudStream?> = runCatching {
        val cleanId = trackId.removePrefix("soundcloud:").substringAfterLast("::").trim()
        if (cleanId.isBlank()) return@runCatching null

        var clientId = currentClientId
        var track = getTrack(cleanId, clientId)
        if (track == null) {
            clientId = refreshClientId()
            track = getTrack(cleanId, clientId)
        }
        val candidateTrack = track ?: return@runCatching null

        val transcodings = candidateTrack.media.transcodings
        if (transcodings.isEmpty()) return@runCatching null

        val fullStreams = transcodings.filter { !it.url.contains("/preview/") }
        val pool = if (fullStreams.isNotEmpty()) fullStreams else transcodings

        // Preferred order:
        // 1. HLS with AAC (mimeType contains mp4)
        // 2. Progressive MP3
        // 3. HLS with MP3 (mimeType contains mpegurl)
        val selected = pool.firstOrNull {
            it.format.protocol.equals("hls", ignoreCase = true) &&
                it.format.mimeType.contains("mp4", ignoreCase = true)
        }
            ?: pool.firstOrNull { it.format.protocol.equals("progressive", ignoreCase = true) }
            ?: pool.firstOrNull { it.format.protocol.equals("hls", ignoreCase = true) }
            ?: pool.firstOrNull()
            ?: return@runCatching null

        val streamAuthUrl = if (selected.url.contains("?")) {
            "${selected.url}&client_id=$clientId"
        } else {
            "${selected.url}?client_id=$clientId"
        }

        var streamResponse = fetchStreamUrl(streamAuthUrl)
        if (streamResponse == null) {
            val newClientId = refreshClientId()
            val retryUrl = if (selected.url.contains("?")) {
                "${selected.url}&client_id=$newClientId"
            } else {
                "${selected.url}?client_id=$newClientId"
            }
            streamResponse = fetchStreamUrl(retryUrl)
        }

        val streamUrl = streamResponse?.url?.takeIf(String::isNotBlank) ?: return@runCatching null
        val isHls = selected.format.protocol.equals("hls", ignoreCase = true)
        val isAac = selected.format.mimeType.contains("mp4", ignoreCase = true) ||
            selected.format.mimeType.contains("aac", ignoreCase = true)
        val codec = if (isAac) "aac" else "mp3"
        val kbps = if (isAac) 96 else 128

        SoundCloudStream(
            url = streamUrl,
            codec = codec,
            kbps = kbps,
            isHls = isHls,
        )
    }

    fun toSong(track: SoundCloudTrack, configId: String? = null): Song {
        val totalSec = (track.duration / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        val durationText = String.format("%d:%02d", m, s)
        val artwork = (track.artworkUrl ?: track.user.avatarUrl)
            ?.replace("-large.", "-t500x500.")
            ?.replace(Regex("^http://"), "https://")

        val videoId = if (configId != null) {
            "src:$configId::${track.id}"
        } else {
            "soundcloud:${track.id}"
        }

        return Song(
            videoId = videoId,
            title = track.title,
            artist = track.user.username.ifBlank { "Unknown Artist" },
            albumName = null,
            thumbnailUrl = artwork,
            durationText = durationText,
            sourceQuality = "STANDARD",
            isExplicit = false,
        )
    }
}
