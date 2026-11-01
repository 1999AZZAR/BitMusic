package com.music.bitchord.desktop

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Runs the Listen Together party server that ships inside this build, so a party can be hosted
 * without a separate download or a terminal. Linux only for now — other platforms keep using a
 * manually configured server, and the "Use built-in" button degrades to offline there.
 */
internal object DesktopPartyServer {
    private var process: Process? = null

    fun isAvailable(): Boolean = DesktopPlatform.isLinux

    fun start() {
        if (!isAvailable()) return
        if (process?.isAlive == true) return
        runCatching {
            val dir = DesktopMediaCache.directory.resolve("party-server")
            Files.createDirectories(dir)
            val bin = dir.resolve("listen-together")
            if (!Files.isRegularFile(bin)) {
                val stream = DesktopPartyServer::class.java.getResourceAsStream("/backend/listen-together")
                    ?: return
                stream.use { input -> Files.copy(input, bin, StandardCopyOption.REPLACE_EXISTING) }
                bin.toFile().setExecutable(true)
            }
            val pb = ProcessBuilder(bin.toAbsolutePath().toString())
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD)
            pb.redirectError(ProcessBuilder.Redirect.DISCARD)
            process = pb.start()
            Runtime.getRuntime().addShutdownHook(Thread { stop() })
        }.onFailure { DesktopTrackLog.log("party server: failed to start: ${it.message}") }
    }

    fun stop() {
        runCatching { process?.destroy() }
        process = null
    }
}