package com.music.bitchord.playback

import com.music.bitchord.data.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals

class QueueBuilderTest {
    private fun song(id: String, title: String = id, artist: String = id) = Song(
        videoId = id,
        title = title,
        artist = artist,
        thumbnailUrl = null,
    )

    @Test
    fun mixedExtensionKeepsARecommendationSpine() {
        val mixed = QueueBuilder.extendMixed(
            existing = emptyList(),
            batches = listOf(
                QueueBuilder.CandidateBatch(
                    "youtube",
                    listOf(song("yt-1"), song("yt-2"), song("yt-3")),
                ),
                QueueBuilder.CandidateBatch("audius", listOf(song("au-1"), song("au-2"))),
                QueueBuilder.CandidateBatch("jiosaavn", listOf(song("jio-1"))),
            ),
            limit = 6,
            primarySourceId = "youtube",
        )

        assertEquals(
            listOf("yt-1", "yt-2", "au-1", "yt-3", "au-2", "jio-1"),
            mixed.map(Song::videoId),
        )
    }

    @Test
    fun mixedExtensionDeduplicatesRecordingsAcrossSources() {
        val mixed = QueueBuilder.extendMixed(
            existing = listOf(song("existing", title = "Same Song", artist = "Artist")),
            batches = listOf(
                QueueBuilder.CandidateBatch(
                    "audius",
                    listOf(song("audius-copy", title = "Same Song", artist = "Artist")),
                ),
                QueueBuilder.CandidateBatch("soundcloud", listOf(song("different", title = "Different"))),
            ),
            limit = 5,
        )

        assertEquals(listOf("different"), mixed.map(Song::videoId))
    }
}
