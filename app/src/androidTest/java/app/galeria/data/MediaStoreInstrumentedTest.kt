package app.galeria.data

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaStoreInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver = context.contentResolver
    private val created = mutableListOf<Uri>()

    @After
    fun cleanUpSyntheticMedia() {
        created.forEach { resolver.delete(it, null, null) }
        created.clear()
    }

    @Test
    fun mediaStoreRoundTripPreservesRealJpegAndMetadata() {
        val bytes = SyntheticMedia.jpegBytes()
        val (sourceUri, sourceItem) = SyntheticMedia.importImage(resolver, bytes)
        created += sourceUri

        assertContentEquals(bytes, resolver.openInputStream(sourceUri)!!.use { it.readBytes() })
        val decoded = BitmapFactory.decodeStream(resolver.openInputStream(sourceUri))
        assertNotNull(decoded)
        assertEquals(96, decoded.width)
        assertEquals(72, decoded.height)

        val loaded = MediaStoreRepository(resolver).load().single { it.uri == sourceUri.toString() }
        assertEquals(sourceItem.displayName, loaded.displayName)
        assertEquals(96, loaded.width)
        assertEquals(72, loaded.height)
        assertEquals("Galeria", loaded.bucketName)

        val duplicate = MediaStoreOperations.duplicate(resolver, loaded)
        created += duplicate
        assertNotEquals(sourceUri, duplicate)
        assertContentEquals(bytes, resolver.openInputStream(duplicate)!!.use { it.readBytes() })

        assertEquals(1, MediaStoreOperations.rename(resolver, loaded.copy(uri = duplicate.toString()), SyntheticMedia.uniqueName("jpg")))
        val copy = MediaStoreOperations.copyToAlbum(resolver, loaded, AlbumTransferPlan("AndroidCopia"))
        created += copy
        assertEquals("Pictures/AndroidCopia/", copy.singleColumn(MediaStore.MediaColumns.RELATIVE_PATH))
        assertContentEquals(bytes, resolver.openInputStream(copy)!!.use { it.readBytes() })

        assertEquals(1, MediaStoreOperations.moveToAlbum(resolver, loaded, AlbumTransferPlan("AndroidMovida")))
        assertEquals("Pictures/AndroidMovida/", sourceUri.singleColumn(MediaStore.MediaColumns.RELATIVE_PATH))
    }

    @Test
    fun editedJpegIsSavedWithRotatedDimensionsAndReadablePixels() {
        val (sourceUri, sourceItem) = SyntheticMedia.importImage(resolver)
        created += sourceUri

        val edited = ImageEditorRenderer.save(resolver, sourceItem, ImageEditState(rotationDegrees = 90))
        created += edited

        val bitmap = BitmapFactory.decodeStream(resolver.openInputStream(edited))
        assertNotNull(bitmap)
        assertEquals(72, bitmap.width)
        assertEquals(96, bitmap.height)
        assertTrue(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2) != 0)
    }

    private fun Uri.singleColumn(column: String): String {
        val cursor = requireNotNull(resolver.query(this, arrayOf(column), null, null, null))
        return cursor.use {
            assertTrue(it.moveToFirst())
            it.getString(0)
        }
    }
}
