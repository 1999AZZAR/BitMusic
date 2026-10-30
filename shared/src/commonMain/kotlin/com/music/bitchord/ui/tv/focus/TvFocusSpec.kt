package com.music.bitchord.ui.tv.focus

/**
 * 10-foot TV Focus Specification constants and calculation logic.
 * Decoupled from Android UI toolkit for multiplatform unit testing.
 */
object TvFocusSpec {
    const val FOCUSED_SCALE = 1.05f
    const val RESTING_SCALE = 1.0f
    const val FOCUSED_BORDER_WIDTH_DP = 2.5f
    const val RESTING_BORDER_WIDTH_DP = 0.0f
    const val FOCUSED_ELEVATION_DP = 16.0f
    const val RESTING_ELEVATION_DP = 0.0f

    /**
     * Calculates the animated target scale factor.
     * When pressed, scale slightly damps down towards resting.
     */
    fun calculateScale(isFocused: Boolean, isPressed: Boolean): Float {
        return when {
            isPressed -> RESTING_SCALE + (FOCUSED_SCALE - RESTING_SCALE) * 0.35f
            isFocused -> FOCUSED_SCALE
            else -> RESTING_SCALE
        }
    }

    /**
     * Calculates the border stroke width in Dp.
     */
    fun calculateBorderWidth(isFocused: Boolean): Float {
        return if (isFocused) FOCUSED_BORDER_WIDTH_DP else RESTING_BORDER_WIDTH_DP
    }

    /**
     * Calculates elevation shadow in Dp.
     */
    fun calculateElevation(isFocused: Boolean, isPressed: Boolean): Float {
        return if (isFocused && !isPressed) FOCUSED_ELEVATION_DP else RESTING_ELEVATION_DP
    }
}
