package app.galeria.data

import kotlin.test.Test
import kotlin.test.assertEquals

class AlbumGrouperTest {
    @Test
    fun groupsByBucketAndUsesTheNewestItemAsCover() {
        val older = MediaItem(
            id = 1,
            uri = "content://image/1",
            displayName = "old.jpg",
            mimeType = "image/jpeg",
            dateTakenMillis = 1_000,
            bucketId = 10,
            bucketName = "Camera",
        )
        val newer = older.copy(
            id = 2,
            uri = "content://image/2",
            displayName = "new.jpg",
            dateTakenMillis = 2_000,
        )
        val screenshots = older.copy(
            id = 3,
            uri = "content://image/3",
            bucketId = 20,
            bucketName = "Screenshots",
            dateTakenMillis = 1_500,
        )

        val albums = AlbumGrouper.group(listOf(older, screenshots, newer))

        assertEquals(listOf("Camera", "Screenshots"), albums.map { it.name })
        assertEquals(2, albums.first().count)
        assertEquals(newer, albums.first().cover)
        assertEquals(listOf(newer, older), albums.first().items)
    }
}
