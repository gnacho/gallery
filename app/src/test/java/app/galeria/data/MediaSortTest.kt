package app.galeria.data

import kotlin.test.Test
import kotlin.test.assertEquals

class MediaSortTest {
    private fun media(id: Long, name: String, date: Long?) = MediaItem(
        id = id,
        uri = "content://media/$id",
        displayName = name,
        mimeType = "image/jpeg",
        dateTakenMillis = date,
    )

    private val items = listOf(
        media(1, "b.jpg", 2000L),
        media(2, "a.jpg", 3000L),
        media(3, "c.jpg", 1000L),
        media(4, "d.jpg", null),
    )

    @Test
    fun dateDescPutsNewestFirstAndUndatedLast() {
        assertEquals(listOf(2L, 1L, 3L, 4L), sortMediaItems(items, SortOrder.DATE_DESC).map { it.id })
    }

    @Test
    fun dateAscPutsOldestFirstAndUndatedLast() {
        assertEquals(listOf(3L, 1L, 2L, 4L), sortMediaItems(items, SortOrder.DATE_ASC).map { it.id })
    }

    @Test
    fun nameAscSortsByDisplayName() {
        assertEquals(listOf(2L, 1L, 3L, 4L), sortMediaItems(items, SortOrder.NAME_ASC).map { it.id })
    }
}
