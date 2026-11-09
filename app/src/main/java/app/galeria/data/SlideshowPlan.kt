package app.galeria.data

data class SlideshowPlan(
    val items: List<MediaItem>,
    val secondsPerImage: Int = 4,
) {
    val imageDurationMillis = secondsPerImage * 1000L
    val totalDurationMillis = imageDurationMillis * items.size

    init {
        require(items.size in 1..30) { "A slideshow requires between 1 and 30 images" }
        require(secondsPerImage in 1..15) { "Image duration must be between 1 and 15 seconds" }
        require(items.all { it.mimeType.startsWith("image/") }) { "A slideshow accepts only images" }
    }
}
