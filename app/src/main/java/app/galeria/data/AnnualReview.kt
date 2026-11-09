package app.galeria.data

import java.time.Instant
import java.time.ZoneId

data class AnnualReview(
    val year: Int,
    val items: List<MediaItem>,
    val imageCount: Int,
    val videoCount: Int,
    val favoriteCount: Int,
    val monthCount: Int,
)

object AnnualReviewGenerator {
    fun build(items: List<MediaItem>, year: Int, zoneId: ZoneId = ZoneId.systemDefault()): AnnualReview? {
        val selected = items.filter { item ->
            item.dateTakenMillis?.let { Instant.ofEpochMilli(it).atZone(zoneId).year == year } == true
        }
        if (selected.isEmpty()) return null
        return AnnualReview(
            year = year,
            items = selected,
            imageCount = selected.count { it.mimeType.startsWith("image/") },
            videoCount = selected.count { it.mimeType.startsWith("video/") },
            favoriteCount = selected.count { it.isFavorite },
            monthCount = selected.mapNotNull { item ->
                item.dateTakenMillis?.let { Instant.ofEpochMilli(it).atZone(zoneId).monthValue }
            }.distinct().size,
        )
    }

    fun availableYears(items: List<MediaItem>, zoneId: ZoneId = ZoneId.systemDefault()): List<Int> = items
        .mapNotNull { item -> item.dateTakenMillis?.let { Instant.ofEpochMilli(it).atZone(zoneId).year } }
        .distinct()
        .sortedDescending()
}
