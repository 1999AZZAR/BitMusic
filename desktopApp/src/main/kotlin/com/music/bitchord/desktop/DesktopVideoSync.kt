package com.music.bitchord.desktop

/**
 * Directives issued by [DesktopVideoSyncClock] to align video frame rendering
 * with the audio presentation clock.
 */
sealed interface DesktopVideoSyncAction {
    /** The frame is on time (within tolerance) and should be rendered immediately. */
    data object RENDER : DesktopVideoSyncAction

    /** The frame is behind the audio playhead and should be dropped/skipped to catch up. */
    data object DROP : DesktopVideoSyncAction

    /** The frame is ahead of the audio playhead and should wait until audio catches up. */
    data class WAIT(val delayMs: Long) : DesktopVideoSyncAction
}

/**
 * Playhead clock synchronization between video frames and the audio engine.
 */
class DesktopVideoSyncClock(
    val toleranceEarlyMs: Long = 4L,
    val toleranceLateMs: Long = 120L,
    /**
     * Offset applied to align video presentation with physical acoustic output.
     * Negative value delays video rendering to allow buffered audio to exit physical speakers.
     */
    val displayLeadMs: Long = -30L,
) {
    /**
     * Determines whether the decoded frame with presentation timestamp [framePtsMs]
     * should be rendered, dropped, or held relative to [audioPositionMs].
     */
    fun evaluate(framePtsMs: Long, audioPositionMs: Long): DesktopVideoSyncAction {
        val targetAudioMs = audioPositionMs + displayLeadMs
        val diff = framePtsMs - targetAudioMs
        return when {
            diff < -toleranceLateMs -> DesktopVideoSyncAction.DROP
            diff > toleranceEarlyMs -> DesktopVideoSyncAction.WAIT(diff)
            else -> DesktopVideoSyncAction.RENDER
        }
    }

    /**
     * Checks if audio clock jumped significantly (e.g. user scrubbed or seeked),
     * requiring the video decoder pipeline to flush and seek.
     */
    fun isDiscontinuous(lastAudioMs: Long, currentAudioMs: Long, thresholdMs: Long = 500L): Boolean {
        if (currentAudioMs < lastAudioMs) return true
        return (currentAudioMs - lastAudioMs) > thresholdMs
    }
}
