package app.galeria.ui

data class ViewerTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    fun zoomBy(factor: Float): ViewerTransform = copy(scale = (scale * factor).coerceIn(1f, 6f))

    fun panBy(x: Float, y: Float): ViewerTransform = copy(offsetX = offsetX + x, offsetY = offsetY + y)

    fun reset(): ViewerTransform = ViewerTransform()
}
