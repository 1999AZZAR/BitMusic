package com.music.bitchord.desktop

import com.music.bitchord.data.model.artworkAt

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.music.bitchord.ui.graphics.toImageBitmap

/** One bounded image cache shared by desktop pages and the player. */
internal object DesktopArtworkCache {
    internal const val budgetBytes = 24L * 1024 * 1024
    private val decoding = Dispatchers.IO.limitedParallelism(2)

    fun install() {
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizeBytes(budgetBytes)
                        .weakReferencesEnabled(false)
                        .build()
                }
                .decoderCoroutineContext(decoding)
                .build()
        }
    }

    fun clear() {
        SingletonImageLoader.get(PlatformContext.INSTANCE).memoryCache?.clear()
    }

    suspend fun load(url: String?, px: Int = 256): ImageBitmap? {
        if (url.isNullOrBlank()) return null
        val context = PlatformContext.INSTANCE
        val request = ImageRequest.Builder(context)
            .data(url.artworkAt(px))
            .size(px.coerceIn(1, 1024))
            .build()
        return (SingletonImageLoader.get(context).execute(request) as? SuccessResult)
            ?.image?.toImageBitmap()
    }
}

/** Artwork, fetched at the size the surface drawing it actually needs. */
@Composable
fun DesktopArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    px: Int? = null,
) {
    val context = LocalPlatformContext.current
    val sized = if (px != null) url.artworkAt(px) else url
    val request = remember(context, sized, px) {
        ImageRequest.Builder(context).data(sized).apply {
            if (px != null) size(px.coerceIn(1, 1024))
        }.build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.background(Color(0xFF2B2B35)),
    )
}
