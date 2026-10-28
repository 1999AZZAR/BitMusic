package com.music.bitchord.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.model.AudioSourceOrigin
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.audioSourceOrigin

/**
 * A compact, visually crisp badge identifying the streaming or storage origin of a track
 * (SoundCloud, YouTube, Local files, JioSaavn, or Addon services).
 */
@Composable
fun AudioSourceBadge(
    song: Song,
    modifier: Modifier = Modifier,
    useShortLabel: Boolean = true,
    showYouTube: Boolean = false,
) {
    val origin = song.audioSourceOrigin
    // YouTube is the canonical catalogue default; unless explicitly requested,
    // only non-default origins (SoundCloud, Local, Addon, JioSaavn) are highlighted
    // to keep standard album tracklists visually clean.
    if (!showYouTube && origin == AudioSourceOrigin.YOUTUBE) return

    val brandColor = when (origin) {
        AudioSourceOrigin.SOUNDCLOUD -> Color(0xFFFF5500)
        AudioSourceOrigin.LOCAL -> Color(0xFF30D158)
        AudioSourceOrigin.JIOSAAVN -> Color(0xFF00BFA5)
        AudioSourceOrigin.ADDON -> Color(0xFFBF5AF2)
        AudioSourceOrigin.YOUTUBE -> Color(0xFFFF453A)
    }

    val shape = RoundedCornerShape(3.5.dp)

    Surface(
        modifier = modifier
            .clip(shape)
            .border(0.5.dp, brandColor.copy(alpha = 0.5f), shape),
        color = brandColor.copy(alpha = 0.14f),
        shape = shape,
    ) {
        Text(
            text = if (useShortLabel) origin.shortLabel else origin.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                lineHeight = 10.sp,
            ),
            color = brandColor,
            modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp),
            maxLines = 1,
        )
    }
}
