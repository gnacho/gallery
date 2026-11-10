package app.galeria.data

import kotlin.math.log2
import kotlin.math.pow

/**
 * ISO 21496-1 (UltraHDR) gain map metadata, matching the "hdrgm" XMP fields.
 *
 * Reference: Android "Ultra HDR image format" specification.
 */
data class GainMapMetadata(
    /** `map_min_log2`: log2 of the minimum content boost (0.0 for no attenuation). */
    val gainMapMin: Float,
    /** `map_max_log2`: log2 of the maximum content boost. */
    val gainMapMax: Float,
    /** `map_gamma`: gamma applied to the stored gain map values. */
    val gamma: Float,
    /** `offset_sdr`: offset added to SDR luminance before the ratio. */
    val offsetSdr: Float,
    /** `offset_hdr`: offset added to HDR luminance before the ratio. */
    val offsetHdr: Float,
    /** `hdr_capacity_min`: log2 of the minimum display boost (0.0). */
    val hdrCapacityMin: Float,
    /** `hdr_capacity_max`: log2 of the maximum display boost. */
    val hdrCapacityMax: Float,
) {
    /**
     * Serializes the "hdrgm" gain map metadata as an XMP packet fragment. A
     * conformant encoder wraps this in the gain map image's XMP packet.
     */
    fun toXmp(): String = buildString {
        append("""<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="XMP Core 5.5.0">""")
        append("""<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">""")
        append("""<rdf:Description rdf:about=""""")
        append(""" xmlns:hdrgm="http://ns.adobe.com/hdr-gain-map/1.0/"""")
        append(""" hdrgm:Version="1.0"""")
        append(""" hdrgm:BaseRenditionIsHDR="False"""")
        append(""" hdrgm:GainMapMin="$gainMapMin"""")
        append(""" hdrgm:GainMapMax="$gainMapMax"""")
        append(""" hdrgm:Gamma="$gamma"""")
        append(""" hdrgm:OffsetSDR="$offsetSdr"""")
        append(""" hdrgm:OffsetHDR="$offsetHdr"""")
        append(""" hdrgm:HDRCapacityMin="$hdrCapacityMin"""")
        append(""" hdrgm:HDRCapacityMax="$hdrCapacityMax"/>""")
        append("""</rdf:RDF></x:xmpmeta>""")
    }
}

/**
 * A single-channel gain map plus its ISO 21496-1 metadata. Each [gain] value is
 * the log2 HDR/SDR pixel ratio for that cell, normalized on encode.
 */
data class HdrGainMap(
    val width: Int,
    val height: Int,
    val gain: FloatArray,
    val metadata: GainMapMetadata,
) {
    /**
     * Encodes the log2 gain values to 8-bit per the spec:
     * `recovery = clamp((log2(pixel_gain) - map_min_log2) / (map_max_log2 - map_min_log2)) ^ map_gamma`,
     * then `floor(recovery * 255 + 0.5)`. With `map_gamma == 1` this is the
     * linear normalization of [gain] into [0, 255].
     */
    fun encodeGainMap8Bit(): IntArray {
        val range = metadata.gainMapMax - metadata.gainMapMin
        return IntArray(gain.size) { i ->
            val t = if (range <= 0f) 0f else ((gain[i] - metadata.gainMapMin) / range).coerceIn(0f, 1f)
            val recovery = t.pow(metadata.gamma)
            (recovery * 255f + 0.5f).toInt().coerceIn(0, 255)
        }
    }
}

/**
 * Clean-room SDR -> HDR upconversion. Computes a luminance-guided gain map and
 * its ISO 21496-1 metadata so a conformant decoder can reconstruct the HDR
 * rendition. No Honor code or binaries are involved.
 */
object GainMapGenerator {

    /** Downsample factor for the gain map relative to the base image. */
    private const val GAIN_MAP_DIVISOR = 4
    private const val GAMMA = 1f

    /** Spec-recommended offsets: 1/64. */
    private const val OFFSET = 0.015625f

    /** min_content_boost is 1.0 (no attenuation), so map_min_log2 == 0. */
    private const val MAP_MIN_LOG2 = 0f

    /** Rec. 709 luminance weights, linear space. */
    private const val WR = 0.2126f
    private const val WG = 0.7152f
    private const val WB = 0.0722f

    /** Highlight boost starts at 50% linear luminance. */
    private const val HIGHLIGHT_EDGE_LO = 0.5f
    private const val HIGHLIGHT_EDGE_HI = 1f

    /**
     * Generates a gain map from an SDR image.
     *
     * @param pixels ARGB_8888 packed pixels, row-major (from [android.graphics.Bitmap.getPixels]).
     * @param maxBoost maximum content boost; the HDR rendition reaches up to
     *   `1 + maxBoost` times the SDR luminance.
     */
    fun generate(pixels: IntArray, width: Int, height: Int, maxBoost: Float = 3f): HdrGainMap {
        require(width > 0 && height > 0) { "dimensions must be positive" }
        require(pixels.size == width * height) { "pixels.size must equal width * height" }
        require(maxBoost >= 0f) { "maxBoost must be non-negative" }

        val gw = (width / GAIN_MAP_DIVISOR).coerceAtLeast(1)
        val gh = (height / GAIN_MAP_DIVISOR).coerceAtLeast(1)
        val gain = FloatArray(gw * gh)

        for (gy in 0 until gh) {
            for (gx in 0 until gw) {
                val y0 = gy * GAIN_MAP_DIVISOR
                val x0 = gx * GAIN_MAP_DIVISOR
                var lumaSum = 0f
                var count = 0
                for (y in y0 until (y0 + GAIN_MAP_DIVISOR).coerceAtMost(height)) {
                    for (x in x0 until (x0 + GAIN_MAP_DIVISOR).coerceAtMost(width)) {
                        val c = pixels[y * width + x]
                        val r = toLinear(((c shr 16) and 0xFF) / 255f)
                        val g = toLinear(((c shr 8) and 0xFF) / 255f)
                        val b = toLinear((c and 0xFF) / 255f)
                        lumaSum += WR * r + WG * g + WB * b
                        count++
                    }
                }
                val ysdr = lumaSum / count
                val highlight = smoothstep(HIGHLIGHT_EDGE_LO, HIGHLIGHT_EDGE_HI, ysdr)
                val yhdr = ysdr * (1f + maxBoost * highlight)
                val pixelGain = (yhdr + OFFSET) / (ysdr + OFFSET)
                gain[gy * gw + gx] = log2(pixelGain)
            }
        }

        val mapMaxLog2 = log2(1f + maxBoost)
        val metadata = GainMapMetadata(
            gainMapMin = MAP_MIN_LOG2,
            gainMapMax = mapMaxLog2,
            gamma = GAMMA,
            offsetSdr = OFFSET,
            offsetHdr = OFFSET,
            hdrCapacityMin = MAP_MIN_LOG2.coerceAtLeast(0f),
            hdrCapacityMax = mapMaxLog2,
        )
        return HdrGainMap(gw, gh, gain, metadata)
    }

    private fun toLinear(srgb: Float): Float =
        if (srgb <= 0.04045f) srgb / 12.92f else ((srgb + 0.055f) / 1.055f).pow(2.4f)

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
