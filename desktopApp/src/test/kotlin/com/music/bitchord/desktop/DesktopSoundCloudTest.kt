package com.music.bitchord.desktop

import com.music.bitchord.data.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopSoundCloudTest {

    @Test
    fun descriptorMatchesKindProperties() {
        val config = DesktopSourceConfig("soundcloud", DesktopSourceKind.SOUNDCLOUD, enabled = true)
        val desc = DesktopSourceDescriptor(
            id = config.id,
            name = config.displayName,
            detail = config.kind.detail,
            kind = config.kind,
            configured = config.isComplete,
            enabled = config.enabled,
            supportsLossless = config.kind.supportsLossless,
        )

        assertEquals("SoundCloud", desc.name)
        assertFalse(desc.supportsLossless)
        assertTrue(desc.configured)
        assertTrue(desc.enabled)
    }

    @Test
    fun soundCloudTracksAreOwnedBySoundCloud() {
        val scSong = Song(
            videoId = "soundcloud:987654",
            title = "Test Remix",
            artist = "Producer",
            thumbnailUrl = null,
        )

        val ytSong = Song(
            videoId = "dQw4w9WgXcQ",
            title = "Never Gonna Give You Up",
            artist = "Rick Astley",
            thumbnailUrl = null,
        )

        assertTrue(scSong.videoId.startsWith("soundcloud:"))
        assertFalse(DesktopSourceRegistry.hasYouTubeOriginal(scSong))
        assertTrue(DesktopSourceRegistry.hasYouTubeOriginal(ytSong))
    }

    @Test
    fun directUrlSearchReturnsResolvedTrack() = kotlinx.coroutines.runBlocking {
        val url = "https://soundcloud.com/alanwalker/faded-slushii-remix-1"
        val results = DesktopSourceRegistry.search(url, com.music.bitchord.data.model.SearchFilter.ALL).getOrNull()
        assertNotNull(results, "Search should succeed for direct SoundCloud URL")
        assertTrue(results.isNotEmpty(), "Should return resolved results")
        val top = results.first()
        assertTrue(top is com.music.bitchord.data.model.SearchResult.TopTrack)
        assertTrue(top.song.videoId.startsWith("soundcloud:"))
        assertEquals("Faded (SLUSHII Remix)", top.song.title)
    }

    @Test
    fun relatedTracksRetrievedForAutoplay() = kotlinx.coroutines.runBlocking {
        val related = DesktopSoundCloud.getRelatedTracks("275391672", limit = 3).getOrNull()
        assertNotNull(related)
        assertTrue(related.isNotEmpty())
    }
}
