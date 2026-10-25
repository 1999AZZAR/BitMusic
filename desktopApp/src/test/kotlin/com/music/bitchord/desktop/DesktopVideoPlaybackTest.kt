package com.music.bitchord.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopVideoPlaybackTest {

    @Test
    fun `stream aspect ratio computes correctly from width and height`() {
        val stream = DesktopVideoStream(
            url = "https://example.com/video.mp4",
            resolution = "720p",
            width = 1280,
            height = 720,
        )
        val expected = 1280f / 720f
        assertEquals(expected, stream.aspectRatio, 0.001f)
    }

    @Test
    fun `video clock renders frame when presentation timestamp matches audio clock`() {
        val clock = DesktopVideoSyncClock()
        val action = clock.evaluate(framePtsMs = 5_000, audioPositionMs = 5_010)
        assertEquals(DesktopVideoSyncAction.RENDER, action)
    }

    @Test
    fun `video clock drops late frame when video is behind audio`() {
        val clock = DesktopVideoSyncClock()
        // Frame is 200ms behind audio clock (> 100ms threshold)
        val action = clock.evaluate(framePtsMs = 5_000, audioPositionMs = 5_200)
        assertEquals(DesktopVideoSyncAction.DROP, action)
    }

    @Test
    fun `video clock waits when video frame is ahead of audio clock`() {
        val clock = DesktopVideoSyncClock()
        // Frame is 80ms ahead of audio clock
        val action = clock.evaluate(framePtsMs = 5_080, audioPositionMs = 5_000)
        assertTrue(action is DesktopVideoSyncAction.WAIT)
        assertEquals(80L, action.delayMs)
    }

    @Test
    fun `clock detects seek jump forward or backward`() {
        val clock = DesktopVideoSyncClock()
        // Normal advancement (e.g., 30ms elapsed) -> no seek
        assertFalse(clock.isDiscontinuous(lastAudioMs = 1_000, currentAudioMs = 1_030))

        // Jump forward by 5 seconds -> discontinuous (seek)
        assertTrue(clock.isDiscontinuous(lastAudioMs = 1_000, currentAudioMs = 6_000))

        // Jump backward -> discontinuous (seek)
        assertTrue(clock.isDiscontinuous(lastAudioMs = 5_000, currentAudioMs = 2_000))
    }

    @Test
    fun `playback state preserves video mode and tracks loading and error states`() {
        val initial = DesktopPlaybackState(
            isVideoMode = true,
            videoLoading = true,
        )
        assertTrue(initial.isVideoMode)
        assertTrue(initial.videoLoading)
        assertEquals(null, initial.videoError)

        val resolved = initial.copy(
            videoStream = DesktopVideoStream(
                url = "https://example.com/v.mp4",
                resolution = "1080p",
                width = 1920,
                height = 1080,
                availableResolutions = listOf("1080p", "720p", "480p"),
            ),
            preferredVideoResolution = "1080p",
            videoLoading = false,
        )
        assertTrue(resolved.isVideoMode)
        assertFalse(resolved.videoLoading)
        assertEquals("1080p", resolved.videoStream?.resolution)
        assertEquals(listOf("1080p", "720p", "480p"), resolved.videoStream?.availableResolutions)
        assertEquals("1080p", resolved.preferredVideoResolution)

        val failed = initial.copy(
            videoStream = null,
            videoLoading = false,
            videoError = "Video unavailable",
        )
        assertTrue(failed.isVideoMode)
        assertFalse(failed.videoLoading)
        assertEquals("Video unavailable", failed.videoError)
    }
}
