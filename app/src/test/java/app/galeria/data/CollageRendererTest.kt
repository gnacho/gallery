package app.galeria.data

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CollageRendererTest {
    @Test
    fun rendersTwoBitmapsIntoASquareCanvas() {
        val first = Bitmap.createBitmap(10, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val second = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }

        val result = CollageRenderer.render(listOf(first, second))

        assertEquals(1600, result.width)
        assertEquals(1600, result.height)
        assertEquals(Color.RED, result.getPixel(100, 800))
        assertEquals(Color.BLUE, result.getPixel(1500, 800))
    }
}
