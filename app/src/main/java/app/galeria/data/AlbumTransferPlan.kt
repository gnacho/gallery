package app.galeria.data

data class AlbumTransferPlan(val albumName: String) {
    private val safeName = albumName.trim()

    init {
        require(safeName.isNotEmpty()) { "The album name cannot be blank" }
        require(safeName != "." && safeName != "..") { "Reserved album name" }
        require(!safeName.contains('/') && !safeName.contains('\\')) { "Album names cannot contain path separators" }
    }

    fun relativePathFor(item: MediaItem): String =
        if (item.mimeType.startsWith("video/")) "Movies/$safeName/" else "Pictures/$safeName/"
}
