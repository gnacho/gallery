package app.galeria.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageEditStateTest {
    @Test
    fun rotatesFlipsAdjustsAndResets() {
        val initial = ImageEditState()
        val edited = initial
            .rotateClockwise()
            .rotateClockwise()
            .rotateCounterClockwise()
            .toggleFlip()
            .copy(brightness = 45f, contrast = 1.4f, filter = ImageFilter.MONO)

        assertEquals(90, edited.rotationDegrees)
        assertEquals(true, edited.flipHorizontal)
        assertEquals(45f, edited.brightness, 0f)
        assertEquals(1.4f, edited.contrast, 0f)
        assertEquals(ImageFilter.MONO, edited.filter)
        assertEquals(ImageEditState(), edited.reset())
        assertEquals(270, initial.rotateCounterClockwise().rotationDegrees)
    }
}
