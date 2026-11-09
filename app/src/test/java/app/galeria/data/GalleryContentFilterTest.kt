package app.galeria.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryContentFilterTest {
    @Test
    fun hidingVideosOnlyChangesTheTimelineView() {
        val image = item(1, "image/jpeg")
        val video = item(2, "video/mp4")

        val views = GalleryContentFilter.views(listOf(image, video), showVideos = false)

        assertEquals(listOf(image, video), views.library)
        assertEquals(listOf(image), views.timeline)
        assertEquals(listOf(image, video), GalleryContentFilter.views(listOf(image, video), true).timeline)
    }

    private fun item(id: Long, mimeType: String) = MediaItem(
        id = id,
        uri = "content://media/$id",
        displayName = "$id",
        mimeType = mimeType,
        dateTakenMillis = id,
        width = 1,
        height = 1,
        sizeBytes = 1,
        bucketId = 1,
        bucketName = "Camera",
        durationMillis = null,
    )
}
