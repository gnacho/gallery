package app.galeria.data

import java.io.ByteArrayOutputStream

/**
 * Clean-room byte-level encoder that produces an UltraHDR v1 JPEG (base SDR JPEG
 * + gain map JPEG + MPF APP2 + the two XMP packets). The structure follows the
 * Android "Ultra HDR image format" specification; no Honor code or binaries are
 * involved.
 *
 * Layout (matches the reference encoder libultrahdr):
 *
 * ```
 * FF D8                        (primary SOI)
 * APP1 XMP  (GContainer)       (primary image metadata)
 * APP2 MPF                     (locates the gain map)
 * <primary image data>         (base JPEG without its own SOI)
 * FF D8                        (gain map SOI)
 * APP1 XMP  (hdrgm)            (gain map metadata)
 * <gain map image data>
 * ```
 */
object UltraHdrEncoder {

    private const val XMP_NAMESPACE = "http://ns.adobe.com/xap/1.0/\u0000"
    private const val MPF_SIG = "MPF\u0000"
    private const val XMP_PACKET_ID = "W5M0MpCehiHzreSzNTczkc9d"

    private val SOI = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
    private val EOI = byteArrayOf(0xFF.toByte(), 0xD9.toByte())

    /** Wraps the XMP XML in an APP1 segment with the Adobe XMP packet header. */
    fun xmpApp1Segment(xmpXml: String): ByteArray {
        val packet = "<?xpacket begin=\"\uFEFF\" id=\"$XMP_PACKET_ID\"?>\n" +
            xmpXml + "\n" +
            "<?xpacket end=\"w\"?>"
        val namespace = XMP_NAMESPACE.toByteArray(Charsets.UTF_8)
        val packetBytes = packet.toByteArray(Charsets.UTF_8)
        return appSegment(0xE1, namespace + packetBytes)
    }

    /**
     * Builds the MPF APP2 segment (CIPA DC-007-2009). The MP entry values are
     * [primaryImageSize], [primaryImageOffset] (0), [gainMapSize],
     * [gainMapOffset].
     */
    fun mpfApp2Segment(
        primaryImageSize: Int,
        primaryImageOffset: Int,
        gainMapSize: Int,
        gainMapOffset: Int,
    ): ByteArray {
        val payload = ByteArrayOutputStream()
        payload.write(MPF_SIG.toByteArray(Charsets.US_ASCII))
        payload.write(byteArrayOf(0x49, 0x49, 0x2A, 0x00)) // "II" + magic 0x002A
        payload.writeU32BE(8) // index IFD offset
        payload.writeU16BE(3) // entry count
        // MPF Version (0xB000, UNDEFINED, count 4, "0100")
        payload.writeU16BE(0xB000)
        payload.writeU16BE(7)
        payload.writeU32BE(4)
        payload.write("0100".toByteArray(Charsets.US_ASCII))
        // Number of Images (0xB001, LONG, count 1, value 2)
        payload.writeU16BE(0xB001)
        payload.writeU16BE(4)
        payload.writeU32BE(1)
        payload.writeU32BE(2)
        // MP Entry (0xB002, UNDEFINED, count 32, offset 50)
        payload.writeU16BE(0xB002)
        payload.writeU16BE(7)
        payload.writeU32BE(32)
        payload.writeU32BE(50)
        // next IFD offset (none)
        payload.writeU32BE(0)
        // MP Entry 0: primary image
        payload.writeU32BE(0x030000)
        payload.writeU32BE(primaryImageSize)
        payload.writeU32BE(primaryImageOffset)
        payload.writeU16BE(0)
        payload.writeU16BE(0)
        // MP Entry 1: gain map image
        payload.writeU32BE(0x000000)
        payload.writeU32BE(gainMapSize)
        payload.writeU32BE(gainMapOffset)
        payload.writeU16BE(0)
        payload.writeU16BE(0)
        return appSegment(0xE2, payload.toByteArray())
    }

    /**
     * Assembles the final UltraHDR JPEG from the base SDR JPEG and the gain map
     * JPEG, inserting the GContainer XMP and MPF into the primary and the hdrgm
     * XMP into the gain map.
     */
    fun encode(baseJpeg: ByteArray, gainMapJpeg: ByteArray, metadata: GainMapMetadata): ByteArray {
        require(isJpeg(baseJpeg)) { "baseJpeg must start with SOI (FF D8)" }
        require(isJpeg(gainMapJpeg)) { "gainMapJpeg must start with SOI (FF D8)" }

        // Gain map image: insert the hdrgm XMP right after its SOI.
        val gainMapXmp = xmpApp1Segment(metadata.toXmp())
        val gainMapWithXmp = insertAfterSoi(gainMapJpeg, gainMapXmp)

        // Primary image: GContainer XMP + MPF right after its SOI.
        val container = GainMapContainer(gainMapWithXmp.size)
        val primaryXmp = xmpApp1Segment(container.toXmp())
        val mpfSize = 2 + 86 // APP2 marker (2) + length (2) + fixed 86-byte payload
        val primaryImageSize = baseJpeg.size + primaryXmp.size + mpfSize
        val gainMapOffset = primaryImageSize
        val mpf = mpfApp2Segment(primaryImageSize, 0, gainMapWithXmp.size, gainMapOffset)

        val header = SOI + primaryXmp + mpf
        val body = baseJpeg.copyOfRange(2, baseJpeg.size) // base without SOI
        return header + body + gainMapWithXmp
    }

    private fun isJpeg(bytes: ByteArray): Boolean =
        bytes.size >= 2 && bytes[0] == SOI[0] && bytes[1] == SOI[1]

    private fun insertAfterSoi(jpeg: ByteArray, segment: ByteArray): ByteArray {
        val out = ByteArray(jpeg.size + segment.size)
        jpeg.copyInto(out, 0, 0, 2)
        segment.copyInto(out, 2)
        jpeg.copyInto(out, 2 + segment.size, 2, jpeg.size)
        return out
    }

    private fun appSegment(marker: Int, payload: ByteArray): ByteArray {
        val length = payload.size + 2 // includes the 2-byte length field
        val out = ByteArray(payload.size + 4)
        out[0] = 0xFF.toByte()
        out[1] = marker.toByte()
        out[2] = ((length shr 8) and 0xFF).toByte()
        out[3] = (length and 0xFF).toByte()
        payload.copyInto(out, 4)
        return out
    }

    private fun ByteArrayOutputStream.writeU16BE(value: Int) {
        write((value shr 8) and 0xFF)
        write(value and 0xFF)
    }

    private fun ByteArrayOutputStream.writeU32BE(value: Int) {
        write((value ushr 24) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 8) and 0xFF)
        write(value and 0xFF)
    }
}
