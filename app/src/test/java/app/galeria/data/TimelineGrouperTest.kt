package app.galeria.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class TimelineGrouperTest {
    @Test
    fun groupsByLocalDayPreservingInputOrder() {
        val items = listOf(
            media(1, "2024-05-01T23:30:00Z"), // 05-02 local
            media(2, "2024-05-02T00:30:00Z"), // 05-02 local
            media(3, "2024-05-01T10:00:00Z"), // 05-01 local
        )

        val sections = TimelineGrouper.group(items, ZoneId.of("Europe/Madrid"))

        assertEquals(listOf(LocalDate.of(2024, 5, 2), LocalDate.of(2024, 5, 1)), sections.map { it.date })
        assertEquals(listOf(1L, 2L), sections[0].items.map { it.id })
        assertEquals(listOf(3L), sections[1].items.map { it.id })
    }

    @Test
    fun groupsItemsWithoutCaptureDateInTheirOwnSection() {
        val items = listOf(
            media(4, null),
            media(2, "2024-05-02T00:30:00Z"),
            media(3, null),
        )

        val sections = TimelineGrouper.group(items, ZoneId.of("UTC"))

        assertEquals(listOf(null, LocalDate.of(2024, 5, 2)), sections.map { it.date })
        assertEquals(listOf(4L, 3L), sections[0].items.map { it.id })
    }

    private fun media(id: Long, timestamp: String?) = MediaItem(
        id = id,
        uri = "content://media/$id",
        displayName = "$id.jpg",
        mimeType = "image/jpeg",
        dateTakenMillis = timestamp?.let { Instant.parse(it).toEpochMilli() },
    )
}
