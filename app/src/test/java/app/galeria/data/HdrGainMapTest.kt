package app.galeria.data

import kotlin.math.log2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HdrGainMapTest {

    private fun argb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    private fun filled(width: Int, height: Int, color: Int): IntArray = IntArray(width * height) { color }

    @Test
    fun blackImageProducesZeroGain() {
        val map = GainMapGenerator.generate(filled(16, 16, argb(0, 0, 0)), 16, 16)
        map.gain.forEach { assertEquals(0f, it, 1e-4f) }
        assertEquals(0f, map.metadata.gainMapMin, 1e-4f)
    }

    @Test
    fun whiteImageProducesMaximumGain() {
        val maxBoost = 3f
        val map = GainMapGenerator.generate(filled(16, 16, argb(255, 255, 255)), 16, 16, maxBoost)
        val expectedMax = log2(1f + maxBoost)
        map.gain.forEach { assertEquals(expectedMax, it, 1e-3f) }
        assertEquals(expectedMax, map.metadata.gainMapMax, 1e-3f)
        assertEquals(1f + maxBoost, map.metadata.hdrCapacityMax, 1e-3f)
    }

    @Test
    fun gainMapIsDownsampledByFour() {
        val map = GainMapGenerator.generate(filled(16, 8, argb(128, 128, 128)), 16, 8)
        assertEquals(4, map.width)
        assertEquals(2, map.height)
        assertEquals(4 * 2, map.gain.size)
    }

    @Test
    fun tinyImagesProduceSingleCell() {
        val map = GainMapGenerator.generate(filled(2, 2, argb(200, 200, 200)), 2, 2)
        assertEquals(1, map.width)
        assertEquals(1, map.height)
        assertEquals(1, map.gain.size)
    }

    @Test
    fun brighterRegionGetsHigherGain() {
        val w = 32
        val h = 8
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val v = if (x < w / 2) 0 else 255
                pixels[y * w + x] = argb(v, v, v)
            }
        }
        val map = GainMapGenerator.generate(pixels, w, h)
        val left = map.gain[0]
        val right = map.gain[map.width - 1]
        assertTrue(left < right, "dark cell gain $left should be < bright cell gain $right")
    }

    @Test
    fun metadataFollowsIsoFields() {
        val map = GainMapGenerator.generate(filled(16, 16, argb(200, 200, 200)), 16, 16)
        assertEquals(1f, map.metadata.gamma)
        assertEquals(0f, map.metadata.epsilonSdr)
        assertEquals(0f, map.metadata.epsilonHdr)
        assertEquals(1f, map.metadata.hdrCapacityMin)
        assertTrue(map.metadata.gainMapMax >= map.metadata.gainMapMin)
    }

    @Test
    fun gainStaysWithinMetadataRange() {
        val w = 16
        val h = 16
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val v = (x * 255) / (w - 1)
                pixels[y * w + x] = argb(v, v, v)
            }
        }
        val map = GainMapGenerator.generate(pixels, w, h)
        map.gain.forEach {
            assertTrue(it >= map.metadata.gainMapMin - 1e-4f, "gain $it below min ${map.metadata.gainMapMin}")
            assertTrue(it <= map.metadata.gainMapMax + 1e-4f, "gain $it above max ${map.metadata.gainMapMax}")
        }
    }

    @Test
    fun encodeGainMap8BitNormalizesToMetadataRange() {
        val maxBoost = 3f
        val map = GainMapGenerator.generate(filled(16, 16, argb(255, 255, 255)), 16, 16, maxBoost)
        val encoded = map.encodeGainMap8Bit()
        assertEquals(map.gain.size, encoded.size)
        encoded.forEach { assertEquals(255, it) }
    }

    @Test
    fun encodeGainMap8BitMapsBlackToZero() {
        val map = GainMapGenerator.generate(filled(16, 16, argb(0, 0, 0)), 16, 16)
        val encoded = map.encodeGainMap8Bit()
        encoded.forEach { assertEquals(0, it) }
    }
}
