package com.music.bitchord.data.soundcloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SoundCloudServiceTest {

    @Test
    fun toSongMapsFieldsCorrectly() {
        val track = SoundCloudTrack(
            id = 123456L,
            title = "Strobe (Club Edit)",
            duration = 375000L,
            artworkUrl = "https://i1.sndcdn.com/artworks-sample-large.jpg",
            policy = "ALLOW",
            user = SoundCloudUser(id = 999L, username = "deadmau5", avatarUrl = null),
            media = SoundCloudMedia(
                transcodings = listOf(
                    SoundCloudTranscoding(
                        url = "https://api-v2.soundcloud.com/media/test/stream/hls",
                        duration = 375000L,
                        format = SoundCloudFormat(protocol = "hls", mimeType = "audio/mp4; codecs=\"mp4a.40.2\""),
                    ),
                ),
            ),
        )

        val desktopSong = SoundCloudService.toSong(track)
        assertEquals("soundcloud:123456", desktopSong.videoId)
        assertEquals("Strobe (Club Edit)", desktopSong.title)
        assertEquals("deadmau5", desktopSong.artist)
        assertEquals("6:15", desktopSong.durationText)
        assertEquals("https://i1.sndcdn.com/artworks-sample-t500x500.jpg", desktopSong.thumbnailUrl)

        val androidSong = SoundCloudService.toSong(track, configId = "custom-sc-id")
        assertEquals("src:custom-sc-id::123456", androidSong.videoId)
    }

    @Test
    fun defaultClientIdIsPresent() {
        val clientId = SoundCloudService.getClientId()
        assertNotNull(clientId)
        assertTrue(clientId.length >= 32, "Client ID should be at least 32 characters")
    }

    @Test
    fun isSoundCloudUrlRecognizesLinks() {
        assertTrue(SoundCloudService.isSoundCloudUrl("https://soundcloud.com/artist/track"))
        assertTrue(SoundCloudService.isSoundCloudUrl("http://soundcloud.com/artist/sets/playlist"))
        assertTrue(SoundCloudService.isSoundCloudUrl("https://m.soundcloud.com/artist/track"))
        assertTrue(SoundCloudService.isSoundCloudUrl("https://on.soundcloud.com/xyz123"))
        assertTrue(SoundCloudService.isSoundCloudUrl("soundcloud.com/artist/track"))

        assertEquals(false, SoundCloudService.isSoundCloudUrl("https://youtube.com/watch?v=123"))
        assertEquals(false, SoundCloudService.isSoundCloudUrl("https://open.spotify.com/track/123"))
        assertEquals(false, SoundCloudService.isSoundCloudUrl("faded remix"))
        assertEquals(false, SoundCloudService.isSoundCloudUrl(""))
    }

    @Test
    fun resolveLiveTrackUrl() = kotlinx.coroutines.runBlocking {
        val url = "https://soundcloud.com/alanwalker/faded-slushii-remix-1"
        val resolved = SoundCloudService.resolveUrl(url).getOrNull()
        assertNotNull(resolved, "Expected track to resolve successfully")
        assertTrue(resolved is SoundCloudResolved.Track)
        assertEquals("Faded (SLUSHII Remix)", resolved.track.title)
        assertTrue(resolved.song.videoId.startsWith("soundcloud:275391672"))
    }

    @Test
    fun getRelatedTracksReturnsSuggestions() = kotlinx.coroutines.runBlocking {
        val related = SoundCloudService.getRelatedTracks("275391672", limit = 5).getOrNull()
        assertNotNull(related, "Expected related tracks to be retrieved")
        assertTrue(related.isNotEmpty(), "Expected non-empty related tracks list")
    }

    @Test
    fun userProfileDataClassStructure() {
        val user = SoundCloudUserFull(id = 12345L, username = "testuser", trackCount = 10, likesCount = 25)
        val song1 = SoundCloudService.toSong(SoundCloudTrack(id = 111L, title = "Song 1"))
        val song2 = SoundCloudService.toSong(SoundCloudTrack(id = 222L, title = "Song 2"))
        val profile = SoundCloudService.SoundCloudUserProfile(
            user = user,
            tracks = listOf(song1),
            likes = listOf(song2),
            playlists = listOf("Test Playlist" to listOf(song1, song2)),
        )
        assertEquals("testuser", profile.user.username)
        assertEquals(1, profile.tracks.size)
        assertEquals(1, profile.likes.size)
        assertEquals(1, profile.playlists.size)
        assertEquals("Test Playlist", profile.playlists.first().first)
        assertEquals(2, profile.playlists.first().second.size)
    }
}
