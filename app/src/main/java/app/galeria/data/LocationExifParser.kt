package app.galeria.data

import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

object LocationExifParser {
    fun parse(input: InputStream): DoubleArray? {
        return runCatching { ExifInterface(input).latLong }.getOrNull()
    }
}
