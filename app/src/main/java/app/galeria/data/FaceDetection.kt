package app.galeria.data

import android.graphics.Bitmap
import android.media.FaceDetector

object FaceDetection {
    fun containsFace(source: Bitmap): Boolean {
        val width = (source.width - source.width % 2).coerceAtLeast(2)
        val height = source.height.coerceAtLeast(2)
        val converted = if (source.width == width && source.height == height && source.config == Bitmap.Config.RGB_565) {
            source
        } else {
            Bitmap.createScaledBitmap(source, width, height, true).copy(Bitmap.Config.RGB_565, false)
        }
        return try {
            val faces = arrayOfNulls<FaceDetector.Face>(4)
            FaceDetector(width, height, 4).findFaces(converted, faces) > 0
        } catch (_: RuntimeException) {
            false
        } finally {
            if (converted !== source) converted.recycle()
        }
    }
}
