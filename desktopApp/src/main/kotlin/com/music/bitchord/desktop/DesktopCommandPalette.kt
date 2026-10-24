package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

/** One thing the palette can do: a name to type for, and what running it does. */
internal class DesktopPaletteCommand(
    val label: String,
    val icon: ImageVector?,
    val run: () -> Unit,
)

/**
 * The Ctrl+K palette: every place the app can go and everything it can do on the spot, found
 * by typing. Navigation and transport and the appearance toggles — the things that already
 * have state to read — rather than a second search engine; the search page is the searcher.
 */
@Composable
internal fun DesktopCommandPalette(
    open: Boolean,
    onClose: () -> Unit,
    commands: List<DesktopPaletteCommand>,
) {
    if (!open) return
    var query by remember(open) { mutableStateOf("") }
    var selected by remember(open) { mutableIntStateOf(0) }
    val filtered = remember(query, commands) {
        val needle = query.trim()
        if (needle.isEmpty()) commands else commands.filter { it.label.contains(needle, ignoreCase = true) }
    }
    LaunchedEffect(query) { selected = 0 }
    val fieldFocus = remember { FocusRequester() }
    LaunchedEffect(open) { runCatching { fieldFocus.requestFocus() } }

    fun run(command: DesktopPaletteCommand) {
        onClose()
        command.run()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClose),
    ) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
                .width(560.dp)
                .clip(RoundedCornerShape(18.dp))
                .desktopBarGlass(RoundedCornerShape(18.dp)),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    ">",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.None),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(fieldFocus)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.Escape -> { onClose(); true }
                                Key.DirectionDown -> {
                                    if (filtered.isNotEmpty()) selected = (selected + 1) % filtered.size
                                    true
                                }
                                Key.DirectionUp -> {
                                    if (filtered.isNotEmpty()) selected = Math.floorMod(selected - 1, filtered.size)
                                    true
                                }
                                Key.Enter -> {
                                    filtered.getOrNull(selected)?.let(::run)
                                    true
                                }
                                else -> false
                            }
                        },
                )
            }
            if (filtered.isEmpty()) {
                Text(
                    "Nothing matches \"$query\"",
                    color = DesktopSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 18.dp).padding(bottom = 14.dp),
                )
                return@Column
            }
            Column(Modifier.padding(bottom = 8.dp)) {
                filtered.take(PALETTE_ROWS).forEachIndexed { index, command ->
                    val isSel = index == selected.coerceIn(0, filtered.size - 1)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                            )
                            .clickable { run(command) }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        command.icon?.let {
                            Icon(
                                it,
                                null,
                                tint = if (isSel) MaterialTheme.colorScheme.primary else DesktopSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            command.label,
                            color = if (isSel) Color.White else DesktopSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private const val PALETTE_ROWS = 9
