package app.galeria.data

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore

object MediaStoreSaver {
    fun saveBitmap(resolver: ContentResolver, bitmap: Bitmap, displayName: String): Uri {
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Galeria/")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(collection, values)) { "MediaStore refused the new image" }
        try {
            requireNotNull(resolver.openOutputStream(uri, "w")).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) { "JPEG compression failed" }
            }
            val published = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(uri, published, null, null)
            return uri
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }
}
