package app.galeria.data

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoTrimSpecTest {
    @Test
    fun acceptsABoundedRangeInsideTheVideo() {
        val spec = VideoTrimSpec(durationMillis = 10_000, startMillis = 1_000, endMillis = 4_500)

        assertEquals(3_500, spec.clippedDurationMillis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAnEndAfterTheDuration() {
        VideoTrimSpec(durationMillis = 10_000, startMillis = 1_000, endMillis = 10_001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAnEmptyRange() {
        VideoTrimSpec(durationMillis = 10_000, startMillis = 2_000, endMillis = 2_000)
    }
}
