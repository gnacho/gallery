package app.galeria.data

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

/**
 * Exports an SDR bitmap as an UltraHDR JPEG: generates the gain map, compresses
 * the base and gain map images, and assembles the container via [UltraHdrEncoder].
 */
object UltraHdrExport {

    private const val JPEG_QUALITY = 90

    fun encode(bitmap: Bitmap, maxBoost: Float = 3f): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val gainMap = GainMapGenerator.generate(pixels, width, height, maxBoost)

        val baseJpeg = compressToJpeg(bitmap)

        val gainMapBitmap = toGrayBitmap(gainMap.encodeGainMap8Bit(), gainMap.width, gainMap.height)
        val gainMapJpeg = compressToJpeg(gainMapBitmap)

        return UltraHdrEncoder.encode(baseJpeg, gainMapJpeg, gainMap.metadata)
    }

    private fun compressToJpeg(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) { "JPEG compression failed" }
        return out.toByteArray()
    }

    private fun toGrayBitmap(gray: IntArray, width: Int, height: Int): Bitmap {
        val pixels = IntArray(width * height)
        for (i in gray.indices) {
            val v = gray[i].coerceIn(0, 255)
            pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}
