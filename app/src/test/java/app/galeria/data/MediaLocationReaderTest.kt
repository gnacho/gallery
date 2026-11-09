package app.galeria.data

import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaLocationReaderTest {
    @Test
    fun readsOnlyImagesWithExifCoordinates() {
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        val image = MediaItem(1, "content://media/images/1", "located.jpg", "image/jpeg", 1)
        val video = image.copy(id = 2, uri = "content://media/video/2", mimeType = "video/mp4")
        val withoutLocation = image.copy(id = 3, uri = "content://media/images/3")
        val bytes = jpegWithLocation()
        Shadows.shadowOf(resolver).registerInputStreamSupplier(Uri.parse(image.uri)) {
            ByteArrayInputStream(bytes)
        }

        val result = MediaLocationReader(resolver).read(listOf(image, video, withoutLocation))

        assertEquals(1, result.size)
        assertEquals(image, result[0].item)
        assertEquals(40.4168, result[0].latitude, 0.000001)
        assertEquals(-3.7038, result[0].longitude, 0.000001)
    }

    private fun jpegWithLocation(): ByteArray {
        val file = File.createTempFile("reader-location", ".jpg")
        return try {
            file.outputStream().use { output ->
                Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 100, output)
            }
            ExifInterface(file).apply {
                setLatLong(40.4168, -3.7038)
                saveAttributes()
            }
            file.readBytes()
        } finally {
            file.delete()
        }
    }
}
