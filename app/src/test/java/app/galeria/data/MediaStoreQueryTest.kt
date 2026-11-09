package app.galeria.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaStoreQueryTest {
    @Test
    fun exposesTheCompleteTimelineProjection() {
        val required = listOf(
            "_id",
            "_display_name",
            "mime_type",
            "datetaken",
            "date_added",
            "width",
            "height",
            "_size",
            "bucket_id",
            "bucket_display_name",
            "duration",
        )

        required.forEach { column -> assertTrue(column in MediaStoreQuery.projection, column) }
        assertEquals("datetaken DESC, date_added DESC, _id DESC", MediaStoreQuery.sortOrder)
    }
}
