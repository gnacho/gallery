package app.galeria.data

enum class MediaKind {
    Image,
    Video,
}

data class MediaRecord(
    val id: Long,
    val kind: MediaKind,
    val displayName: String = "$id.jpg",
    val mimeType: String = "image/jpeg",
    val dateTakenMillis: Long? = null,
    val dateAddedSeconds: Long? = null,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0,
    val bucketId: Long = 0,
    val bucketName: String? = null,
    val durationMillis: Long? = null,
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
)

object MediaItemFactory {
    fun from(record: MediaRecord) = MediaItem(
        id = record.id,
        uri = when (record.kind) {
            MediaKind.Image -> "content://media/external/images/media/${record.id}"
            MediaKind.Video -> "content://media/external/video/media/${record.id}"
        },
        displayName = record.displayName,
        mimeType = record.mimeType,
        dateTakenMillis = record.dateTakenMillis ?: record.dateAddedSeconds?.times(1000),
        width = record.width,
        height = record.height,
        sizeBytes = record.sizeBytes,
        bucketId = record.bucketId,
        bucketName = record.bucketName,
        durationMillis = record.durationMillis,
        isFavorite = record.isFavorite,
        isTrashed = record.isTrashed,
    )
}
