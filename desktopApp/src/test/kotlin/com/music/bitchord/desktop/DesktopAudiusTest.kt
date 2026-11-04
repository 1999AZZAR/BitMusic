package com.music.bitchord.desktop

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopAudiusTest {
    @Test
    fun recognizesAudiusTrackUrlsOnly() {
        assertTrue(DesktopAudius.isAudiusUrl("https://audius.co/artist/track-263514"))
        assertTrue(DesktopAudius.isAudiusUrl("www.audius.co/artist/track-263514"))
        assertFalse(DesktopAudius.isAudiusUrl("https://soundcloud.com/artist/track"))
        assertFalse(DesktopAudius.isAudiusUrl("https://not-audius.co/artist/track-263514"))
    }
}
