package com.music.bitchord.desktop

import androidx.compose.runtime.CompositionLocalProvider
import com.music.bitchord.ui.player.PlayerPlatform
import com.music.bitchord.data.DebugLog
import com.music.bitchord.data.TrackLog
import com.music.bitchord.data.innertube.InnerTubeXResolver
import com.music.bitchord.data.innertube.StreamResolver
import com.music.bitchord.data.innertube.potoken.PoTokenGenerator
import com.music.bitchord.data.lyrics.LyricsTranslation
import com.music.bitchord.data.spotify.SpotifyLibrary
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowPlacement
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import bitchord.desktopapp.generated.resources.Res
import bitchord.desktopapp.generated.resources.logo
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.Toolkit
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.painterResource

fun main() {
    // One instance at a time, before anything expensive exists: a second launch asks the
    // running one to come forward and exits, rather than fighting it for the speakers, the
    // settings and the tray.
    if (!DesktopSingleInstance.acquire { DesktopWindowVisibility.show() }) {
        DesktopSingleInstance.exitAsDuplicate()
    }
    DesktopArtworkCache.install()
    // Before any WebView exists, so the sign-in window can start over with an empty jar.
    DesktopWebCookies.install()
    DesktopWindowVisibility.install()
    // The player is the phone's, from the shared UI module; this is what it reads underneath.
    PlayerPlatform.install(DesktopPlayerHost)
    com.music.bitchord.ui.AppUi.install(DesktopAppUiHost)
    // The data layer both apps share logs through here, and keeps translated
    // lyrics beside the rest of the desktop's cache.
    DebugLog.sink = DebugLog.Sink { level, tag, message, error ->
        DesktopTrackLog.log("$tag/$level: $message" + (error?.let { " (${it.message})" } ?: ""))
    }
    LyricsTranslation.cacheDir = DesktopMediaCache.directory.toFile()
    // The Spotify library both apps share, on the tokens the web player mints in JavaFX.
    SpotifyLibrary.auth = SpotifyLibrary.Auth {
        DesktopSpotifyToken.accessToken()?.let { SpotifyLibrary.Tokens(it, DesktopSpotifyToken.clientToken()) }
    }
    // Desktop follows its own language picker (zh-Hant / zh-Hans preserved).
    com.music.bitchord.data.innertube.Innertube.appLanguage = { DesktopStrings.resolvedTag() }
    // YouTube playback is the phone's StreamResolver over InnerTubeX, with BotGuard PoTokens
    // minted in JavaFX's WebView where the phone uses Android's.
    TrackLog.echo = { level, tag, message, error ->
        DesktopTrackLog.log("$tag/$level: $message" + (error?.let { " (${it.message})" } ?: ""))
    }
    StreamResolver.maxKbps = {
        DesktopStreamClient.ceiling.get() ?: DesktopSourceRegistry.ceiling(null).maxKbps
    }
    InnerTubeXResolver.init(
        filesDir = DesktopMediaCache.directory.toFile(),
        store = DesktopInnerTubeXStore,
        poTokenProvider = PoTokenGenerator(
            createMinter = { DesktopPoTokenWebView.getNewPoTokenGenerator() },
            available = { DesktopPoTokenWebView.available },
        ).asTokenProvider(),
        prewarm = false,
    )
    // The public "apps open right now" count; see Presence.
    DesktopPresence.install()
    desktopMain()
}

