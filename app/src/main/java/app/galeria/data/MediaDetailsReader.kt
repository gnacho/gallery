package app.galeria.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.exifinterface.media.ExifInterface

data class DetailEntry(val label: String, val value: String)

data class MediaDetails(
    val item: MediaItem,
    val entries: List<DetailEntry>,
    val histogram: IntArray,
    val latitude: Double?,
    val longitude: Double?,
)

object MediaDetailsReader {
    fun read(resolver: ContentResolver, item: MediaItem): MediaDetails {
        val entries = mutableListOf<DetailEntry>()
        var latitude: Double? = null
        var longitude: Double? = null
        if (item.mimeType.startsWith("image/")) {
            resolver.openInputStream(Uri.parse(item.uri))?.use { input ->
                val exif = ExifInterface(input)
                val tags = listOf(
                    "Fecha original" to ExifInterface.TAG_DATETIME_ORIGINAL,
                    "Fabricante" to ExifInterface.TAG_MAKE,
                    "Modelo" to ExifInterface.TAG_MODEL,
                    "ISO" to ExifInterface.TAG_ISO_SPEED,
                    "Exposición" to ExifInterface.TAG_EXPOSURE_TIME,
                    "Apertura" to ExifInterface.TAG_F_NUMBER,
                    "Distancia focal" to ExifInterface.TAG_FOCAL_LENGTH,
                    "Balance de blancos" to ExifInterface.TAG_WHITE_BALANCE,
                    "Flash" to ExifInterface.TAG_FLASH,
                    "Software" to ExifInterface.TAG_SOFTWARE,
                )
                tags.forEach { (label, tag) ->
                    exif.getAttribute(tag)?.takeIf { it.isNotBlank() }?.let { entries += DetailEntry(label, it) }
                }
                exif.latLong?.let { coordinates ->
                    if (coordinates.size == 2) {
                        latitude = coordinates[0]
                        longitude = coordinates[1]
                    }
                }
            }
        }
        val histogram = if (item.mimeType.startsWith("image/")) {
            val thumbnail = resolver.loadThumbnail(Uri.parse(item.uri), Size(256, 256), null)
            histogram(thumbnail)
        } else {
            IntArray(256)
        }
        return MediaDetails(item, entries, histogram, latitude, longitude)
    }

    private fun histogram(bitmap: Bitmap): IntArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return HistogramCalculator.luminance(pixels)
    }
}
