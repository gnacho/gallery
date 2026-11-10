package app.galeria.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UltraHdrExportInstrumentedTest {

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun exportedJpegCarriesAGainMap() {
        val bitmap = syntheticHighlightBitmap(64, 64)

        val exported = UltraHdrExport.encode(bitmap)

        val decoded = BitmapFactory.decodeByteArray(exported, 0, exported.size)
        assertNotNull("decoded bitmap must not be null", decoded)
        assertNotNull("decoded bitmap must carry a gain map (UltraHDR)", decoded!!.gainmap)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun gainMapHasExpectedResolution() {
        val bitmap = syntheticHighlightBitmap(128, 64)

        val exported = UltraHdrExport.encode(bitmap)

        val decoded = BitmapFactory.decodeByteArray(exported, 0, exported.size)
        val gainmap = decoded?.gainmap
        assertNotNull(gainmap)
        // gain map is 1/4 of each dimension
        assertEquals(128 / 4, gainmap!!.gainmapContents.width)
        assertEquals(64 / 4, gainmap.gainmapContents.height)
    }

    private fun syntheticHighlightBitmap(width: Int, height: Int): Bitmap {
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val v = if (y < height / 2) 255 else 0
                pixels[y * width + x] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}
