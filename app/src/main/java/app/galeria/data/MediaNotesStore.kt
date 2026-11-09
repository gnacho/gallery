package app.galeria.data

import android.content.Context
import android.util.Base64

class MediaNotesStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("gallery_media_notes", Context.MODE_PRIVATE)

    fun all(): Map<String, String> = preferences.all.mapNotNull { (encoded, value) ->
        (value as? String)?.let { note ->
            runCatching {
                String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8) to note
            }.getOrNull()
        }
    }.toMap()

    fun set(uri: String, note: String) {
        val key = Base64.encodeToString(uri.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
        val trimmed = note.trim()
        if (trimmed.isEmpty()) preferences.edit().remove(key).apply()
        else preferences.edit().putString(key, trimmed).apply()
    }
}
