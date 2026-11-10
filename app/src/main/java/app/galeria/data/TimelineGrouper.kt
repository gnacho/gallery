package app.galeria.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object TimelineGrouper {
    fun group(items: List<MediaItem>, zoneId: ZoneId): List<TimelineSection> {
        return items
            .groupBy { item ->
                item.dateTakenMillis?.let { timestamp ->
                    Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
                }
            }
            .map { (date: LocalDate?, dayItems: List<MediaItem>) -> TimelineSection(date, dayItems) }
    }
}
