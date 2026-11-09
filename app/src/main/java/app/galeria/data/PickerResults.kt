package app.galeria.data

import android.content.ClipData
import android.content.Intent
import android.net.Uri

object PickerResults {
    fun single(uri: Uri): Intent = Intent()
        .setData(uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    fun multiple(uris: List<Uri>): Intent {
        require(uris.isNotEmpty()) { "At least one URI is required" }
        if (uris.size == 1) return single(uris.single())
        val clip = ClipData.newRawUri("Galeria", uris.first())
        uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
        return Intent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply { clipData = clip }
    }
}
