package app.galeria.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import kotlin.math.ceil

data class TrashRetention(
    val expiresAtMillis: Long,
    val remainingDays: Int,
    val expired: Boolean,
)

object TrashRetentionReader {
    fun read(resolver: ContentResolver, uri: Uri, nowMillis: Long): TrashRetention? =
        resolver.query(uri, arrayOf(MediaStore.MediaColumns.DATE_EXPIRES), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_EXPIRES)
            if (index < 0 || cursor.isNull(index)) return@use null
            val expiresAtMillis = cursor.getLong(index) * 1000L
            val remainingMillis = expiresAtMillis - nowMillis
            TrashRetention(
                expiresAtMillis = expiresAtMillis,
                remainingDays = ceil(remainingMillis / 86_400_000.0).toInt(),
                expired = remainingMillis <= 0,
            )
        }
}
