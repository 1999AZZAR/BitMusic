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
    fun rankedExtensionDoesNotForceWeakSources() {
        val mixed = QueueBuilder.extendRanked(
            existing = emptyList(),
            candidates = listOf(
                QueueBuilder.RankedCandidate("audius", song("weak"), 39),
                QueueBuilder.RankedCandidate("jiosaavn", song("good"), 55),
                QueueBuilder.RankedCandidate("youtube", song("best"), 90),
            ),
            limit = 5,
            minimumScore = 40,
        )

        assertEquals(listOf("best", "good"), mixed.map(Song::videoId))
    }

    @Test
    fun mixedExtensionDeduplicatesRecordingsAcrossSources() {
        val mixed = QueueBuilder.extendRanked(
            existing = listOf(song("existing", title = "Same Song", artist = "Artist")),
            candidates = listOf(
                QueueBuilder.RankedCandidate(
                    "audius",
                    song("audius-copy", title = "Same Song", artist = "Artist"),
                    100,
                ),
                QueueBuilder.RankedCandidate(
                    "soundcloud",
                    song("different", title = "Different"),
                    50,
                ),
            ),
            limit = 5,
            minimumScore = 40,
        )

        assertEquals(listOf("different"), mixed.map(Song::videoId))
    }
}
