package app.galeria.data

data class VideoTrimSpec(
    val durationMillis: Long,
    val startMillis: Long,
    val endMillis: Long,
) {
    init {
        require(durationMillis > 0) { "The video duration must be positive" }
        require(startMillis >= 0) { "The start cannot be negative" }
        require(endMillis > startMillis) { "The end must be after the start" }
        require(endMillis <= durationMillis) { "The end cannot exceed the video duration" }
    }

    val clippedDurationMillis: Long
        get() = endMillis - startMillis
}
