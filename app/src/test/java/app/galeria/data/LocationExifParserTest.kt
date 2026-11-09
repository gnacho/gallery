package app.galeria.data

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocationExifParserTest {
    @Test
    fun readsCoordinatesWrittenToAJpeg() {
        val file = File.createTempFile("location", ".jpg")
        try {
            file.outputStream().use { output ->
                Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 100, output)
            }
            ExifInterface(file).apply {
                setLatLong(40.4168, -3.7038)
                saveAttributes()
            }

            val result = LocationExifParser.parse(ByteArrayInputStream(file.readBytes()))

            assertArrayEquals(doubleArrayOf(40.4168, -3.7038), result, 0.000001)
        } finally {
            file.delete()
        }
    }
}
