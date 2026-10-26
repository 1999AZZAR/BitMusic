package com.music.bitchord.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AudioQualityBadgeFormatterTest {

    @Test
    fun testStandardQualityOpus() {
        val snapshot = NerdStats.Snapshot(
            mimeType = "audio/opus",
            bitrateKbps = 160,
            sampleRateHz = 48_000,
            channels = 2,
            bitDepth = 16,
        )
        val badge = assertNotNull(AudioQualityBadgeFormatter.formatBadge(snapshot))
        assertEquals(AudioQualityTier.STANDARD_QUALITY, badge.tier)
        assertEquals("Standard Quality", badge.title)
        assertEquals("Opus · 160 kbps · 48 kHz", badge.details)
        assertEquals(false, badge.isLossless)
    }

    @Test
    fun testStandardQualityOpusWithNullBitrate() {
        val snapshot = NerdStats.Snapshot(
            mimeType = "audio/opus",
            bitrateKbps = null,
            sampleRateHz = 48_000,
            channels = 2,
            bitDepth = null,
        )
        val badge = assertNotNull(AudioQualityBadgeFormatter.formatBadge(snapshot))
        assertEquals(AudioQualityTier.STANDARD_QUALITY, badge.tier)
        assertEquals("Standard Quality", badge.title)
        assertEquals("Opus · 48 kHz", badge.details)
        assertEquals(false, badge.isLossless)
    }

    @Test
    fun testDataSaverLowQuality() {
        val snapshot = NerdStats.Snapshot(
            mimeType = "audio/opus",
            bitrateKbps = 70,
            sampleRateHz = 48_000,
            channels = 2,
            bitDepth = null,
        )
        val badge = assertNotNull(AudioQualityBadgeFormatter.formatBadge(snapshot))
        assertEquals(AudioQualityTier.LOW_QUALITY, badge.tier)
        assertEquals("Data Saver", badge.title)
        assertEquals("Opus · 70 kbps · 48 kHz", badge.details)
        assertEquals(false, badge.isLossless)
    }

    @Test
    fun testHighQualityAac() {
        val snapshot = NerdStats.Snapshot(
            mimeType = "audio/mp4a-latm",
            bitrateKbps = 320,
            sampleRateHz = 44_100,
            channels = 2,
            bitDepth = 16,
        )
        val badge = assertNotNull(AudioQualityBadgeFormatter.formatBadge(snapshot))
        assertEquals(AudioQualityTier.HIGH_QUALITY, badge.tier)
        assertEquals("High Quality", badge.title)
        assertEquals("AAC · 320 kbps · 44.1 kHz", badge.details)
        assertEquals(false, badge.isLossless)
    }

    @Test
    fun testCdLosslessFlac() {
        val snapshot = NerdStats.Snapshot(
            mimeType = "audio/flac",
            bitrateKbps = 900,
            sampleRateHz = 44_100,
            channels = 2,
            bitDepth = 16,
        )
        val badge = assertNotNull(AudioQualityBadgeFormatter.formatBadge(snapshot))
        assertEquals(AudioQualityTier.LOSSLESS, badge.tier)
        assertEquals("Lossless", badge.title)
        assertEquals("FLAC · 16-bit · 44.1 kHz", badge.details)
        assertEquals(true, badge.isLossless)
        assertEquals(true, badge.isAnimated)
    }

    @Test
    fun testHiResLosslessFlac() {
        val snapshot = NerdStats.Snapshot(
            mimeType = "audio/flac",
            bitrateKbps = 4608,
            sampleRateHz = 96_000,
            channels = 2,
            bitDepth = 24,
        )
        val badge = assertNotNull(AudioQualityBadgeFormatter.formatBadge(snapshot))
        assertEquals(AudioQualityTier.HI_RES_LOSSLESS, badge.tier)
        assertEquals("Hi-Res Lossless", badge.title)
        assertEquals("FLAC · 24-bit · 96 kHz", badge.details)
        assertEquals(true, badge.isLossless)
        assertEquals(true, badge.isAnimated)
    }

    @Test
    fun testDolbyAtmos() {
        val snapshot = NerdStats.Snapshot(
            mimeType = "audio/eac3-joc",
            bitrateKbps = 768,
            sampleRateHz = 48_000,
            channels = 6,
            bitDepth = 16,
        )
        val badge = assertNotNull(AudioQualityBadgeFormatter.formatBadge(snapshot))
        assertEquals(AudioQualityTier.DOLBY_ATMOS, badge.tier)
        assertEquals("Dolby Atmos", badge.title)
        assertEquals("E-AC-3 JOC · 768 kbps · 48 kHz", badge.details)
        assertEquals(true, badge.isAnimated)
    }

    @Test
    fun testNullSnapshot() {
        assertNull(AudioQualityBadgeFormatter.formatBadge(null))
    }
}
