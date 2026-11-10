package app.galeria.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore

private enum class MediaCollectionMode { Normal, Favorites, Trash }

class MediaStoreRepository(private val contentResolver: ContentResolver) {
    constructor(context: Context) : this(context.applicationContext.contentResolver)

    fun load(): List<MediaItem> = load(MediaCollectionMode.Normal, SortOrder.DATE_DESC)

    fun load(sortOrder: SortOrder): List<MediaItem> = load(MediaCollectionMode.Normal, sortOrder)

    fun loadFavorites(): List<MediaItem> = load(MediaCollectionMode.Favorites, SortOrder.DATE_DESC)

    fun loadTrash(): List<MediaItem> = load(MediaCollectionMode.Trash, SortOrder.DATE_DESC)

    private fun load(mode: MediaCollectionMode, sortOrder: SortOrder): List<MediaItem> = buildList {
        addAll(query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaKind.Image, mode))
        addAll(query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaKind.Video, mode))
    }.distinctBy { it.uri }.sortedWith(sortComparator(sortOrder))

    private fun sortComparator(order: SortOrder): Comparator<MediaItem> = when (order) {
        SortOrder.DATE_DESC -> compareByDescending<MediaItem> { it.dateTakenMillis ?: Long.MIN_VALUE }.thenByDescending { it.id }
        SortOrder.DATE_ASC -> compareBy<MediaItem> { it.dateTakenMillis ?: Long.MAX_VALUE }.thenBy { it.id }
        SortOrder.NAME_ASC -> compareBy<MediaItem> { it.displayName.lowercase() }.thenBy { it.id }
    }

    private fun query(uri: Uri, kind: MediaKind, mode: MediaCollectionMode): List<MediaItem> {
        val cursor = if (mode == MediaCollectionMode.Normal) {
            contentResolver.query(
                uri,
                MediaStoreQuery.projection,
                null,
                null,
                MediaStoreQuery.sortOrder,
            )
        } else {
            val arguments = Bundle().apply {
                val key = if (mode == MediaCollectionMode.Favorites) {
                    MediaStore.QUERY_ARG_MATCH_FAVORITE
                } else {
                    MediaStore.QUERY_ARG_MATCH_TRASHED
                }
                putInt(key, MediaStore.MATCH_INCLUDE)
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, MediaStoreQuery.sortOrder)
            }
            contentResolver.query(uri, MediaStoreQuery.projection, arguments, null)
        } ?: return emptyList()
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    val id = it.longOrNull("_id") ?: continue
                    val mimeType = it.stringOrNull("mime_type")
                        ?: if (kind == MediaKind.Image) "image/*" else "video/*"
                    val expectedPrefix = if (kind == MediaKind.Image) "image/" else "video/"
                    if (!mimeType.startsWith(expectedPrefix)) continue
                    add(
                        MediaItemFactory.from(
                            MediaRecord(
                                id = id,
                                kind = kind,
                                displayName = it.stringOrNull("_display_name") ?: id.toString(),
                                mimeType = mimeType,
                                dateTakenMillis = it.positiveLongOrNull("datetaken"),
                                dateAddedSeconds = it.positiveLongOrNull("date_added"),
                                width = it.longOrNull("width")?.toInt() ?: 0,
                                height = it.longOrNull("height")?.toInt() ?: 0,
                                sizeBytes = it.longOrNull("_size") ?: 0,
                                bucketId = it.longOrNull("bucket_id") ?: 0,
                                bucketName = it.stringOrNull("bucket_display_name"),
                                durationMillis = it.positiveLongOrNull("duration"),
                                isFavorite = mode == MediaCollectionMode.Favorites || it.longOrNull("is_favorite") == 1L,
                                isTrashed = mode == MediaCollectionMode.Trash || it.longOrNull("is_trashed") == 1L,
                            ),
                        ),
                    )
                }
            }
        }
    }
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
