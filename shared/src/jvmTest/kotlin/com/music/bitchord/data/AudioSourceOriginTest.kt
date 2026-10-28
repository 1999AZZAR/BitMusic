package com.music.bitchord.data

import com.music.bitchord.data.model.AudioSourceOrigin
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.audioSourceOrigin
import kotlin.test.Test
import kotlin.test.assertEquals

class AudioSourceOriginTest {

    private fun testSong(videoId: String): Song = Song(
        videoId = videoId,
        title = "Test Song",
        artist = "Test Artist",
        thumbnailUrl = null,
    )

    @Test
    fun testSoundCloudOrigin() {
        val song = testSong("soundcloud:987654321")
        assertEquals(AudioSourceOrigin.SOUNDCLOUD, song.audioSourceOrigin)
        assertEquals("SoundCloud", song.audioSourceOrigin.label)
        assertEquals("SC", song.audioSourceOrigin.shortLabel)
    }

    @Test
    fun testLocalOrigins() {
        assertEquals(AudioSourceOrigin.LOCAL, testSong("local:downloads:123").audioSourceOrigin)
        assertEquals(AudioSourceOrigin.LOCAL, testSong("file:///storage/music/track.flac").audioSourceOrigin)
        assertEquals(AudioSourceOrigin.LOCAL, testSong("content://media/external/audio/media/10").audioSourceOrigin)
    }

    @Test
    fun testJioSaavnOrigin() {
        val song = testSong("jiosaavn:track_id_99")
        assertEquals(AudioSourceOrigin.JIOSAAVN, song.audioSourceOrigin)
        assertEquals("JioSaavn", song.audioSourceOrigin.label)
        assertEquals("SAAVN", song.audioSourceOrigin.shortLabel)
    }

    @Test
    fun testAddonOrigin() {
        val song = testSong("src:user-addon::audio_uuid")
        assertEquals(AudioSourceOrigin.ADDON, song.audioSourceOrigin)
        assertEquals("Addon", song.audioSourceOrigin.label)
        assertEquals("ADDON", song.audioSourceOrigin.shortLabel)
    }

    @Test
    fun testYouTubeOrigin() {
        val song = testSong("dQw4w9WgXcQ")
        assertEquals(AudioSourceOrigin.YOUTUBE, song.audioSourceOrigin)
        assertEquals("YouTube", song.audioSourceOrigin.label)
        assertEquals("YT", song.audioSourceOrigin.shortLabel)
    }
}
