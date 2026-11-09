package app.galeria.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerTransformTest {
    @Test
    fun limitsZoomAccumulatesPanAndResets() {
        val initial = ViewerTransform()
        val zoomed = initial.zoomBy(3f).panBy(12f, -8f)
        val maximum = zoomed.zoomBy(10f)
        val minimum = initial.zoomBy(0.1f)

        assertEquals(1f, initial.scale, 0f)
        assertEquals(3f, zoomed.scale, 0f)
        assertEquals(12f, zoomed.offsetX, 0f)
        assertEquals(-8f, zoomed.offsetY, 0f)
        assertEquals(6f, maximum.scale, 0f)
        assertEquals(1f, minimum.scale, 0f)
        assertEquals(ViewerTransform(), maximum.reset())
    }
}
