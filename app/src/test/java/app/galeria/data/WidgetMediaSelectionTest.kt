package app.galeria.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetMediaSelectionTest {
    @Test
    fun choosesTheNewestStillImageAndNeverAVideo() {
        val olderImage = MediaItem(1, "content://image/1", "old.jpg", "image/jpeg", 1000L, 0, 0, 0, 0, null, null)
        val newerVideo = MediaItem(2, "content://video/2", "new.mp4", "video/mp4", 3000L, 0, 0, 0, 0, null, null)
        val newerImage = MediaItem(3, "content://image/3", "new.jpg", "image/jpeg", 2000L, 0, 0, 0, 0, null, null)

        assertEquals(newerImage, WidgetMediaSelection.latestImage(listOf(olderImage, newerVideo, newerImage)))
        assertNull(WidgetMediaSelection.latestImage(listOf(newerVideo)))
    }
}
