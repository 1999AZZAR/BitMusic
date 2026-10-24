package com.music.bitchord.desktop

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * The things the app says out loud. The audio engine recovers an output, a download fails, a
 * source switches — events that used to live in `desktop.log` where nobody looks, shown as a
 * quiet glass pill above the player bar for a few seconds instead.
 *
 * Emitters can be any thread (the engine's is the audio thread); [show] never blocks.
 */
internal object DesktopToasts {

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val messages: SharedFlow<String> = _messages

    fun show(message: String) {
        _messages.tryEmit(message)
    }
}

/** The pills themselves, laid over everything; host once, at the window's root. */
@Composable
internal fun DesktopToastHost() {
    val live = remember { mutableStateListOf<Pair<Long, String>>() }
    LaunchedEffect(Unit) {
        DesktopToasts.messages.collect { message ->
            val id = System.nanoTime()
            live.add(id to message)
            delay(TOAST_MS)
            live.removeAll { it.first == id }
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier
                .padding(bottom = 92.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            live.takeLast(VISIBLE_TOASTS).forEach { (_, message) ->
                Text(
                    message,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .desktopBarGlass(RoundedCornerShape(999.dp))
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
    }
}

private const val TOAST_MS = 3_500L
private const val VISIBLE_TOASTS = 3
