package app.galeria.data

object HistogramCalculator {
    fun luminance(pixels: IntArray): IntArray {
        val bins = IntArray(256)
        for (color in pixels) {
            val alpha = color ushr 24 and 0xFF
            if (alpha == 0) continue
            val red = color ushr 16 and 0xFF
            val green = color ushr 8 and 0xFF
            val blue = color and 0xFF
            val luminance = (299 * red + 587 * green + 114 * blue) / 1000
            bins[luminance]++
        }
        return bins
    }
}
