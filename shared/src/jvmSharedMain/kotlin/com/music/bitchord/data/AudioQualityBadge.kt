package com.music.bitchord.data

import java.util.Locale

/**
 * High-level tier classification for active audio streams.
 */
enum class AudioQualityTier(val label: String) {
    HI_RES_LOSSLESS("Hi-Res Lossless"),
    LOSSLESS("Lossless"),
    DOLBY_ATMOS("Dolby Atmos"),
    HIGH_QUALITY("High Quality"),
    STANDARD_QUALITY("Standard Quality"),
    LOW_QUALITY("Data Saver"),
    UNKNOWN("Unknown")
}

/**
 * Presentation-ready audio quality badge metadata for Now Playing screens.
 */
data class AudioQualityBadgeInfo(
    val tier: AudioQualityTier,
    val title: String,
    val details: String?,
    val isLossless: Boolean,
    val isAnimated: Boolean,
)

/**
 * Resolves and formats stream metadata from [NerdStats.Snapshot] into user-facing badges.
 */
object AudioQualityBadgeFormatter {

    fun resolveTier(stats: NerdStats.Snapshot?): AudioQualityTier {
        if (stats == null) return AudioQualityTier.UNKNOWN
        return when {
            stats.isDolbyAtmos -> AudioQualityTier.DOLBY_ATMOS
            stats.isHiRes -> AudioQualityTier.HI_RES_LOSSLESS
            stats.isLossless -> AudioQualityTier.LOSSLESS
            stats.isHiQuality -> AudioQualityTier.HIGH_QUALITY
            stats.isLowQuality -> AudioQualityTier.LOW_QUALITY
            else -> AudioQualityTier.STANDARD_QUALITY
        }
    }

    fun formatDetails(stats: NerdStats.Snapshot?): String? {
        if (stats == null) return null
        val parts = mutableListOf<String>()

        val codec = NerdStats.codecLabel(stats.mimeType)
        if (!codec.isNullOrBlank()) {
            parts.add(codec)
        }

        val kbps = stats.bitrateKbps
        if (kbps != null && kbps > 0 && !stats.isLossless) {
            parts.add("$kbps kbps")
        }

        val bitDepth = stats.bitDepth
        if (bitDepth != null && bitDepth > 0 && stats.isLossless) {
            parts.add("$bitDepth-bit")
        }

        val sampleRate = stats.sampleRateHz
        if (sampleRate != null && sampleRate > 0) {
            val mhz = NerdStats.megahertzLabel(sampleRate)
            if (mhz != null) {
                parts.add(mhz)
            } else {
                val khz = sampleRate / 1000f
                val formatted = if (khz % 1f == 0f) {
                    "${khz.toInt()} kHz"
                } else {
                    "%.1f kHz".format(Locale.ROOT, khz)
                }
                parts.add(formatted)
            }
        }

        return if (parts.isEmpty()) null else parts.joinToString(" · ")
    }

    fun formatBadge(stats: NerdStats.Snapshot?): AudioQualityBadgeInfo? {
        if (stats == null) return null
        val tier = resolveTier(stats)
        if (tier == AudioQualityTier.UNKNOWN) return null
        val details = formatDetails(stats)
        return AudioQualityBadgeInfo(
            tier = tier,
            title = tier.label,
            details = details,
            isLossless = tier == AudioQualityTier.LOSSLESS || tier == AudioQualityTier.HI_RES_LOSSLESS,
            isAnimated = tier == AudioQualityTier.LOSSLESS || tier == AudioQualityTier.HI_RES_LOSSLESS || tier == AudioQualityTier.DOLBY_ATMOS,
        )
    }
}
