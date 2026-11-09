package app.galeria.data

enum class ImageFilter { ORIGINAL, MONO, SEPIA, VIVID }

data class CropSpec(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init {
        require(left >= 0f && top >= 0f && right <= 1f && bottom <= 1f)
        require(right > left && bottom > top)
    }
}

data class NormalizedPoint(val x: Float, val y: Float) {
    init {
        require(x in 0f..1f && y in 0f..1f)
    }
}

data class DrawingStroke(
    val points: List<NormalizedPoint>,
    val color: Int,
    val widthFraction: Float = 0.02f,
) {
    init {
        require(points.isNotEmpty())
        require(widthFraction > 0f)
    }
}

data class MosaicRegion(val region: CropSpec, val blockSize: Int = 16) {
    init {
        require(blockSize >= 2)
    }
}

data class TextOverlay(
    val text: String,
    val x: Float,
    val y: Float,
    val color: Int,
    val sizeFraction: Float = 0.1f,
) {
    init {
        require(text.isNotBlank())
        require(x in 0f..1f && y in 0f..1f)
        require(sizeFraction > 0f)
    }
}

data class BorderSpec(val widthFraction: Float, val color: Int) {
    init {
        require(widthFraction > 0f && widthFraction <= 0.25f)
    }
}

data class WatermarkSpec(val text: String, val lunar: Boolean = false) {
    init {
        require(text.isNotBlank())
    }
}

data class ImageEditState(
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val filter: ImageFilter = ImageFilter.ORIGINAL,
    val filterIntensity: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val crop: CropSpec? = null,
    val drawings: List<DrawingStroke> = emptyList(),
    val texts: List<TextOverlay> = emptyList(),
    val mosaics: List<MosaicRegion> = emptyList(),
    val border: BorderSpec? = null,
    val watermark: WatermarkSpec? = null,
) {
    init {
        require(rotationDegrees in 0..359 && rotationDegrees % 90 == 0)
        require(brightness in -100f..100f)
        require(contrast in 0.5f..2f)
        require(filterIntensity in 0f..1f)
        require(saturation in 0f..2f)
        require(warmth in -100f..100f)
    }

    fun rotateClockwise(): ImageEditState = copy(rotationDegrees = (rotationDegrees + 90) % 360)

    fun rotateCounterClockwise(): ImageEditState = copy(rotationDegrees = (rotationDegrees + 270) % 360)

    fun toggleFlip(): ImageEditState = copy(flipHorizontal = !flipHorizontal)

    fun reset(): ImageEditState = ImageEditState()
}
