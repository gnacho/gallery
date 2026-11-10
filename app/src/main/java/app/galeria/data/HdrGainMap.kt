package app.galeria.data

import kotlin.math.log2
import kotlin.math.pow

/**
 * ISO 21496-1 gain map metadata (the UltraHDR "hdrgm" fields).
 */
data class GainMapMetadata(
    /** Log2 gain ratio of the darkest gain map entry. */
    val gainMapMin: Float,
    /** Log2 gain ratio of the brightest gain map entry. */
    val gainMapMax: Float,
    /** Gain map gamma (1.0 = linear). */
    val gamma: Float,
    /** SDR offset applied before the ratio. */
    val epsilonSdr: Float,
    /** HDR offset applied before the ratio. */
    val epsilonHdr: Float,
    /** Display ratio of the SDR rendition (typically 1.0). */
    val hdrCapacityMin: Float,
    /** Display ratio the HDR rendition can reach. */
    val hdrCapacityMax: Float,
)

/**
 * A single-channel gain map plus its ISO 21496-1 metadata. Each [gain] value is
 * the log2 HDR/SDR ratio for that cell.
 */
data class HdrGainMap(
    val width: Int,
    val height: Int,
    val gain: FloatArray,
    val metadata: GainMapMetadata,
) {
    /**
     * Encodes the log2 gain values to 8-bit, normalized to the metadata range
     * [gainMapMin, gainMapMax]. The caller packs these into a single-channel
     * image (for example ALPHA_8) for the gain map container.
     */
    fun encodeGainMap8Bit(): IntArray {
        val range = metadata.gainMapMax - metadata.gainMapMin
        return IntArray(gain.size) { i ->
            val t = if (range <= 0f) 0f else ((gain[i] - metadata.gainMapMin) / range).coerceIn(0f, 1f)
            (t * 255f + 0.5f).toInt()
        }
    }
}

/**
 * Clean-room SDR -> HDR upconversion. Computes a luminance-guided gain map and
 * its ISO 21496-1 metadata so a conformant decoder can reconstruct the HDR
 * rendition as `SDR * 2^gain`. No Honor code or binaries are involved.
 */
object GainMapGenerator {

    /** Downsample factor for the gain map relative to the base image. */
    private const val GAIN_MAP_DIVISOR = 4
    private const val GAMMA = 1f
    private const val EPSILON = 0f
    private const val HDR_CAPACITY_MIN = 1f

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
     * @param maxBoost maximum highlight boost; the HDR rendition reaches up to
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
                val luma = lumaSum / count
                val highlight = smoothstep(HIGHLIGHT_EDGE_LO, HIGHLIGHT_EDGE_HI, luma)
                gain[gy * gw + gx] = log2(1f + maxBoost * highlight)
            }
        }

        val metadata = GainMapMetadata(
            gainMapMin = 0f,
            gainMapMax = log2(1f + maxBoost),
            gamma = GAMMA,
            epsilonSdr = EPSILON,
            epsilonHdr = EPSILON,
            hdrCapacityMin = HDR_CAPACITY_MIN,
            hdrCapacityMax = 1f + maxBoost,
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
