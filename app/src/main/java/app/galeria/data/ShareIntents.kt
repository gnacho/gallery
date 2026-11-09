package app.galeria.data

import android.content.Intent
import android.net.Uri

object ShareIntents {
    fun forItems(items: List<MediaItem>): Intent {
        require(items.isNotEmpty()) { "At least one media item is required" }
        val mimeType = items.map { it.mimeType }.distinct().singleOrNull() ?: "*/*"
        return if (items.size == 1) {
            Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_STREAM, Uri.parse(items.single().uri))
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    ArrayList(items.map { Uri.parse(it.uri) }),
                )
        }.setType(mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
