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
}
