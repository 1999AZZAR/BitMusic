package com.music.bitchord.desktop

import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.types.Variant
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Freedesktop notifications (org.freedesktop.Notifications) for track changes on Linux, so the
 * desktop shell shows what is playing together with its artwork. Other platforms fall back to
 * their own mechanisms (Windows SMTC, macOS later).
 */
@DBusInterfaceName("org.freedesktop.Notifications")
interface FreedesktopNotifications : DBusInterface {
    fun Notify(
        appName: String,
        replacesId: UInt,
        appIcon: String,
        summary: String,
        body: String,
        actions: List<String>,
        hints: Map<String, Variant<*>>,
        expireTimeout: Int,
    ): UInt

    fun CloseNotification(id: UInt)
}

internal object DesktopLinuxNotifications {
    private val lock = Any()
    private var worker: ExecutorService? = null
    private fun worker(): ExecutorService = synchronized(lock) {
        worker?.takeIf { !it.isShutdown } ?: Executors.newSingleThreadExecutor { r ->
            Thread(r, "BitMusic-Notify").apply { isDaemon = true }
        }.also { worker = it }
    }

    private var connection: org.freedesktop.dbus.connections.impl.DBusConnection? = null
    private var started = false
    private var lastId: UInt = 0u

    fun isLinux(): Boolean = DesktopPlatform.isLinux

    fun start() {
        if (!isLinux()) return
        if (synchronized(lock) { started }) return
        synchronized(lock) { started = true }
        worker().execute {
            runCatching {
                val bus = DBusConnectionBuilder.forSessionBus().withShared(false).build()
                synchronized(lock) { connection = bus }
            }.onFailure {
                synchronized(lock) { started = false; connection = null }
                DesktopTrackLog.log("notifications: could not connect to session bus: ${it.message}")
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            connection?.close()
            connection = null
            started = false
            worker?.shutdownNow()
            worker = null
        }
    }

    /**
     * Shows (or replaces) a notification for the current track. [artworkPath] may be a local file
     * path or a file:// URI; when null only the app icon is used.
     */
    fun notifyTrack(title: String, artist: String, artworkPath: String?) {
        if (!isLinux()) return
        worker().execute {
            val bus = synchronized(lock) { connection } ?: return@execute
            runCatching {
                val remote = bus.getRemoteObject(
                    "org.freedesktop.Notifications",
                    "/org/freedesktop/Notifications",
                    FreedesktopNotifications::class.java,
                )
                val hints = linkedMapOf<String, Variant<*>>()
                artworkPath?.takeIf(String::isNotBlank)?.let { path ->
                    val file = if (path.startsWith("file://")) File(URI(path)) else File(path)
                    if (file.isFile) hints["image-path"] = Variant(file.absolutePath)
                }
                val id = remote.Notify(
                    "BitMusic",
                    lastId,
                    "bitmusic",
                    title,
                    artist,
                    emptyList(),
                    hints,
                    5000,
                )
                synchronized(lock) { lastId = id }
            }.onFailure {
                DesktopTrackLog.log("notifications: could not notify: ${it.message}")
            }
        }
    }

    private fun URI(s: String): java.net.URI = java.net.URI(s)
}