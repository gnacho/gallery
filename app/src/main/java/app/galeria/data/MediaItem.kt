package app.galeria.data

data class MediaItem(
    val id: Long,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val dateTakenMillis: Long?,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0,
    val bucketId: Long = 0,
    val bucketName: String? = null,
    val durationMillis: Long? = null,
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
)

data class TimelineSection(
    val date: java.time.LocalDate?,
    val items: List<MediaItem>,
)
