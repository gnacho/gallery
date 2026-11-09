package app.galeria.data

import android.content.ContentResolver
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.util.UUID

object SyntheticMedia {
    fun uniqueName(extension: String): String = "android-test-${UUID.randomUUID()}.$extension"

    fun jpegBytes(width: Int = 96, height: Int = 72): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(24, 80, 160))
        Canvas(bitmap).apply {
            drawCircle(width * 0.35f, height * 0.45f, height * 0.25f, Paint().apply { color = Color.YELLOW })
            drawRect(width * 0.6f, height * 0.2f, width * 0.9f, height * 0.8f, Paint().apply { color = Color.MAGENTA })
        }
        return ByteArrayOutputStream().also { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output))
            bitmap.recycle()
        }.toByteArray()
    }

    fun importImage(resolver: ContentResolver, bytes: ByteArray = jpegBytes()): Pair<Uri, MediaItem> {
        val name = uniqueName("jpg")
        val source = MediaItem(0, "", name, "image/jpeg", null)
        val uri = MediaStoreOperations.importBytes(resolver, source, bytes)
        return uri to source.copy(id = android.content.ContentUris.parseId(uri), uri = uri.toString())
    }

    fun importVideo(resolver: ContentResolver, assets: AssetManager): Pair<Uri, MediaItem> {
        val bytes = assets.open("synthetic.mp4").use { it.readBytes() }
        val name = uniqueName("mp4")
        val source = MediaItem(
            id = 0,
            uri = "",
            displayName = name,
            mimeType = "video/mp4",
            dateTakenMillis = null,
            width = 96,
            height = 72,
            durationMillis = 2_000,
        )
        val uri = MediaStoreOperations.importBytes(resolver, source, bytes)
        return uri to source.copy(id = android.content.ContentUris.parseId(uri), uri = uri.toString())
    }
}
