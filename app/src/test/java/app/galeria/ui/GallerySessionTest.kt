package app.galeria.ui

import app.galeria.data.MediaItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GallerySessionTest {
    @Test
    fun loadedMediaBuildsTimelineAndViewerKeepsSelectedItemUntilClosed() {
        val item = MediaItem(
            id = 7,
            uri = "content://media/external/images/media/7",
            displayName = "foto.jpg",
            mimeType = "image/jpeg",
            dateTakenMillis = Instant.parse("2026-10-30T10:00:00Z").toEpochMilli(),
            width = 100,
            height = 100,
            sizeBytes = 1000,
            bucketId = 1,
            bucketName = "Camera",
        )
        val zone = ZoneId.of("Europe/Madrid")

        val loaded = GalleryNavigation.mediaLoaded(GalleryNavigation.initial(), listOf(item), zone)
        val opened = GalleryNavigation.openViewer(loaded, item)

        assertEquals(LocalDate.of(2026, 10, 30), loaded.sections.single().date)
        assertEquals(item, opened.viewerItem)
        assertNull(GalleryNavigation.closeViewer(opened).viewerItem)
    }

    @Test
    fun deniedMediaPermissionHasAnExplicitRecoverableState() {
        val denied = GalleryNavigation.permissionDenied(GalleryNavigation.initial())

        assertTrue(denied.permissionDenied)
    }
}
