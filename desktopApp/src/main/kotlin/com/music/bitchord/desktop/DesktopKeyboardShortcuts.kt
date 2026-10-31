package com.music.bitchord.desktop

/**
 * Global keyboard shortcut hooks. The app's composable sets the handlers once it is composed; the
 * AWT-level listener installed in [Main] dispatches media keys to them, so play/pause/next/previous
 * work even when the window does not have Compose focus (e.g. media keys on Linux/Windows/macOS).
 */
internal object DesktopKeyboardShortcuts {
    var onPlayPause: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
    var onSeekBack: (() -> Unit)? = null
    var onSeekForward: (() -> Unit)? = null
}
