package app.galeria.data

import android.content.ContentResolver
import android.net.Uri

class MediaLocationReader(private val resolver: ContentResolver) {
    fun read(items: List<MediaItem>): List<MediaLocation> {
        return items.mapNotNull { item ->
            if (!item.mimeType.startsWith("image/")) return@mapNotNull null
            val coordinates = runCatching {
                resolver.openInputStream(Uri.parse(item.uri))?.use(LocationExifParser::parse)
            }.getOrNull() ?: return@mapNotNull null
            MediaLocation(item, coordinates[0], coordinates[1])
        }
    }
}