private fun desktopMain() = application {
    // Closing puts the window away rather than ending the process, while there is a tray icon to
    // bring it back from — see [DesktopWindowVisibility].
    val visible by DesktopWindowVisibility.visible.collectAsState()
    // The window comes back where it was left: size and place survive restarts, debounced so a
    // drag across the desk writes one value rather than two hundred. A remembered place that
    // is off every current screen — the monitor it lived on is gone — starts fresh instead of
    // opening in nowhere.
    val remembered = remember { DesktopPersistence().string(KEY_WINDOW_GEOMETRY) }
        ?.split('|')
        ?.mapNotNull { it.toIntOrNull() }
        ?.takeIf { it.size == 4 }
    val screen = remember {
        runCatching {
            java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
        }.getOrNull()
    }
    val startAt = remembered?.takeIf { rect ->
        screen == null || (
            rect[2] >= screen.x - 16 && rect[3] >= screen.y - 16 &&
                rect[2] < screen.x + screen.width && rect[3] < screen.y + screen.height
            )
    }
    val state = rememberWindowState(
        width = (startAt?.get(0) ?: 1_220).dp,
        height = (startAt?.get(1) ?: 780).dp,
        position = startAt?.let { WindowPosition(it[2].dp, it[3].dp) } ?: WindowPosition.PlatformDefault,
    )
    val reduceAnimation by DesktopAppearanceSettings.reduceAnimation.collectAsState()
    val reduceBlur by DesktopAppearanceSettings.reduceDynamicBlur.collectAsState()
    LaunchedEffect(visible, state.isMinimized, reduceAnimation, reduceBlur) {
        val rendering = visible && !state.isMinimized
        DesktopWindowVisibility.setRenderingActive(rendering)
        DesktopPlayerSettings.reduceAnimation.value = reduceAnimation || !rendering
        DesktopPlayerSettings.reduceDynamicBlur.value = reduceBlur || !rendering
    }
    LaunchedEffect(state) {
        snapshotFlow { Triple(state.placement, state.size, state.position) }.collectLatest {
            delay(600)
            if (state.placement == WindowPlacement.Floating) {
                DesktopPersistence().saveString(
                    KEY_WINDOW_GEOMETRY,
                    "${state.size.width.value.toInt()}|${state.size.height.value.toInt()}" +
                        "|${state.position.x.value.toInt()}|${state.position.y.value.toInt()}",
                )
            }
        }
    }
    LaunchedEffect(visible) {
        if (visible && state.isMinimized) {
            state.isMinimized = false
        }
    }
    // Hoisted so the player can fill the screen and the caption buttons can maximize — see
    // [DesktopWindowMode]. Fullscreen on Windows is a native borderless window that covers the
    // taskbar, so it needs no placement change here; only ordinary maximize does.
    val placement by DesktopWindowMode.placement.collectAsState()
    LaunchedEffect(placement) { state.placement = placement }
    // ...and back, for the times the window is moved between placements by something that is not
    // us; see [DesktopWindowMode.adopt].
    LaunchedEffect(state) {
        snapshotFlow { state.placement }.collect(DesktopWindowMode::adopt)
    }
    val maximized by DesktopWindowMode.maximized.collectAsState()
    LaunchedEffect(maximized) {
        if (DesktopPlatform.isMac) {
            DesktopMacFrame.updateCornerRadius(maximized)
        }
    }
    Window(
        onCloseRequest = { if (DesktopWindowVisibility.onCloseRequest()) exitApplication() },
        visible = visible,
        title = "BitMusic",
        icon = painterResource(Res.drawable.logo),
        state = state,
        // No system title bar on Windows, where the application draws its own instead — see
        // [DesktopWindowChrome].
        undecorated = DesktopPlatform.drawsOwnWindowFrame,
        // For Mica and Acrylic behind the sidebar and top bar: only a transparent window lets
        // DWM's material show through the chrome. Fixed at creation, so it is on wherever the
        // material is possible and the chosen one can be switched live — see [DesktopWindowBackdrop].
        transparent = DesktopWindowBackdrop.available,
        resizable = true,
    ) {
        val composeWindow = window
        val openingSize = remember { state.size }
        DisposableEffect(composeWindow) {
            val listener = object : java.awt.event.KeyAdapter() {
                override fun keyPressed(e: java.awt.event.KeyEvent) {
                    when (e.keyCode) {
                        java.awt.event.KeyEvent.VK_SPACE -> {
                            if (e.keyCode == java.awt.event.KeyEvent.VK_SPACE) {
                                val focus = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
                                val contentPane = composeWindow.contentPane
                                if (focus != null && focus !== contentPane &&
                                    (focus.javaClass.name.contains("Text", ignoreCase = true) ||
                                        focus.javaClass.name.contains("Editor", ignoreCase = true) ||
                                        focus.javaClass.name.contains("Button", ignoreCase = true))) {
                                    return
                                }
                            }
                            DesktopKeyboardShortcuts.onPlayPause?.invoke()
                            e.consume()
                        }

                        java.awt.event.KeyEvent.VK_LEFT -> {
                            if (e.isAltDown) {
                                DesktopKeyboardShortcuts.onSeekBack?.invoke()
                                e.consume()
                            }
                        }
                        java.awt.event.KeyEvent.VK_RIGHT -> {
                            if (e.isAltDown) {
                                DesktopKeyboardShortcuts.onSeekForward?.invoke()
                                e.consume()
                            }
                        }
                    }
                }
            }
            composeWindow.addKeyListener(listener)
            (composeWindow.contentPane as? javax.swing.JComponent)?.addKeyListener(listener)
            onDispose {
                composeWindow.removeKeyListener(listener)
                (composeWindow.contentPane as? javax.swing.JComponent)?.removeKeyListener(listener)
            }
        }
        LaunchedEffect(composeWindow) {
            DesktopWindowVisibility.attachWindow(composeWindow)
            // AWT's default is white, and it is what shows for the frame or two a moved or resized
            // window takes to repaint: a white band along its edges. Near-black rather than the
            // pure thing: on a fractional-scale display the content can sit a sub-pixel short of
            // the window's edge, and that sliver paints from here — where pure black read as a
            // hard line across the player's tinted backdrop.
            // Not over a transparent window, whose clear background is what the material shows
            // through; DWM moves that window, so there is no band to hide.
            if (!DesktopWindowBackdrop.available) {
                composeWindow.background = java.awt.Color(0x12, 0x12, 0x12)
                composeWindow.contentPane.background = java.awt.Color(0x12, 0x12, 0x12)
            } else {
                composeWindow.background = java.awt.Color(0, 0, 0, 0)
                composeWindow.contentPane.background = java.awt.Color(0, 0, 0, 0)
                (composeWindow.contentPane as? javax.swing.JComponent)?.isOpaque = false
                composeWindow.rootPane.isOpaque = false
            }
            // Modern Java AWT (Java 9+) Dimensions operate in logical points/dp on all platforms,
            // matching Compose's window state. Scaling by defaultTransform scale twice caused
            // massive double-scaling bugs on Windows (1.5x) and macOS Retina (2.0x).
            composeWindow.minimumSize = Dimension(
                MIN_WINDOW_WIDTH_DP.toInt(),
                MIN_WINDOW_HEIGHT_DP.toInt(),
            )
            composeWindow.size = Dimension(
                openingSize.width.value.toInt(),
                openingSize.height.value.toInt(),
            )
            if (DesktopPlatform.isWindows && DesktopWindowsFrame.install("BitMusic")) {
                DesktopWindowBackdrop.apply()
            } else if (DesktopPlatform.isMac && DesktopMacFrame.install(composeWindow)) {
                DesktopWindowBackdrop.apply()
            }
        }
        val actions = remember {
            DesktopWindowActions(
                minimize = {
                    if (!DesktopWindowsFrame.minimize() && !(DesktopPlatform.isMac && DesktopMacFrame.minimize())) {
                        state.isMinimized = true
                    }
                },
                toggleMaximize = {
                    if (!DesktopWindowsFrame.toggleMaximize() && !(DesktopPlatform.isMac && DesktopMacFrame.toggleMaximize())) {
                        // An undecorated AWT window otherwise maximizes to the monitor bounds on
                        // Windows when the native frame bridge is unavailable. Give AWT the
                        // monitor's usable work area before Compose switches the placement.
                        if (DesktopPlatform.isWindows && !DesktopWindowMode.maximized.value) {
                            val screen = composeWindow.graphicsConfiguration.bounds
                            val insets = Toolkit.getDefaultToolkit().getScreenInsets(
                                composeWindow.graphicsConfiguration,
                            )
                            composeWindow.maximizedBounds = Rectangle(
                                screen.x + insets.left,
                                screen.y + insets.top,
                                screen.width - insets.left - insets.right,
                                screen.height - insets.top - insets.bottom,
                            )
                        }
                        DesktopWindowMode.toggleMaximized()
                    }
                },
                // The same door the system's close button went through, so the tray keeps the
                // process alive exactly as it did before.
                close = { if (DesktopWindowVisibility.onCloseRequest()) exitApplication() },
            )
        }
        CompositionLocalProvider(
            LocalDesktopWindowActions provides actions,
            LocalDesktopWindowScope provides this,
        ) {
            BitChordDesktopApp()
        }
    }
}

private const val MIN_WINDOW_WIDTH_DP = 720.0
private const val MIN_WINDOW_HEIGHT_DP = 520.0

/** Where the window's size and place are remembered between runs, as "w|h|x|y". */
private const val KEY_WINDOW_GEOMETRY = "window_geometry"

