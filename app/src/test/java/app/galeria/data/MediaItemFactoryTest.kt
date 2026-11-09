package app.galeria.data

import kotlin.test.Test
import kotlin.test.assertEquals

class MediaItemFactoryTest {
    @Test
    fun fallsBackToDateAddedWhenCaptureDateIsMissing() {
        val item = MediaItemFactory.from(
            MediaRecord(
                id = 7,
                kind = MediaKind.Image,
                dateTakenMillis = null,
                dateAddedSeconds = 123,
            ),
        )

        assertEquals(123_000L, item.dateTakenMillis)
    }

    @Test
    fun usesTheVideoCollectionForVideos() {
        val item = MediaItemFactory.from(
            MediaRecord(id = 8, kind = MediaKind.Video, mimeType = "video/mp4"),
        )

        assertEquals("content://media/external/video/media/8", item.uri)
    }

    @Test
    fun keepsFavoriteAndTrashState() {
        val item = MediaItemFactory.from(
            MediaRecord(id = 9, kind = MediaKind.Image, isFavorite = true, isTrashed = true),
        )

        assertEquals(true, item.isFavorite)
        assertEquals(true, item.isTrashed)
    }
}
