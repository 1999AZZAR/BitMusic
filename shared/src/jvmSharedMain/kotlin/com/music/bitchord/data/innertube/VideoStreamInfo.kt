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
    val isVideoOnly: Boolean = false,
) {
    /** Whether audio is pre-muxed into the video container (e.g. H.264 + AAC in MP4). */
    val isPreMuxed: Boolean
        get() = !isVideoOnly && (audioCodec != null || mimeType.contains("mp4a") || mimeType.contains("aac"))

    val resolutionTier: String
        get() = if (height > 0) "${height}p" else resolution
}

/**
 * Stream format selection heuristics for video renditions.
 */
object VideoStreamSelector {

    /**
     * Extracts unique resolution tiers (e.g. ["1080p", "720p", "480p", "360p"])
     * sorted from highest to lowest.
     */
    fun availableResolutions(candidates: List<VideoStreamInfo>): List<String> {
        return candidates
            .map { it.resolutionTier }
            .distinct()
            .sortedByDescending { resolutionHeight(it) }
    }

    /**
     * Resolves the best candidate matching [targetResolution] (e.g. "1080p", "720p", "480p", "360p", or "auto").
     * When "auto", null, or empty, picks the highest quality stream up to [maxHeight] (default 1080).
     * Prefers MP4 (H.264) container for optimal performance and universal decoding.
     */
    fun selectStream(
        candidates: List<VideoStreamInfo>,
        targetResolution: String? = null,
        maxHeight: Int = 1080,
    ): VideoStreamInfo? {
        if (candidates.isEmpty()) return null

        val isAuto = targetResolution.isNullOrBlank() || targetResolution.equals("auto", ignoreCase = true)
        val targetHeight = if (!isAuto) targetResolution.let { resolutionHeight(it) }.takeIf { it > 0 } else null

        val pool = if (targetHeight != null) {
            candidates.filter { it.height == targetHeight }
                .ifEmpty { candidates.filter { it.height <= targetHeight } }
                .ifEmpty { candidates }
        } else {
            candidates.filter { it.height <= maxHeight }
                .ifEmpty { candidates }
        }

        return pool.maxWithOrNull(
            compareBy<VideoStreamInfo> { it.height }
                .thenBy { if (it.mimeType.contains("mp4") || it.videoCodec?.contains("avc") == true) 2 else 1 }
                .thenBy { it.fps }
        ) ?: pool.firstOrNull()
    }

    /**
     * Picks the highest quality progressive pre-muxed stream up to [maxHeight] (default 720p).
     * Preserved for backward compatibility.
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

    fun resolutionHeight(res: String): Int {
        val digits = res.filter { it.isDigit() }
        return digits.toIntOrNull() ?: 0
    }
}
