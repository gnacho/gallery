package app.galeria.data

import android.app.PendingIntent
import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore

object MediaStoreRequests {
    fun trash(resolver: ContentResolver, items: List<MediaItem>, trashed: Boolean = true): PendingIntent =
        MediaStore.createTrashRequest(resolver, items.uris(), trashed)

    fun delete(resolver: ContentResolver, items: List<MediaItem>): PendingIntent =
        MediaStore.createDeleteRequest(resolver, items.uris())

    fun favorite(resolver: ContentResolver, items: List<MediaItem>, favorite: Boolean = true): PendingIntent =
        MediaStore.createFavoriteRequest(resolver, items.uris(), favorite)

    fun write(resolver: ContentResolver, items: List<MediaItem>): PendingIntent =
        MediaStore.createWriteRequest(resolver, items.uris())

    private fun List<MediaItem>.uris(): Collection<Uri> = map { Uri.parse(it.uri) }
}
