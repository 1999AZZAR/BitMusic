package com.music.bitchord.data.innertube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VideoStreamResolverTest {

    @Test
    fun testVideoStreamInfoProperties() {
        val stream = VideoStreamInfo(
            url = "https://googlevideo.com/videoplayback?itag=22",
            resolution = "720p",
            width = 1280,
            height = 720,
            fps = 30,
            mimeType = "video/mp4; codecs=\"avc1.64001F, mp4a.40.2\"",
            videoCodec = "avc1.64001F",
            audioCodec = "mp4a.40.2",
        )

        assertEquals("720p", stream.resolution)
        assertEquals(1280, stream.width)
        assertEquals(720, stream.height)
        assertEquals(30, stream.fps)
        assertEquals("video/mp4; codecs=\"avc1.64001F, mp4a.40.2\"", stream.mimeType)
        assertEquals(true, stream.isPreMuxed)
    }

    @Test
    fun testVideoStreamSelectionBestPreMuxed() {
        val candidates = listOf(
            VideoStreamInfo("http://video/360p.mp4", "360p", 640, 360, 30, "video/mp4", "avc1", "mp4a"),
            VideoStreamInfo("http://video/720p.mp4", "720p", 1280, 720, 30, "video/mp4", "avc1", "mp4a"),
            VideoStreamInfo("http://video/480p.mp4", "480p", 854, 480, 30, "video/mp4", "avc1", "mp4a"),
        )

        val picked = VideoStreamSelector.pickBestPreMuxed(candidates)
        assertNotNull(picked)
        assertEquals("720p", picked.resolution)
        assertEquals(1280, picked.width)
    }

    @Test
    fun testVideoStreamSelectionEmptyCandidates() {
        val picked = VideoStreamSelector.pickBestPreMuxed(emptyList())
        assertNull(picked)
    }

    @Test
    fun testAvailableResolutionsDeduplicatedAndSorted() {
        val candidates = listOf(
            VideoStreamInfo("http://video/360p.mp4", "360p", 640, 360, 30, "video/mp4", "avc1"),
            VideoStreamInfo("http://video/1080p-adaptive.mp4", "1080p", 1920, 1080, 60, "video/mp4", "avc1", isVideoOnly = true),
            VideoStreamInfo("http://video/720p.mp4", "720p", 1280, 720, 30, "video/mp4", "avc1"),
            VideoStreamInfo("http://video/720p-adaptive.mp4", "720p", 1280, 720, 60, "video/mp4", "avc1", isVideoOnly = true),
            VideoStreamInfo("http://video/480p.mp4", "480p", 854, 480, 30, "video/mp4", "avc1"),
        )

        val resolutions = VideoStreamSelector.availableResolutions(candidates)
        assertEquals(listOf("1080p", "720p", "480p", "360p"), resolutions)
    }

    @Test
    fun testSelectStreamByResolutionTarget() {
        val candidates = listOf(
            VideoStreamInfo("http://video/360p.mp4", "360p", 640, 360, 30, "video/mp4", "avc1"),
            VideoStreamInfo("http://video/480p.mp4", "480p", 854, 480, 30, "video/mp4", "avc1"),
            VideoStreamInfo("http://video/720p.mp4", "720p", 1280, 720, 30, "video/mp4", "avc1"),
            VideoStreamInfo("http://video/1080p.mp4", "1080p", 1920, 1080, 60, "video/mp4", "avc1", isVideoOnly = true),
        )

        val res1080 = VideoStreamSelector.selectStream(candidates, targetResolution = "1080p")
        assertNotNull(res1080)
        assertEquals("1080p", res1080.resolution)
        assertEquals(1080, res1080.height)

        val res480 = VideoStreamSelector.selectStream(candidates, targetResolution = "480p")
        assertNotNull(res480)
        assertEquals("480p", res480.resolution)
        assertEquals(480, res480.height)

        val auto = VideoStreamSelector.selectStream(candidates, targetResolution = "auto")
        assertNotNull(auto)
        assertEquals("1080p", auto.resolution)
    }
}
