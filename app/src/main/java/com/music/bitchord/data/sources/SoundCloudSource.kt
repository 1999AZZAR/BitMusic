package com.music.bitchord.data.sources

import com.music.bitchord.data.TrackLog
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.soundcloud.SoundCloudService

private const val TAG = "BitChord"

class SoundCloudSource(
    override val config: SourceConfig,
) : MusicSource, SourceRegistry.ConfigBacked {

    override val configId: String get() = config.id
    override val kind: SourceKind get() = SourceKind.SOUNDCLOUD
    override val displayName: String get() = config.label.ifBlank { SourceKind.SOUNDCLOUD.label }

    /** Always Ok since the API endpoints don't need user credentials to search. */
    override suspend fun health(): SourceHealth = SourceHealth.Ok()

    override suspend fun search(
        query: String,
        limit: Int,
        waitForAll: Boolean,
        request: StreamRequest?,
    ): List<Song> {
        TrackLog.d(TAG, "▶ SoundCloud searchTracks() query=\"$query\" limit=$limit")
        val tracks = SoundCloudService.searchTracks(query, limit).getOrDefault(emptyList())
        TrackLog.d(TAG, "  ✓ SoundCloud returned ${tracks.size} tracks")
        return tracks.map { SoundCloudService.toSong(it, configId = config.id) }
    }

    override suspend fun stream(trackId: String, request: StreamRequest): SourceStream? {
        TrackLog.d(TAG, "▶ SoundCloud getStreamUrl() trackId=$trackId request=$request")
        val stream = SoundCloudService.resolveStream(trackId).getOrNull()
        if (stream == null || stream.url.isBlank()) {
            TrackLog.w(TAG, "  ✗ SoundCloud had no stream URL for $trackId")
            return null
        }
        TrackLog.d(TAG, "  ✓ SoundCloud ${stream.kbps}kbps ${stream.codec} ${stream.url.take(96)}")
        return SourceStream(
            url = stream.url,
            format = StreamFormat(codec = stream.codec, kbps = stream.kbps),
        )
    }
}
