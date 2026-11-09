package app.galeria.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSpeedTest {
    @Test
    fun cyclesThroughDiscreteVideoSpeeds() {
        assertEquals(listOf(0.5f, 1f, 1.5f, 2f), PlaybackSpeed.supported)
        assertEquals(1f, PlaybackSpeed.next(0.5f))
        assertEquals(1.5f, PlaybackSpeed.next(1f))
        assertEquals(2f, PlaybackSpeed.next(1.5f))
        assertEquals(0.5f, PlaybackSpeed.next(2f))
    }
}
