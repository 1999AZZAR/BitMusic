package com.music.bitchord.desktop

import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.soundcloud.SoundCloudService

/** SoundCloud catalogue and CDN adapter for desktop source-aware playback. */
internal object DesktopSoundCloud {
    private const val TRACK_PREFIX = "soundcloud:"

    suspend fun search(query: String, limit: Int = 20): Result<List<SearchResult>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        SoundCloudService.searchTracks(query, limit).getOrDefault(emptyList())
            .map { SoundCloudService.toSong(it) }
            .map(SearchResult::Track)
    }

    suspend fun stream(trackKey: String): Result<DesktopStream?> = runCatching {
        val trackId = trackKey.removePrefix(TRACK_PREFIX).trim()
        if (trackId.isBlank()) return@runCatching null
        val scStream = SoundCloudService.resolveStream(trackId).getOrNull() ?: return@runCatching null

        DesktopStream(
            url = scStream.url,
            format = DesktopStreamFormat(codec = scStream.codec, kbps = scStream.kbps),
            sourceId = "soundcloud",
            transport = if (scStream.isHls) "hls" else null,
        )
    }

    /** Finds the same catalogue recording for a YouTube-origin song. */
    suspend fun match(song: Song): Song? = matches(song).firstOrNull()

    suspend fun resolveUrl(url: String): Result<com.music.bitchord.data.soundcloud.SoundCloudResolved> =
        SoundCloudService.resolveUrl(url)

    suspend fun getRelatedTracks(trackId: String, limit: Int = 20): Result<List<Song>> = runCatching {
        SoundCloudService.getRelatedTracks(trackId, limit).getOrDefault(emptyList())
            .map { SoundCloudService.toSong(it) }
    }

    /** Every copy this catalogue holds of [song], most confident first. */
    suspend fun matches(song: Song): List<Song> {
        if (song.title.isBlank() || song.isVideo) return emptyList()
        for (query in DesktopTrackMatcher.queries(song)) {
            val candidates = SoundCloudService.searchTracks(query, 20).getOrDefault(emptyList())
                .map { SoundCloudService.toSong(it) }
            DesktopTrackMatcher.ranked(candidates, song).ifEmpty { null }?.let { return it }
        }
        return emptyList()
    }
}
