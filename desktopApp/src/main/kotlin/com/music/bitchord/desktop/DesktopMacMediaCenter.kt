package com.music.bitchord.desktop

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

/**
 * macOS Now Playing / Control Center integration. Best-effort: every native call is wrapped so a
 * failure on an older macOS or a missing framework degrades to "no system media card" instead of
 * crashing the app. Media keys and in-app shortcuts still work regardless.
 */
internal object DesktopMacMediaCenter {

    interface LibObjC : Library {
        fun objc_getClass(name: String): Pointer?
        fun sel_registerName(name: String): Pointer?
        fun objc_msgSend(receiver: Pointer?, selector: Pointer?, vararg args: Any?): Pointer?
    }

    private val objc: LibObjC? by lazy {
        runCatching { Native.load("objc", LibObjC::class.java) }.getOrNull()
    }

    private fun cls(name: String): Pointer? = objc?.objc_getClass(name)
    private fun sel(name: String): Pointer? = objc?.sel_registerName(name)

    private fun msg(receiver: Pointer?, selector: String, vararg args: Any?): Pointer? =
        runCatching { objc?.objc_msgSend(receiver, sel(selector), *args) }.getOrNull()

    private var started = false

    fun isMac(): Boolean = DesktopPlatform.isMac

    fun start() {
        if (!isMac()) return
        started = true
    }

    fun stop() {
        started = false
    }

    fun publish(playback: DesktopPlaybackState) {
        if (!isMac() || !started) return
        runCatching {
            val song = playback.song ?: return
            val centerCls = cls("MPNowPlayingInfoCenter") ?: return
            val defaultCenterSel = sel("defaultCenter") ?: return
            val center = msg(centerCls, "defaultCenter") ?: return

            val title = nsString(song.title)
            val artist = nsString(song.artist)
            val album = song.albumName?.takeIf(String::isNotBlank)?.let(::nsString)

            val keys = mutableListOf<Pointer?>()
            val values = mutableListOf<Pointer?>()
            fun add(key: String, value: Pointer?) {
                if (value == null) return
                keys += nsString(key)
                values += value
            }
            add("MPMediaItemPropertyTitle", title)
            add("MPMediaItemPropertyArtist", artist)
            album?.let { add("MPMediaItemPropertyAlbumTitle", it) }
            if (playback.durationMs > 0L) {
                add("MPMediaItemPropertyPlaybackDuration", nsNumber(playback.durationMs / 1000.0))
            }
            add("MPNowPlayingInfoPropertyElapsedPlaybackTime", nsNumber(playback.positionMs / 1000.0))
            add("MPNowPlayingInfoPropertyPlaybackRate", nsNumber(if (playback.isPlaying) 1.0 else 0.0))

            if (keys.isEmpty()) return
            val dict = dictionary(keys, values) ?: return
            msg(center, "setNowPlayingInfo:", dict)
        }
    }

    private fun nsString(s: String): Pointer? =
        runCatching {
            val cls = cls("NSString") ?: return null
            msg(cls, "stringWithUTF8String:", s)
        }.getOrNull()

    private fun nsNumber(d: Double): Pointer? =
        runCatching {
            val cls = cls("NSNumber") ?: return null
            msg(cls, "numberWithDouble:", d)
        }.getOrNull()

    private fun dictionary(keys: List<Pointer?>, values: List<Pointer?>): Pointer? =
        runCatching {
            val cls = cls("NSDictionary") ?: return null
            // dictionaryWithObjects:forKeys:count:
            // Need to pass C arrays; build via JNA Memory.
            val n = keys.size
            val ptrSize = 8
            val valuesMem = com.sun.jna.Memory(n.toLong() * ptrSize)
            val keysMem = com.sun.jna.Memory(n.toLong() * ptrSize)
            for (i in 0 until n) {
                valuesMem.setPointer(i.toLong() * ptrSize, values[i])
                keysMem.setPointer(i.toLong() * ptrSize, keys[i])
            }
            msg(cls, "dictionaryWithObjects:forKeys:count:", valuesMem, keysMem, n.toLong())
        }.getOrNull()
}