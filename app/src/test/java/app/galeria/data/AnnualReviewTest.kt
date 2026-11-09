package app.galeria.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class AnnualReviewTest {
    @Test
    fun summarizesAYearAndListsAvailableYears() {
        val zone = ZoneId.of("Europe/Madrid")
        val items = listOf(
            MediaItem(1, "content://media/1", "uno.jpg", "image/jpeg", 1_704_067_200_000, isFavorite = true),
            MediaItem(2, "content://media/2", "dos.mp4", "video/mp4", 1_709_452_800_000),
            MediaItem(3, "content://media/3", "tres.jpg", "image/jpeg", 1_672_531_200_000),
        )
        val review = AnnualReviewGenerator.build(items, 2024, zone)
        assertEquals(2024, review?.year)
        assertEquals(listOf(1L, 2L), review?.items?.map { it.id })
        assertEquals(1, review?.imageCount)
        assertEquals(1, review?.videoCount)
        assertEquals(1, review?.favoriteCount)
        assertEquals(2, review?.monthCount)
        assertEquals(listOf(2024, 2023), AnnualReviewGenerator.availableYears(items, zone))
    }
}
