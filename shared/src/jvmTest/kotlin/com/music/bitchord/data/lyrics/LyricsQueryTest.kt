package com.music.bitchord.data.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals

class LyricsQueryTest {

    @Test
    fun testProducerBracketsStripping() {
        assertEquals("Mask Off", "Mask Off (Prod. by Metro Boomin)".forLyricsSearch())
        assertEquals("Magnolia", "Magnolia [prod. Pierre Bourne]".forLyricsSearch())
        assertEquals("Stronger", "Stronger (Produced by Kanye)".forLyricsSearch())
        assertEquals("Lucid Dreams", "Lucid Dreams prod. by Nick Mira".forLyricsSearch())
    }

    @Test
    fun testPromotionalLabelsStripping() {
        assertEquals("Faded", "Faded [Free DL]".forLyricsSearch())
        assertEquals("Alone", "Alone (Free Download)".forLyricsSearch())
        assertEquals("Type Beat", "Type Beat [FREE]".forLyricsSearch())
        assertEquals("Midnight", "Midnight [OUT NOW!]".forLyricsSearch())
        assertEquals("After Hours", "After Hours [Stream/Buy]".forLyricsSearch())
        assertEquals("Spectre", "Spectre [NCS Release]".forLyricsSearch())
        assertEquals("New Track", "New Track [Exclusive]".forLyricsSearch())
        assertEquals("Premiere Song", "Premiere Song [Premiere]".forLyricsSearch())
    }

    @Test
    fun testQualityAndPackagingStripping() {
        assertEquals("Blinding Lights", "Blinding Lights [Official Audio]".forLyricsSearch())
        assertEquals("Blinding Lights", "Blinding Lights (Official Lyric Video)".forLyricsSearch())
        assertEquals("Blinding Lights", "Blinding Lights (Visualizer)".forLyricsSearch())
        assertEquals("Blinding Lights", "Blinding Lights [HQ]".forLyricsSearch())
        assertEquals("Blinding Lights", "Blinding Lights [4K]".forLyricsSearch())
        assertEquals("Blinding Lights", "Blinding Lights [1080p]".forLyricsSearch())
        assertEquals("Blinding Lights", "Blinding Lights [Clean]".forLyricsSearch())
        assertEquals("Blinding Lights", "Blinding Lights [Explicit]".forLyricsSearch())
        assertEquals("Blinding Lights", "Blinding Lights (Snippet)".forLyricsSearch())
        assertEquals("Summer Song", "Summer Song (Remix 2024)".forLyricsSearch())
        assertEquals("Summer Song", "Summer Song [2024]".forLyricsSearch())
        assertEquals("Levitating", "Levitating | Official Music Video".forLyricsSearch())
    }

    @Test
    fun testCombinedCreditsAndPromos() {
        assertEquals("Rockstar", "Rockstar (feat. 21 Savage) [Free DL] (Prod. by Tank God)".forLyricsSearch())
        assertEquals("Levitating", "Levitating (feat. DaBaby) | Official Audio".forLyricsSearch())
        assertEquals("golden hour", "golden hour".forLyricsSearch())
    }

    @Test
    fun testLeavesDistinctRecordingsIntact() {
        // These identify distinct audio recordings, not metadata fluff
        assertEquals("Dracula (JENNIE Remix)", "Dracula (JENNIE Remix)".forLyricsSearch())
        assertEquals("Everlong (Acoustic)", "Everlong (Acoustic)".forLyricsSearch())
        assertEquals("Song 2 (Live)", "Song 2 (Live)".forLyricsSearch())
        assertEquals("Bohemian Rhapsody (Remastered 2011)", "Bohemian Rhapsody (Remastered 2011)".forLyricsSearch())
        assertEquals("Nights (Sped Up)", "Nights (Sped Up)".forLyricsSearch())
    }

    @Test
    fun testFallbackWhenTitleIsOnlyPackaging() {
        assertEquals("(Official Video)", "(Official Video)".forLyricsSearch())
        assertEquals("[FREE]", "[FREE]".forLyricsSearch())
    }

    @Test
    fun testArtistChannelCleaning() {
        assertEquals("Tame Impala", "Tame Impala - Topic".artistForLyricsSearch())
        assertEquals("TheWeeknd", "TheWeekndVEVO".artistForLyricsSearch())
        assertEquals("Marshmello", "Marshmello Official".artistForLyricsSearch())
        assertEquals("Alan Walker", "Alan Walker Official Channel".artistForLyricsSearch())
        assertEquals("Dua Lipa", "Dua Lipa (Official)".artistForLyricsSearch())
        assertEquals("Coldplay", "Coldplay".artistForLyricsSearch())
    }
}
