package app.galeria.data

data class AlbumSummary(
    val id: Long,
    val name: String,
    val cover: MediaItem,
    val count: Int,
    val items: List<MediaItem>,
)

object AlbumGrouper {
    fun group(items: List<MediaItem>): List<AlbumSummary> = items
        .sortedWith(compareByDescending<MediaItem> { it.dateTakenMillis ?: Long.MIN_VALUE }.thenByDescending { it.id })
        .groupBy { it.bucketId }
        .map { (bucketId, albumItems) ->
            AlbumSummary(
                id = bucketId,
                name = albumItems.first().bucketName ?: "Sin álbum",
                cover = albumItems.first(),
                count = albumItems.size,
                items = albumItems,
            )
        }
        .sortedByDescending { it.cover.dateTakenMillis ?: Long.MIN_VALUE }
}
