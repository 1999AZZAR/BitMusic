package com.music.bitchord.desktop

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.system.exitProcess

/**
 * One BitMusic at a time. A second launch would fight the first for the audio output, the
 * settings and the tray — and pay for a second WebView, a second decode-ahead, a second
 * everything, to do it.
 *
 * The guard is an OS-level lock on a file in the app's state directory: atomic to take, and
 * released by the kernel the moment the holder dies, crash included — no stale lock can ever
 * keep the app from starting. The first instance also listens on a loopback socket whose port
 * it writes into that file, so a second launch does not merely die: it asks the running
 * instance to bring its window forward first, which is what launching the app from a launcher
 * means anyway.
 */
internal object DesktopSingleInstance {

    private const val LOCK_NAME = "instance.lock"
    private const val PORT_NAME = "instance.port"
    private const val ACTIVATE = "activate"

    private var lock: FileLock? = null
    private var portFile: Path? = null
    private var server: ServerSocket? = null

    /**
     * Takes the single-instance lock. True when this process is the one instance and may start;
     * false when another already holds it — in which case that instance has been asked to show
     * itself and this process should exit immediately.
     *
     * A lock that cannot be taken at all (a read-only home, a full disk) is not evidence of a
     * second instance, so it starts anyway rather than refusing to run over housekeeping.
     */
    fun acquire(onActivate: () -> Unit): Boolean = try {
        val directory = stateDirectory()
        // The port lives in its own file, opened and closed freely: writing it through the
        // locked file's own path would open a second channel to it, and POSIX releases a
        // process's lock on a file the moment ANY of its channels to that file closes.
        portFile = directory.resolve(PORT_NAME)
        val channel = FileChannel.open(
            directory.resolve(LOCK_NAME),
            StandardOpenOption.CREATE,
            StandardOpenOption.READ,
            StandardOpenOption.WRITE,
        )
        val taken = channel.tryLock()
        if (taken == null) {
            channel.close()
            askToSurface()
            false
        } else {
            lock = taken
            listenForActivation(onActivate)
            true
        }
    } catch (failure: Exception) {
        DesktopTrackLog.log("single-instance lock unavailable; starting anyway: ${failure.message}")
        true
    }

    /** `$XDG_STATE_HOME/bitchord`, the directory the desktop log already lives in. */
    private fun stateDirectory(): Path {
        val state = System.getenv("XDG_STATE_HOME")?.takeIf { it.isNotBlank() }
            ?: "${System.getProperty("user.home")}/.local/state"
        return Files.createDirectories(Path.of(state, "bitchord"))
    }

    /**
     * The socket half: bound to loopback on a port the kernel chose, so it can never collide
     * with anything, and named in the lock file for whoever comes second.
     */
    private fun listenForActivation(onActivate: () -> Unit) {
        val socket = ServerSocket(0, 4, InetAddress.getLoopbackAddress())
        server = socket
        Files.writeString(
            portFile!!,
            "port=${socket.localPort}\n",
            StandardCharsets.UTF_8,
        )
        Thread({
            while (!socket.isClosed) {
                // One bad connection must not take the listener down with it.
                val handled = runCatching {
                    socket.accept().use { client ->
                        client.soTimeout = 2_000
                        val asked = BufferedReader(
                            InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8),
                        ).readLine()
                        if (asked == ACTIVATE) onActivate()
                    }
                }
                if (handled.isFailure && !socket.isClosed) {
                    Thread.sleep(200)
                }
            }
        }, "BitMusic-SingleInstance").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY - 1
        }.start()
    }

    /** What a second launch does instead of starting: wake the instance that holds the lock. */
    private fun askToSurface() {
        val file = portFile ?: return
        val port = runCatching {
            Files.readString(file, StandardCharsets.UTF_8)
                .lineSequence().firstOrNull { it.startsWith("port=") }
                ?.substringAfter('=')?.trim()?.toIntOrNull()
        }.getOrNull() ?: return
        runCatching {
            Socket().use { client ->
                client.connect(InetSocketAddress(InetAddress.getLoopbackAddress(), port), 1_500)
                client.getOutputStream().write("$ACTIVATE\n".toByteArray(StandardCharsets.UTF_8))
                client.getOutputStream().flush()
            }
            DesktopTrackLog.log("another instance is running; asked it to come forward")
        }.onFailure {
            // The lock is held but nothing answered — the holder is starting up or going away.
            DesktopTrackLog.log("another instance is running, but it did not answer; not starting a second")
        }
    }

    /** What a second launch runs instead of the app. */
    fun exitAsDuplicate(): Nothing {
        exitProcess(0)
    }
}
