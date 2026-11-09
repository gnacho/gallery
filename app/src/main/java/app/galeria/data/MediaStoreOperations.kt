package app.galeria.data

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import java.io.File

object MediaStoreOperations {
    fun duplicate(resolver: ContentResolver, item: MediaItem): Uri {
        val isVideo = item.mimeType.startsWith("video/")
        val collection = if (isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, copyName(item.displayName))
            put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, if (isVideo) "Movies/Galeria/" else "Pictures/Galeria/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val target = requireNotNull(resolver.insert(collection, values)) { "MediaStore refused the copy" }
        try {
            val input = requireNotNull(resolver.openInputStream(Uri.parse(item.uri)))
            val output = requireNotNull(resolver.openOutputStream(target, "w"))
            input.use { source -> output.use { destination -> source.copyTo(destination) } }
            resolver.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return target
        } catch (error: Exception) {
            resolver.delete(target, null, null)
            throw error
        }
    }

    fun importBytes(resolver: ContentResolver, item: MediaItem, bytes: ByteArray): Uri {
        val isVideo = item.mimeType.startsWith("video/")
        val collection = if (isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, item.displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, if (isVideo) "Movies/Galeria/" else "Pictures/Galeria/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val target = requireNotNull(resolver.insert(collection, values)) { "MediaStore refused the restore" }
        try {
            requireNotNull(resolver.openOutputStream(target, "w")).use { it.write(bytes) }
            resolver.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return target
        } catch (error: Exception) {
            resolver.delete(target, null, null)
            throw error
        }
    }

    fun importFile(resolver: ContentResolver, item: MediaItem, source: File, displayName: String): Uri =
        importFile(resolver, source, displayName, item.mimeType)

    fun importFile(resolver: ContentResolver, source: File, displayName: String, mimeType: String): Uri {
        val isVideo = mimeType.startsWith("video/")
        val collection = if (isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, if (isVideo) "Movies/Galeria/" else "Pictures/Galeria/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val target = requireNotNull(resolver.insert(collection, values)) { "MediaStore refused the import" }
        try {
            source.inputStream().use { input ->
                requireNotNull(resolver.openOutputStream(target, "w")).use { output -> input.copyTo(output) }
            }
            resolver.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return target
        } catch (error: Exception) {
            resolver.delete(target, null, null)
            throw error
        }
    }

    fun importUri(resolver: ContentResolver, source: Uri, displayName: String, mimeType: String): Uri {
        val isVideo = mimeType.startsWith("video/")
        val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, if (isVideo) "Movies/Galeria/" else "Pictures/Galeria/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val target = requireNotNull(resolver.insert(collection, values)) { "MediaStore refused the received content" }
        try {
            requireNotNull(resolver.openInputStream(source)).use { input ->
                requireNotNull(resolver.openOutputStream(target, "w")).use { output -> input.copyTo(output) }
            }
            resolver.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return target
        } catch (error: Throwable) {
            resolver.delete(target, null, null)
            throw error
        }
    }

    fun copyToAlbum(resolver: ContentResolver, item: MediaItem, plan: AlbumTransferPlan): Uri {
        val collection = if (item.mimeType.startsWith("video/")) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, item.displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, plan.relativePathFor(item))
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(collection, values)) { "Unable to create album copy" }
        try {
            resolver.openInputStream(Uri.parse(item.uri))!!.use { input ->
                resolver.openOutputStream(uri, "w")!!.use { output -> input.copyTo(output) }
            }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    fun moveToAlbum(resolver: ContentResolver, item: MediaItem, plan: AlbumTransferPlan): Int =
        resolver.update(
            Uri.parse(item.uri),
            ContentValues().apply { put(MediaStore.MediaColumns.RELATIVE_PATH, plan.relativePathFor(item)) },
            null,
            null,
        )

    fun rename(resolver: ContentResolver, item: MediaItem, newName: String): Int {
        val safeName = RenamePlan.fileName(item.displayName, newName)
        return resolver.update(
            Uri.parse(item.uri),
            ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, safeName) },
            null,
            null,
        )
    }

    internal fun copyName(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot > 0) "${name.substring(0, dot)}-copia${name.substring(dot)}" else "$name-copia"
    }
}
