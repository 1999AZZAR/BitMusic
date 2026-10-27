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
}
