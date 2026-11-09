package app.galeria.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SlideshowPlanTest {
    @Test
    fun buildsAnImageSequenceWithTheRequestedDuration() {
        val plan = SlideshowPlan(listOf(image(1), image(2)), secondsPerImage = 3)

        assertEquals(2, plan.items.size)
        assertEquals(3000L, plan.imageDurationMillis)
        assertEquals(6000L, plan.totalDurationMillis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsVideosInAnImageSequence() {
        SlideshowPlan(listOf(image(1).copy(mimeType = "video/mp4")))
    }

    private fun image(id: Long) = MediaItem(
        id = id,
        uri = "content://media/external/images/media/$id",
        displayName = "$id.jpg",
        mimeType = "image/jpeg",
        dateTakenMillis = id,
    )
}
