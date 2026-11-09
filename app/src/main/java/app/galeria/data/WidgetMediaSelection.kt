package app.galeria.data

object WidgetMediaSelection {
    fun latestImage(items: List<MediaItem>): MediaItem? = items
        .filter { it.mimeType.startsWith("image/") && !it.isTrashed }
        .maxWithOrNull(compareBy<MediaItem> { it.dateTakenMillis ?: Long.MIN_VALUE }.thenBy { it.id })
}
