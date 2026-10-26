package com.music.bitchord.ui.tv.remote

/**
 * High-level user intent triggered by TV remote control buttons or media hardware keys.
 */
enum class TvRemoteAction {
    PLAY_PAUSE,
    PLAY,
    PAUSE,
    NEXT_TRACK,
    PREVIOUS_TRACK,
    FAST_FORWARD, // Seek forward step (+10s)
    REWIND,       // Seek backward step (-10s)
    TOGGLE_DRAWER, // Open/close playback queue and lyrics drawer
    TOGGLE_MENU,   // Contextual menu actions
    NONE
}

/**
 * Maps Android TV remote control key codes and hardware media keys to actionable player intents.
 * Decoupled from Android Framework classes for pure testability.
 */
object TvRemoteKeyDispatcher {

    const val SEEK_STEP_MS = 10_000L

    // Standard Android KeyEvent keyCodes
    const val KEYCODE_DPAD_UP = 19
    const val KEYCODE_MENU = 82
    const val KEYCODE_MEDIA_PLAY_PAUSE = 85
    const val KEYCODE_MEDIA_NEXT = 87
    const val KEYCODE_MEDIA_PREVIOUS = 88
    const val KEYCODE_MEDIA_REWIND = 89
    const val KEYCODE_MEDIA_FAST_FORWARD = 90
    const val KEYCODE_MEDIA_PLAY = 126
    const val KEYCODE_MEDIA_PAUSE = 127

    /**
     * Resolves the [TvRemoteAction] for a given [keyCode].
     *
     * @param keyCode The Android KeyEvent key code.
     * @param isPlayingViewActive True if the TV interface is currently showing full-screen playback.
     */
    fun resolveAction(keyCode: Int, isPlayingViewActive: Boolean = false): TvRemoteAction {
        return when (keyCode) {
            KEYCODE_MEDIA_PLAY_PAUSE -> TvRemoteAction.PLAY_PAUSE
            KEYCODE_MEDIA_PLAY -> TvRemoteAction.PLAY
            KEYCODE_MEDIA_PAUSE -> TvRemoteAction.PAUSE
            KEYCODE_MEDIA_NEXT -> TvRemoteAction.NEXT_TRACK
            KEYCODE_MEDIA_PREVIOUS -> TvRemoteAction.PREVIOUS_TRACK
            KEYCODE_MEDIA_FAST_FORWARD -> TvRemoteAction.FAST_FORWARD
            KEYCODE_MEDIA_REWIND -> TvRemoteAction.REWIND
            KEYCODE_DPAD_UP -> if (isPlayingViewActive) TvRemoteAction.TOGGLE_DRAWER else TvRemoteAction.NONE
            KEYCODE_MENU -> TvRemoteAction.TOGGLE_MENU
            else -> TvRemoteAction.NONE
        }
    }
}
