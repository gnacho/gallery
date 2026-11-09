package app.galeria.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.util.Size
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CollageRenderer {
    fun render(bitmaps: List<Bitmap>): Bitmap {
        val layout = CollageLayout.forCount(bitmaps.size)
        val output = Bitmap.createBitmap(1600, 1600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.BLACK)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        layout.cells.forEach { cell ->
            val source = bitmaps[cell.index]
            val destination = Rect(
                (cell.left * output.width).toInt(),
                (cell.top * output.height).toInt(),
                ((cell.left + cell.width) * output.width).toInt(),
                ((cell.top + cell.height) * output.height).toInt(),
            )
            val destinationRatio = destination.width().toFloat() / destination.height()
            val sourceRatio = source.width.toFloat() / source.height
            val sourceRect = if (sourceRatio > destinationRatio) {
                val width = (source.height * destinationRatio).toInt()
                val left = (source.width - width) / 2
                Rect(left, 0, left + width, source.height)
            } else {
                val height = (source.width / destinationRatio).toInt()
                val top = (source.height - height) / 2
                Rect(0, top, source.width, top + height)
            }
            canvas.drawBitmap(source, sourceRect, destination, paint)
        }
        return output
    }

    fun create(resolver: ContentResolver, items: List<MediaItem>): Uri {
        val images = items.filter { it.mimeType.startsWith("image/") }
        require(images.size in 2..9) { "Select between 2 and 9 images" }
        val bitmaps = images.map { item ->
            resolver.loadThumbnail(Uri.parse(item.uri), Size(1024, 1024), null)
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
        return MediaStoreSaver.saveBitmap(resolver, render(bitmaps), "Collage_$timestamp.jpg")
    }
}
