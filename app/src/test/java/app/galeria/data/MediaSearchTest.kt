package app.galeria.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSearchTest {
    private val items = listOf(
        MediaItem(1, "content://media/1", "Puesta de sol.jpg", "image/jpeg", 1_704_067_200_000, bucketName = "Cámara"),
        MediaItem(2, "content://media/2", "clip.mp4", "video/mp4", 1_704_067_200_000, bucketName = "Playa"),
        MediaItem(3, "content://media/3", "documento.webp", "image/webp", null, bucketName = "Descargas"),
    )

    @Test
    fun matchesLocalNotesWithoutChangingResultOrder() {
        val notes = mapOf(
            "content://media/3" to "Factura del hotel en Córdoba, viaje de trabajo",
            "content://media/1" to "Viaje en familia",
        )
        assertEquals(listOf(3L), MediaSearch.filter(items, "cordoba", notes = notes).map { it.id })
        assertEquals(listOf(3L), MediaSearch.filter(items, "viaje hotel", notes = notes).map { it.id })
        assertEquals(listOf(1L, 3L), MediaSearch.filter(items, "viaje", notes = notes).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), MediaSearch.filter(items, "", notes = notes).map { it.id })
    }

    @Test
    fun normalizesAccentsAndMatchesNamesAlbumsMimeTypesAndYears() {
        assertEquals(listOf(1L), MediaSearch.filter(items, "  PUESTA ").map { it.id })
        assertEquals(listOf(2L), MediaSearch.filter(items, "playa").map { it.id })
        assertEquals(listOf(2L), MediaSearch.filter(items, "video/mp4").map { it.id })
        assertEquals(listOf(1L, 2L), MediaSearch.filter(items, "2024").map { it.id })
        assertEquals(items, MediaSearch.filter(items, "   "))
    }
}
