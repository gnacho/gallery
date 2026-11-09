package app.galeria.data

import android.graphics.Bitmap
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FaceDetectionTest {
    @Test
    fun solidImageDoesNotProduceAPersonEntry() {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)

        assertFalse(FaceDetection.containsFace(bitmap))
    }
}
