package app.galeria.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UltraHdrEncoderTest {

    private val baseJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
    private val gainMapJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())

    private fun metadata() = GainMapGenerator.generate(IntArray(16 * 16) { 0xFFFFFFFF.toInt() }, 16, 16).metadata

    private fun ByteArray.indexOf(sub: ByteArray): Int {
        outer@ for (i in 0..size - sub.size) {
            for (j in sub.indices) if (this[i + j] != sub[j]) continue@outer
            return i
        }
        return -1
    }

    private fun ByteArray.containsAscii(s: String): Boolean = indexOf(s.toByteArray(Charsets.US_ASCII)) >= 0

    @Test
    fun encodeStartsWithSoiAndEndsWithGainMapEoi() {
        val out = UltraHdrEncoder.encode(baseJpeg, gainMapJpeg, metadata())
        assertEquals(0xFF.toByte(), out[0])
        assertEquals(0xD8.toByte(), out[1])
        assertEquals(0xFF.toByte(), out[out.size - 2])
        assertEquals(0xD9.toByte(), out[out.size - 1])
        assertTrue(out.size > baseJpeg.size + gainMapJpeg.size)
    }

    @Test
    fun encodeContainsMpfAndBothXmpPackets() {
        val out = UltraHdrEncoder.encode(baseJpeg, gainMapJpeg, metadata())
        assertTrue(out.containsAscii("MPF\u0000"), "missing MPF signature")
        assertTrue(out.containsAscii("http://ns.adobe.com/xap/1.0/"), "missing XMP namespace")
        assertTrue(out.containsAscii("hdrgm:Version=\"1.0\""), "missing hdrgm metadata")
        assertTrue(out.containsAscii("Container:Directory"), "missing GContainer directory")
        assertTrue(out.containsAscii("Item:Semantic=\"GainMap\""), "missing GainMap item")
    }

    @Test
    fun mpfSegmentCarriesVersionAndTwoImages() {
        val mpf = UltraHdrEncoder.mpfApp2Segment(100, 0, 50, 100)
        assertTrue(mpf.containsAscii("MPF\u0000"))
        assertTrue(mpf.containsAscii("0100"), "missing MPF version 1.0")
        // Number of Images tag 0xB001 with value 2
        val two = byteArrayOf(0xB0.toByte(), 0x01, 0x00, 0x04, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x02)
        assertTrue(mpf.indexOf(two) >= 0, "missing number-of-images=2 entry")
    }

    @Test
    fun mpfEntryOffsetsPointToGainMap() {
        // primary image 100 bytes at offset 0, gain map 50 bytes at offset 100.
        val mpf = UltraHdrEncoder.mpfApp2Segment(100, 0, 50, 100)
        // primary size and gain map offset are written big-endian in the MP entries.
        val primarySizeBe = byteArrayOf(0x00, 0x00, 0x00, 0x64) // 100
        val gainMapOffsetBe = byteArrayOf(0x00, 0x00, 0x00, 0x64) // 100
        assertTrue(mpf.indexOf(primarySizeBe) >= 0, "missing primary image size 100")
        assertTrue(mpf.indexOf(gainMapOffsetBe) >= 0, "missing gain map offset 100")
    }

    @Test
    fun encodeIsDeterministic() {
        val m = metadata()
        val a = UltraHdrEncoder.encode(baseJpeg, gainMapJpeg, m)
        val b = UltraHdrEncoder.encode(baseJpeg, gainMapJpeg, m)
        assertTrue(a.contentEquals(b), "encode must be deterministic")
    }
}
