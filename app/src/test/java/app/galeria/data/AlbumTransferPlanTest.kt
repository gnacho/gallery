package app.galeria.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumTransferPlanTest {
    @Test
    fun buildsSafeRelativePathsForImagesAndVideos() {
        val plan = AlbumTransferPlan("  Viajes  ")
        val image = MediaItem(1, "content://image/1", "a.jpg", "image/jpeg", 1L)
        val video = image.copy(mimeType = "video/mp4")

        assertEquals("Pictures/Viajes/", plan.relativePathFor(image))
        assertEquals("Movies/Viajes/", plan.relativePathFor(video))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPathSeparatorsInAlbumNames() {
        AlbumTransferPlan("../private")
    }
}
