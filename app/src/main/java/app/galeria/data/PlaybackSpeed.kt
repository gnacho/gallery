package app.galeria.data

object PlaybackSpeed {
    val supported: List<Float> = listOf(0.5f, 1f, 1.5f, 2f)

    fun next(current: Float): Float {
        val index = supported.indexOf(current).coerceAtLeast(0)
        return supported[(index + 1) % supported.size]
    }
}
