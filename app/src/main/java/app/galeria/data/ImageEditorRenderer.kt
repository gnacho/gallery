package app.galeria.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.util.Size
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageEditorRenderer {
    fun render(source: Bitmap, state: ImageEditState): Bitmap {
        val matrix = Matrix().apply {
            postRotate(state.rotationDegrees.toFloat())
            if (state.flipHorizontal) postScale(-1f, 1f)
        }
        val transformed = if (state.rotationDegrees == 0 && !state.flipHorizontal) {
            source
        } else {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }
        val cropped = state.crop?.let { crop ->
            val left = (crop.left * transformed.width).toInt()
            val top = (crop.top * transformed.height).toInt()
            val right = (crop.right * transformed.width).toInt().coerceAtMost(transformed.width)
            val bottom = (crop.bottom * transformed.height).toInt().coerceAtMost(transformed.height)
            Bitmap.createBitmap(transformed, left, top, right - left, bottom - top)
        } ?: transformed
        val output = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(colorMatrix(state)) }
        val canvas = Canvas(output)
        canvas.drawBitmap(cropped, 0f, 0f, paint)
        state.mosaics.forEach { mosaic ->
            val left = (mosaic.region.left * output.width).toInt()
            val top = (mosaic.region.top * output.height).toInt()
            val right = (mosaic.region.right * output.width).toInt().coerceAtMost(output.width)
            val bottom = (mosaic.region.bottom * output.height).toInt().coerceAtMost(output.height)
            var blockTop = top
            while (blockTop < bottom) {
                val blockBottom = (blockTop + mosaic.blockSize).coerceAtMost(bottom)
                var blockLeft = left
                while (blockLeft < right) {
                    val blockRight = (blockLeft + mosaic.blockSize).coerceAtMost(right)
                    var red = 0L
                    var green = 0L
                    var blue = 0L
                    var alpha = 0L
                    for (y in blockTop until blockBottom) {
                        for (x in blockLeft until blockRight) {
                            val color = output.getPixel(x, y)
                            red += Color.red(color)
                            green += Color.green(color)
                            blue += Color.blue(color)
                            alpha += Color.alpha(color)
                        }
                    }
                    val count = ((blockRight - blockLeft) * (blockBottom - blockTop)).toLong()
                    val average = Color.argb(
                        (alpha / count).toInt(),
                        (red / count).toInt(),
                        (green / count).toInt(),
                        (blue / count).toInt(),
                    )
                    for (y in blockTop until blockBottom) {
                        for (x in blockLeft until blockRight) output.setPixel(x, y, average)
                    }
                    blockLeft = blockRight
                }
                blockTop = blockBottom
            }
        }
        state.texts.forEach { overlay ->
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = overlay.color
                textSize = overlay.sizeFraction * output.height
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            canvas.drawText(overlay.text, overlay.x * output.width, overlay.y * output.height, textPaint)
        }
        state.drawings.forEach { stroke ->
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stroke.color
                style = Paint.Style.STROKE
                strokeWidth = stroke.widthFraction * minOf(output.width, output.height)
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            if (stroke.points.size == 1) {
                val point = stroke.points.single()
                canvas.drawCircle(point.x * output.width, point.y * output.height, strokePaint.strokeWidth / 2f, strokePaint)
            } else {
                val path = Path()
                stroke.points.forEachIndexed { index, point ->
                    val x = point.x * output.width
                    val y = point.y * output.height
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                canvas.drawPath(path, strokePaint)
            }
        }
        state.border?.let { border ->
            val borderPaint = Paint().apply { color = border.color }
            val thickness = (border.widthFraction * minOf(output.width, output.height)).toInt().coerceAtLeast(1)
            canvas.drawRect(0f, 0f, output.width.toFloat(), thickness.toFloat(), borderPaint)
            canvas.drawRect(0f, (output.height - thickness).toFloat(), output.width.toFloat(), output.height.toFloat(), borderPaint)
            canvas.drawRect(0f, 0f, thickness.toFloat(), output.height.toFloat(), borderPaint)
            canvas.drawRect((output.width - thickness).toFloat(), 0f, output.width.toFloat(), output.height.toFloat(), borderPaint)
        }
        state.watermark?.let { watermark ->
            val margin = output.height * 0.03f
            val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = output.height * 0.06f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setShadowLayer(2f, 1f, 1f, Color.BLACK)
            }
            if (watermark.lunar) {
                val radius = output.height * 0.06f
                val centerX = margin + radius
                val centerY = output.height - margin - radius
                val outer = Path().apply { addCircle(centerX, centerY, radius, Path.Direction.CW) }
                val cutout = Path().apply {
                    addCircle(centerX + radius * 0.45f, centerY - radius * 0.15f, radius * 0.85f, Path.Direction.CW)
                }
                val crescent = Path()
                crescent.op(outer, cutout, Path.Op.DIFFERENCE)
                canvas.drawPath(crescent, watermarkPaint)
            }
            canvas.drawText(
                watermark.text,
                output.width - watermarkPaint.measureText(watermark.text) - margin,
                output.height - margin,
                watermarkPaint,
            )
        }
        return output
    }

    fun save(resolver: ContentResolver, item: MediaItem, state: ImageEditState): Uri {
        require(item.mimeType.startsWith("image/")) { "Only still images can be edited" }
        val source = resolver.loadThumbnail(Uri.parse(item.uri), Size(2048, 2048), null)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
        return MediaStoreSaver.saveBitmap(resolver, render(source, state), "Edicion_$timestamp.jpg")
    }

    private fun colorMatrix(state: ImageEditState): ColorMatrix {
        val filter = when (state.filter) {
            ImageFilter.ORIGINAL -> ColorMatrix()
            ImageFilter.MONO -> ColorMatrix().apply { setSaturation(0f) }
            ImageFilter.VIVID -> ColorMatrix().apply { setSaturation(1.4f) }
            ImageFilter.SEPIA -> ColorMatrix(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
        }
        val filterValues = filter.array
        val intensity = state.filterIntensity
        val weightedFilter = ColorMatrix(FloatArray(20) { index ->
            val identity = if (index % 6 == 0) 1f else 0f
            identity * (1f - intensity) + filterValues[index] * intensity
        })
        weightedFilter.postConcat(ColorMatrix().apply { setSaturation(state.saturation) })
        val warmth = state.warmth / 100f
        weightedFilter.postConcat(ColorMatrix().apply { setScale(1f + warmth, 1f, 1f - warmth, 1f) })
        val contrast = state.contrast
        val translation = state.brightness + 128f * (1f - contrast)
        val adjustment = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translation,
                0f, contrast, 0f, 0f, translation,
                0f, 0f, contrast, 0f, translation,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        weightedFilter.postConcat(adjustment)
        return weightedFilter
    }
}
