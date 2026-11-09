package app.galeria.data

data class LibraryOrganizer(
    val hiddenBuckets: Set<Long> = emptySet(),
    val privateUris: Set<String> = emptySet(),
    val customAlbums: Map<String, String> = emptyMap(),
) {
    fun visible(items: List<MediaItem>): List<MediaItem> = items.filter {
        it.bucketId !in hiddenBuckets && it.uri !in privateUris && !it.isTrashed
    }

    fun withCustomAlbums(items: List<MediaItem>): List<MediaItem> = items.map { item ->
        val album = customAlbums[item.uri]
        if (album == null) item else item.copy(bucketId = album.hashCode().toLong(), bucketName = album)
    }

    fun toggleHidden(bucketId: Long): LibraryOrganizer = copy(
        hiddenBuckets = if (bucketId in hiddenBuckets) hiddenBuckets - bucketId else hiddenBuckets + bucketId,
    )

    fun assign(items: List<MediaItem>, album: String): LibraryOrganizer {
        val cleanName = album.trim()
        require(cleanName.isNotEmpty()) { "Album name cannot be blank" }
        return copy(customAlbums = customAlbums + items.associate { it.uri to cleanName })
    }

    fun renameAlbum(oldName: String, newName: String): LibraryOrganizer {
        val cleanName = newName.trim()
        require(cleanName.isNotEmpty()) { "Album name cannot be blank" }
        return copy(customAlbums = customAlbums.mapValues { (_, album) -> if (album == oldName) cleanName else album })
    }

    fun deleteAlbum(name: String): LibraryOrganizer = copy(
        customAlbums = customAlbums.filterValues { it != name },
    )
}
