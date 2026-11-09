package app.galeria.data

import android.graphics.Bitmap
import android.provider.MediaStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaStoreSaverTest {
    @Test
    fun savesAndPublishedAJpeg() {
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

        val uri = MediaStoreSaver.saveBitmap(resolver, bitmap, "collage-test.jpg")

        assertNotNull(uri)
        resolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.IS_PENDING), null, null, null).use { cursor ->
            assertNotNull(cursor)
            requireNotNull(cursor).moveToFirst()
            assertEquals("collage-test.jpg", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
        }
    }
}
