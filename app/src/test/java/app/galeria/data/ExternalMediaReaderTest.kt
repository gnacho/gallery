package app.galeria.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExternalMediaReaderTest {
    @Test
    fun preservesTheExternalUriAndReadsItsMetadata() {
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        val uri = Uri.parse("content://external.documents/document/photo")
        ShadowContentResolver.registerProviderInternal("external.documents", ExternalProvider())

        val item = ExternalMediaReader.read(resolver, uri, "image/png")

        assertEquals(uri.toString(), item?.uri)
        assertEquals("shared.png", item?.displayName)
        assertEquals("image/png", item?.mimeType)
        assertEquals(640, item?.width)
    }

    private class ExternalProvider : ContentProvider() {
        override fun onCreate() = true
        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor = MatrixCursor(MediaStoreQuery.projection).apply {
            addRow(
                MediaStoreQuery.projection.map<String, Any?> { column ->
                    when (column) {
                        "_id" -> 42L
                        "_display_name" -> "shared.png"
                        "mime_type" -> "image/png"
                        "datetaken" -> 123000L
                        "width" -> 640
                        "height" -> 480
                        else -> null
                    }
                }.toTypedArray(),
            )
        }
        override fun getType(uri: Uri) = "image/png"
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    }
}
