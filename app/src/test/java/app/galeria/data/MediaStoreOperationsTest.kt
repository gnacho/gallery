package app.galeria.data

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaStoreOperationsTest {
    @Test
    fun duplicatesBytesUnderACopyName() {
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        val source = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "sample.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/")
        }))
        resolver.openOutputStream(source)!!.use { it.write(byteArrayOf(1, 2, 3, 4)) }
        val item = MediaItemFactory.from(MediaRecord(id = 1, kind = MediaKind.Image)).copy(uri = source.toString(), displayName = "sample.jpg")

        val copy = MediaStoreOperations.duplicate(resolver, item)

        assertNotEquals(source, copy)
        assertContentEquals(byteArrayOf(1, 2, 3, 4), resolver.openInputStream(copy)!!.use { it.readBytes() })
        resolver.query(copy, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null).use {
            requireNotNull(it).moveToFirst()
            assertEquals("sample-copia.jpg", it.getString(0))
        }
    }

    @Test
    fun restoresRawBytesWithTheOriginalName() {
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        val item = MediaItem(7, "content://old/7", "restored.jpg", "image/jpeg", 123L)

        val restored = MediaStoreOperations.importBytes(resolver, item, byteArrayOf(9, 8, 7))

        assertContentEquals(byteArrayOf(9, 8, 7), resolver.openInputStream(restored)!!.use { it.readBytes() })
        resolver.query(restored, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null).use {
            requireNotNull(it).moveToFirst()
            assertEquals("restored.jpg", it.getString(0))
        }
    }

    @Test
    fun importsATrimmedVideoFromAFile() {
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        val source = File.createTempFile("trim", ".mp4", RuntimeEnvironment.getApplication().cacheDir).apply {
            writeBytes(byteArrayOf(4, 5, 6, 7, 8))
        }
        val item = MediaItem(9, "content://old/9", "holiday.mp4", "video/mp4", 123L)

        val imported = MediaStoreOperations.importFile(resolver, item, source, "holiday-recorte.mp4")

        assertContentEquals(byteArrayOf(4, 5, 6, 7, 8), resolver.openInputStream(imported)!!.use { it.readBytes() })
        resolver.query(imported, arrayOf(MediaStore.Video.Media.DISPLAY_NAME), null, null, null).use {
            requireNotNull(it).moveToFirst()
            assertEquals("holiday-recorte.mp4", it.getString(0))
        }
        source.delete()
    }

    @Test
    fun importsGeneratedMediaWithAnExplicitMimeType() {
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        val source = File.createTempFile("slideshow", ".mp4", RuntimeEnvironment.getApplication().cacheDir).apply {
            writeBytes(byteArrayOf(6, 7, 8))
        }

        val imported = MediaStoreOperations.importFile(resolver, source, "movie.mp4", "video/mp4")

        resolver.query(imported, arrayOf(MediaStore.MediaColumns.MIME_TYPE), null, null, null).use {
            requireNotNull(it).moveToFirst()
            assertEquals("video/mp4", it.getString(0))
        }
        source.delete()
    }

    @Test
    fun copiesAnImageIntoTheChosenAlbum() {
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        val source = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "sample.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Camera/")
        }))
        resolver.openOutputStream(source)!!.use { it.write(byteArrayOf(3, 1, 4)) }
        val item = MediaItem(1, source.toString(), "sample.jpg", "image/jpeg", 123L)

        val copied = MediaStoreOperations.copyToAlbum(resolver, item, AlbumTransferPlan("Viajes"))

        assertNotEquals(source, copied)
        resolver.query(copied, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null, null).use {
            requireNotNull(it).moveToFirst()
            assertEquals("Pictures/Viajes/", it.getString(0))
        }
        assertContentEquals(byteArrayOf(3, 1, 4), resolver.openInputStream(copied)!!.use { it.readBytes() })
    }

    @Test
    fun movesAnExistingImageIntoTheChosenAlbum() {
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        val source = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "sample.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Camera/")
        }))
        val item = MediaItem(1, source.toString(), "sample.jpg", "image/jpeg", 123L)

        val changed = MediaStoreOperations.moveToAlbum(resolver, item, AlbumTransferPlan("Viajes"))

        assertEquals(1, changed)
        resolver.query(source, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null, null).use {
            requireNotNull(it).moveToFirst()
            assertEquals("Pictures/Viajes/", it.getString(0))
        }
    }

    @Test
    fun importsReceivedContentWithoutLosingItsNameOrBytes() {
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        val source = Uri.parse("content://incoming/received.png")
        shadowOf(resolver).registerInputStreamSupplier(source) { ByteArrayInputStream(byteArrayOf(8, 9, 10)) }

        val imported = MediaStoreOperations.importUri(resolver, source, "received.png", "image/png")

        assertContentEquals(byteArrayOf(8, 9, 10), resolver.openInputStream(imported)!!.use { it.readBytes() })
        resolver.query(imported, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null).use {
            requireNotNull(it).moveToFirst()
            assertEquals("received.png", it.getString(0))
        }
    }

    @Test
    fun renamesAnExistingEntry() {
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        val source = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "before.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }))
        val item = MediaItemFactory.from(MediaRecord(id = 1, kind = MediaKind.Image)).copy(uri = source.toString())

        val updated = MediaStoreOperations.rename(resolver, item, "after.jpg")

        assertEquals(1, updated)
        resolver.query(source, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null).use {
            requireNotNull(it).moveToFirst()
            assertEquals("after.jpg", it.getString(0))
        }
    }
}
