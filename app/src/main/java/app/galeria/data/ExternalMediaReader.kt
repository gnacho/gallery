package app.galeria.data

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri

object ExternalMediaReader {
    fun read(resolver: ContentResolver, source: Uri, mimeTypeHint: String? = null): MediaItem? {
        val queried = runCatching {
            resolver.query(source, MediaStoreQuery.projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val mimeType = cursor.stringOrNull("mime_type")
                    ?: resolver.getType(source)
                    ?: mimeTypeHint
                    ?: return@use null
                val kind = mimeType.toMediaKind() ?: return@use null
                val id = cursor.longOrNull("_id") ?: source.hashCode().toLong()
                MediaItemFactory.from(
                    MediaRecord(
                        id = id,
                        kind = kind,
                        displayName = cursor.stringOrNull("_display_name") ?: source.lastPathSegment ?: id.toString(),
                        mimeType = mimeType,
                        dateTakenMillis = cursor.positiveLongOrNull("datetaken"),
                        dateAddedSeconds = cursor.positiveLongOrNull("date_added"),
                        width = cursor.longOrNull("width")?.toInt() ?: 0,
                        height = cursor.longOrNull("height")?.toInt() ?: 0,
                        sizeBytes = cursor.longOrNull("_size") ?: 0,
                        bucketId = cursor.longOrNull("bucket_id") ?: 0,
                        bucketName = cursor.stringOrNull("bucket_display_name"),
                        durationMillis = cursor.positiveLongOrNull("duration"),
                        isFavorite = cursor.longOrNull("is_favorite") == 1L,
                        isTrashed = cursor.longOrNull("is_trashed") == 1L,
                    ),
                ).copy(uri = source.toString())
            }
        }.getOrNull()
        if (queried != null) return queried
        val mimeType = mimeTypeHint ?: resolver.getType(source) ?: return null
        val kind = mimeType.toMediaKind() ?: return null
        val id = source.hashCode().toLong()
        return MediaItemFactory.from(
            MediaRecord(
                id = id,
                kind = kind,
                displayName = source.lastPathSegment ?: id.toString(),
                mimeType = mimeType,
            ),
        ).copy(uri = source.toString())
    }
}

private fun String.toMediaKind(): MediaKind? = when {
    startsWith("image/") -> MediaKind.Image
    startsWith("video/") -> MediaKind.Video
    else -> null
}

private fun Cursor.longOrNull(column: String): Long? {
    val index = getColumnIndex(column)
    return if (index < 0 || isNull(index)) null else getLong(index)
}

private fun Cursor.positiveLongOrNull(column: String): Long? = longOrNull(column)?.takeIf { it > 0 }

private fun Cursor.stringOrNull(column: String): String? {
    val index = getColumnIndex(column)
    return if (index < 0 || isNull(index)) null else getString(index)
}
