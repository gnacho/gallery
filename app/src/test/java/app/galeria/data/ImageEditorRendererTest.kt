package app.galeria.data

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageEditorRendererTest {
    @Test
    fun pixelatesOnlyTheSelectedMosaicRegion() {
        val source = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until 8) for (y in 0 until 8) setPixel(x, y, if (x < 4) Color.RED else Color.BLUE)
        }
        val mosaic = MosaicRegion(CropSpec(0.25f, 0f, 0.75f, 1f), blockSize = 8)

        val edited = ImageEditorRenderer.render(source, ImageEditState(mosaics = listOf(mosaic)))

        assertEquals(Color.RED, edited.getPixel(0, 0))
        assertEquals(127, Color.red(edited.getPixel(4, 0)))
        assertEquals(127, Color.blue(edited.getPixel(4, 0)))
        assertEquals(0, Color.green(edited.getPixel(4, 0)))
    }

    @Test
    fun rendersATextOverlay() {
        val source = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val overlay = TextOverlay("H", 0.1f, 0.8f, Color.RED, 0.7f)

        val edited = ImageEditorRenderer.render(source, ImageEditState(texts = listOf(overlay)))

        assertTrue((0 until edited.width).any { x ->
            (0 until edited.height).any { y ->
                val color = edited.getPixel(x, y)
                Color.red(color) == 255 && Color.green(color) == 0 && Color.blue(color) == 0
            }
        })
    }

    @Test
    fun drawsAFreehandStrokeOverTheImage() {
        val source = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val stroke = DrawingStroke(
            points = listOf(NormalizedPoint(0f, 0f), NormalizedPoint(1f, 1f)),
            color = Color.RED,
            widthFraction = 0.1f,
        )

        val edited = ImageEditorRenderer.render(source, ImageEditState(drawings = listOf(stroke)))

        assertEquals(Color.RED, edited.getPixel(10, 10))
    }

    @Test
    fun keepsOriginalColorsWhenFilterIntensityIsZero() {
        val source = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.rgb(10, 100, 200))

        val result = ImageEditorRenderer.render(source, ImageEditState(filter = ImageFilter.MONO, filterIntensity = 0f))

        assertEquals(Color.rgb(10, 100, 200), result.getPixel(0, 0))
    }

    @Test
    fun removesColorWhenSaturationIsZero() {
        val source = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.rgb(10, 100, 200))

        val result = ImageEditorRenderer.render(source, ImageEditState(saturation = 0f))
        val pixel = result.getPixel(0, 0)

        assertEquals(Color.red(pixel), Color.green(pixel))
        assertEquals(Color.green(pixel), Color.blue(pixel))
    }

    @Test
    fun increasesRedOverBlueWithPositiveWarmth() {
        val source = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.rgb(100, 100, 100))

        val result = ImageEditorRenderer.render(source, ImageEditState(warmth = 100f))
        val pixel = result.getPixel(0, 0)

        assertTrue(Color.red(pixel) > Color.blue(pixel))
    }

    @Test
    fun cropsTheSelectedNormalizedRegion() {
        val source = Bitmap.createBitmap(4, 2, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
            for (x in 2..3) for (y in 0..1) setPixel(x, y, Color.BLUE)
        }

        val cropped = ImageEditorRenderer.render(
            source,
            ImageEditState(crop = CropSpec(0.5f, 0f, 1f, 1f)),
        )

        assertEquals(2, cropped.width)
        assertEquals(2, cropped.height)
        assertEquals(Color.BLUE, cropped.getPixel(0, 0))
    }

    @Test
    fun drawsABorderWithoutChangingTheImageSize() {
        val source = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }

        val edited = ImageEditorRenderer.render(
            source,
            ImageEditState(border = BorderSpec(widthFraction = 0.1f, color = Color.RED)),
        )

        assertEquals(20, edited.width)
        assertEquals(20, edited.height)
        assertEquals(Color.RED, edited.getPixel(0, 0))
        assertEquals(Color.RED, edited.getPixel(19, 19))
        assertEquals(Color.BLUE, edited.getPixel(10, 10))
    }

    @Test
    fun rendersALunarNightComposition() {
        val source = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }

        val edited = ImageEditorRenderer.render(
            source,
            ImageEditState(watermark = WatermarkSpec("Noche lunar", lunar = true)),
        )

        assertEquals(Color.WHITE, edited.getPixel(18, 352))
        assertEquals(Color.BLUE, edited.getPixel(45, 346))
    }

    @Test
    fun rendersACustomWatermark() {
        val source = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }

        val edited = ImageEditorRenderer.render(source, ImageEditState(watermark = WatermarkSpec("Galeria")))

        assertEquals(400, edited.width)
        assertEquals(400, edited.height)
        assertTrue((0 until edited.width).any { x ->
            (0 until edited.height).any { y -> edited.getPixel(x, y) == Color.WHITE }
        })
    }

    @Test
    fun rotatesFlipsAndAppliesMonochrome() {
        val source = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888).apply {
            setPixel(0, 0, Color.RED)
            setPixel(1, 0, Color.BLUE)
        }

        val rotated = ImageEditorRenderer.render(source, ImageEditState(rotationDegrees = 90))
        assertEquals(1, rotated.width)
        assertEquals(2, rotated.height)

        val flipped = ImageEditorRenderer.render(source, ImageEditState(flipHorizontal = true))
        assertEquals(Color.BLUE, flipped.getPixel(0, 0))
        assertEquals(Color.RED, flipped.getPixel(1, 0))

        val mono = ImageEditorRenderer.render(source, ImageEditState(filter = ImageFilter.MONO))
        val color = mono.getPixel(0, 0)
        assertEquals(Color.red(color), Color.green(color))
        assertEquals(Color.green(color), Color.blue(color))
        assertTrue(Color.red(color) > 0)
    }
}
