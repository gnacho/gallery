package app.galeria.data

data class LibraryViews(
    val library: List<MediaItem>,
    val timeline: List<MediaItem>,
)

object GalleryContentFilter {
    fun views(items: List<MediaItem>, showVideos: Boolean): LibraryViews = LibraryViews(
        library = items,
        timeline = if (showVideos) items else items.filter { !it.mimeType.startsWith("video/") },
    )
}
