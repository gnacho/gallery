package app.galeria.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class MemorySection(val year: Int, val items: List<MediaItem>)

object MemoryGrouper {
    fun group(items: List<MediaItem>, today: LocalDate, zoneId: ZoneId): List<MemorySection> = items
        .mapNotNull { item ->
            val timestamp = item.dateTakenMillis ?: return@mapNotNull null
            val date = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
            item.takeIf {
                date.year < today.year && date.month == today.month && date.dayOfMonth == today.dayOfMonth
            }?.let { it to date.year }
        }
        .groupBy { (_, year) -> year }
        .map { (year, pairs) -> MemorySection(year, pairs.map { (item) -> item }) }
        .sortedByDescending { it.year }
}
