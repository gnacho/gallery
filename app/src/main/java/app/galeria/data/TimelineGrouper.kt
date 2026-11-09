package app.galeria.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object TimelineGrouper {
    fun group(items: List<MediaItem>, zoneId: ZoneId): List<TimelineSection> {
        return items
            .sortedWith(compareByDescending<MediaItem> { it.dateTakenMillis }.thenByDescending { it.id })
            .groupBy { item ->
                item.dateTakenMillis?.let { timestamp ->
                    Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
                }
            }
            .map { (date: LocalDate?, dayItems: List<MediaItem>) -> TimelineSection(date, dayItems) }
            .sortedWith(compareBy(nullsLast(reverseOrder<LocalDate>())) { it.date })
    }
}
