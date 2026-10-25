package com.music.bitchord.data.innertube

/**
 * Metadata and direct playback URL for a video stream.
 */
data class VideoStreamInfo(
    val url: String,
    val resolution: String,
    val width: Int,
    val height: Int,
    val fps: Int = 30,
    val mimeType: String,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
) {
    /** Whether audio is pre-muxed into the video container (e.g. H.264 + AAC in MP4). */
    val isPreMuxed: Boolean
        get() = audioCodec != null || mimeType.contains("mp4a") || mimeType.contains("aac")
}

/**
 * Stream format selection heuristics for video renditions.
 */
object VideoStreamSelector {

    /**
     * Picks the highest quality progressive pre-muxed stream up to [maxHeight] (default 720p).
     * Prefers MP4 (H.264) for universal hardware decoding compatibility.
     */
    fun pickBestPreMuxed(candidates: List<VideoStreamInfo>, maxHeight: Int = 720): VideoStreamInfo? {
        if (candidates.isEmpty()) return null

        return candidates
            .filter { it.isPreMuxed && it.height <= maxHeight }
            .maxWithOrNull(
                compareBy<VideoStreamInfo> { it.height }
                    .thenBy { if (it.mimeType.contains("mp4")) 1 else 0 }
                    .thenBy { it.fps }
            ) ?: candidates.filter { it.height <= maxHeight }.maxByOrNull { it.height }
            ?: candidates.minByOrNull { it.height }
    }
}
