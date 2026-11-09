package app.galeria.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HistogramCalculatorTest {
    @Test
    fun calculatesLuminanceAndIgnoresTransparentPixels() {
        val histogram = HistogramCalculator.luminance(
            intArrayOf(
                0xFF000000.toInt(),
                0xFFFFFFFF.toInt(),
                0xFFFF0000.toInt(),
                0x00FFFFFF,
            ),
        )

        assertEquals(256, histogram.size)
        assertEquals(1, histogram[0])
        assertEquals(1, histogram[76])
        assertEquals(1, histogram[255])
        assertEquals(3, histogram.sum())
    }
}
