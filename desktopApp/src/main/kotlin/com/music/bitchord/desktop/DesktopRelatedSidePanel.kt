package com.music.bitchord.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.model.ArtistPage
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.icons.BitChordIcons
import kotlinx.coroutines.launch

/**
 * Spotify "Now Playing View" + YouTube Music "Related" context sidepanel.
 * Displays rich artist background, album connection, and dynamic similar track recommendations.
 */
@Composable
internal fun DesktopRelatedSidePanel(
    song: Song?,
    onPlaySong: (Song) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    onAddToQueue: ((Song) -> Unit)? = null,
) {
    if (song == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    BitChordIcons.Explore,
                    contentDescription = null,
                    tint = DesktopSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = DesktopStrings["d_nothing_playing", "Play a track to see related music & artist details"],
                    color = DesktopSecondary,
                    fontSize = 13.sp,
                )
            }
        }
        return
    }

    val scope = rememberCoroutineScope()
    var artistDetails by remember(song.videoId, song.artistId) { mutableStateOf<ArtistPage?>(null) }
    var similarSongs by remember(song.videoId) { mutableStateOf<List<Song>>(emptyList()) }
    var loadingSimilar by remember(song.videoId) { mutableStateOf(true) }
    var loadingArtist by remember(song.videoId) { mutableStateOf(false) }

    var resolvedArtistId by remember(song.videoId, song.artistId) { mutableStateOf(song.artistId) }
    var resolvedAlbumId by remember(song.videoId, song.albumId) { mutableStateOf(song.albumId) }
    var resolvedAlbumName by remember(song.videoId, song.albumName) { mutableStateOf(song.albumName) }

    LaunchedEffect(song.videoId) {
        loadingSimilar = true
        loadingArtist = true

        // 1. Fetch similar tracks / radio stream
        scope.launch {
            DesktopSearchClient.radio(song.videoId).fold(
                onSuccess = { tracks ->
                    similarSongs = tracks.filterNot { it.videoId == song.videoId }
                    loadingSimilar = false
                },
                onFailure = {
                    similarSongs = emptyList()
                    loadingSimilar = false
                },
            )
        }

        // 2. Resolve missing artistId or albumId if necessary
        var effectiveArtistId = resolvedArtistId
        if (effectiveArtistId == null || resolvedAlbumId == null) {
            val trackInfo = DesktopSearchClient.trackLinks(song.videoId).getOrNull()
            if (trackInfo != null) {
                if (effectiveArtistId == null && trackInfo.artistId != null) {
                    effectiveArtistId = trackInfo.artistId
                    resolvedArtistId = effectiveArtistId
                }
                if (resolvedAlbumId == null && trackInfo.albumId != null) {
                    resolvedAlbumId = trackInfo.albumId
                }
                if (resolvedAlbumName == null && trackInfo.albumName != null) {
                    resolvedAlbumName = trackInfo.albumName
                }
            }
        }

        // 3. Load full artist profile if artistId is resolved
        if (effectiveArtistId != null) {
            DesktopSearchClient.artistPage(effectiveArtistId).fold(
                onSuccess = { page ->
                    artistDetails = page
                    loadingArtist = false
                },
                onFailure = {
                    loadingArtist = false
                },
            )
        } else {
            loadingArtist = false
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        // --- Card 1: About the Artist (Spotify Now Playing View) ---
        item(key = "about-artist") {
            AboutArtistCard(
                artistName = song.artist,
                artistDetails = artistDetails,
                loading = loadingArtist,
                onOpenArtist = {
                    resolvedArtistId?.let(onOpenArtist)
                },
            )
        }

        // --- Card 2: Album Card (if song belongs to an album) ---
        if (!resolvedAlbumName.isNullOrBlank()) {
            item(key = "from-album") {
                AlbumContextCard(
                    albumName = resolvedAlbumName.orEmpty(),
                    artistName = song.artist,
                    coverUrl = song.thumbnailUrl,
                    onOpenAlbum = {
                        val albId = resolvedAlbumId
                        if (albId != null) {
                            onOpenAlbum(albId, resolvedAlbumName.orEmpty())
                        }
                    },
                )
            }
        }

        // --- Section 3: Similar Tracks / Radio (YouTube Music Related) ---
        item(key = "similar-header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    BitChordIcons.Explore,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = DesktopStrings["similar_tracks", "Similar Tracks"],
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (loadingSimilar) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 1.5.dp,
                        color = DesktopSecondary,
                    )
                }
            }
        }

        if (similarSongs.isNotEmpty()) {
            items(similarSongs, key = { it.videoId }) { itemSong ->
                SimilarSongRow(
                    song = itemSong,
                    onClick = { onPlaySong(itemSong) },
                    onAddToQueue = onAddToQueue?.let { cb -> { cb(itemSong) } },
                )
            }
        } else if (!loadingSimilar) {
            item(key = "empty-similar") {
                Text(
                    text = DesktopStrings["no_similar_tracks", "No related tracks available"],
                    color = DesktopSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun AboutArtistCard(
    artistName: String,
    artistDetails: ArtistPage?,
    loading: Boolean,
    onOpenArtist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedBio by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.75.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = DesktopStrings["about_the_artist", "About the Artist"],
                color = DesktopSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val avatarUrl = artistDetails?.thumbnailUrl
                if (!avatarUrl.isNullOrBlank()) {
                    DesktopArtwork(
                        url = avatarUrl,
                        modifier = Modifier.size(52.dp).clip(CircleShape),
                        px = 128,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = artistName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val listenerCount = artistDetails?.monthlyListenerCount
                    val subscriberCount = artistDetails?.subscriberCountText
                    val stat = listenerCount ?: subscriberCount
                    if (!stat.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stat,
                            color = DesktopSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            val bio = artistDetails?.description
            if (!bio.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = bio,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    maxLines = if (expandedBio) 12 else 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .animateContentSize()
                        .clickable { expandedBio = !expandedBio },
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenArtist)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = DesktopStrings["open_artist", "View Artist Profile"],
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = DesktopSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun AlbumContextCard(
    albumName: String,
    artistName: String,
    coverUrl: String?,
    onOpenAlbum: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.75.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .clickable(onClick = onOpenAlbum)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DesktopArtwork(
                url = coverUrl,
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)),
                px = 128,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = DesktopStrings["from_the_album", "From the album"],
                    color = DesktopSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = albumName,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artistName,
                    color = DesktopSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = DesktopSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SimilarSongRow(
    song: Song,
    onClick: () -> Unit,
    onAddToQueue: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var hovered by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (hovered) Color.White.copy(alpha = 0.07f) else Color.Transparent)
            .clickable(onClick = onClick)
            .pointerInput(song.videoId) {
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            PointerEventType.Enter -> hovered = true
                            PointerEventType.Exit -> hovered = false
                        }
                    }
                }
            }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesktopArtwork(
            url = song.thumbnailUrl,
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(6.dp)),
            px = 96,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                color = DesktopSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val duration = song.durationText
        if (!duration.isNullOrBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = duration,
                color = DesktopSecondary,
                fontSize = 11.sp,
            )
        }
        if (hovered && onAddToQueue != null) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onAddToQueue),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    BitChordIcons.Plus,
                    contentDescription = "Add to queue",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}
