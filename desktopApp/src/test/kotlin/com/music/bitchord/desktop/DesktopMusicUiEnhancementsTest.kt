package com.music.bitchord.desktop

import com.music.bitchord.data.model.Song
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopMusicUiEnhancementsTest {

    @Test
    fun testSidePanelEnumIncludesRelated() {
        val panels = DesktopSidePanel.values()
        assertEquals(3, panels.size)
        assertTrue(panels.contains(DesktopSidePanel.LYRICS))
        assertTrue(panels.contains(DesktopSidePanel.QUEUE))
        assertTrue(panels.contains(DesktopSidePanel.RELATED))
        assertEquals(DesktopSidePanel.RELATED, DesktopSidePanel.valueOf("RELATED"))
    }

    @Test
    fun testQueueWithoutAutoplay() {
        val s1 = Song(videoId = "v1", title = "Track 1", artist = "Artist 1", thumbnailUrl = null)
        val s2 = Song(videoId = "v2", title = "Track 2", artist = "Artist 2", thumbnailUrl = null)
        val sAuto = Song(videoId = "v3", title = "Track 3", artist = "Artist 3", thumbnailUrl = null, fromAutoplay = true)

        val queue = DesktopQueue(songs = listOf(s1, s2, sAuto), index = 0)
        assertEquals(3, queue.songs.size)

        val trimmed = queue.withoutAutoplay()
        assertEquals(2, trimmed.songs.size)
        assertFalse(trimmed.songs.any { it.fromAutoplay })
    }

    @Test
    fun testMoodPillKeywords() {
        val canonicalMoodKeywords = listOf("Relax", "Energize", "Workout", "Focus", "Commute", "Party")
        assertEquals(6, canonicalMoodKeywords.size)
        assertTrue(canonicalMoodKeywords.contains("Relax"))
        assertTrue(canonicalMoodKeywords.contains("Workout"))
        assertTrue(canonicalMoodKeywords.contains("Focus"))
    }
}
