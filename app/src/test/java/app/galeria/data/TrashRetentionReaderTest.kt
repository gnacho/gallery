package app.galeria.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TrashRetentionReaderTest {
    @Test
    fun readsMediaStoreExpiryAndCalculatesWholeDaysRemaining() {
        val now = 1_700_000_000_000L
        val expirySeconds = (now + 2L * 86_400_000L) / 1000L
        val provider = object : ContentProvider() {
            override fun onCreate() = true
            override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor =
                MatrixCursor(arrayOf("date_expires")).apply { addRow(arrayOf(expirySeconds)) }
            override fun getType(uri: Uri) = "image/jpeg"
            override fun insert(uri: Uri, values: ContentValues?): Uri? = null
            override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0
            override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?) = 0
        }
        ShadowContentResolver.registerProviderInternal("media", provider)

        val retention = TrashRetentionReader.read(
            RuntimeEnvironment.getApplication().contentResolver,
            Uri.parse("content://media/7"),
            now,
        )

        assertEquals(2, retention?.remainingDays)
        assertFalse(retention!!.expired)
    }
}
