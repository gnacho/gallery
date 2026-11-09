package app.galeria.data

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryGrouperTest {
    @Test
    fun findsPreviousYearsOnTheSameMonthAndDay() {
        val zone = ZoneId.of("Europe/Madrid")
        val items = listOf(
            MediaItem(1, "content://media/1", "hoy.jpg", "image/jpeg", at("2026-10-31T10:00:00Z"), 1, 1, 1, 1, "Camera"),
            MediaItem(2, "content://media/2", "antes.jpg", "image/jpeg", at("2025-10-31T10:00:00Z"), 1, 1, 1, 1, "Camera"),
            MediaItem(3, "content://media/3", "otro.jpg", "image/jpeg", at("2025-10-30T10:00:00Z"), 1, 1, 1, 1, "Camera"),
            MediaItem(4, "content://media/4", "viejo.jpg", "image/jpeg", at("2024-10-31T10:00:00Z"), 1, 1, 1, 1, "Camera"),
            MediaItem(5, "content://media/5", "sin-fecha.jpg", "image/jpeg", null, 1, 1, 1, 1, "Camera"),
        )

        val memories = MemoryGrouper.group(items, LocalDate.of(2026, 10, 31), zone)

        assertEquals(listOf(2025, 2024), memories.map { it.year })
        assertEquals(listOf(2L), memories[0].items.map { it.id })
        assertEquals(listOf(4L), memories[1].items.map { it.id })
    }

    private fun at(value: String): Long = java.time.Instant.parse(value).toEpochMilli()
}
