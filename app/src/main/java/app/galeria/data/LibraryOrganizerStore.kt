package app.galeria.data

import android.content.Context
import org.json.JSONObject

class LibraryOrganizerStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("library_organization", Context.MODE_PRIVATE)

    fun load(): LibraryOrganizer {
        val customJson = JSONObject(preferences.getString("custom_albums", "{}") ?: "{}")
        val customAlbums = buildMap {
            customJson.keys().forEach { key -> put(key, customJson.getString(key)) }
        }
        return LibraryOrganizer(
            hiddenBuckets = preferences.getStringSet("hidden_buckets", emptySet()).orEmpty().mapNotNullTo(mutableSetOf()) {
                it.toLongOrNull()
            },
            privateUris = preferences.getStringSet("private_uris", emptySet()).orEmpty(),
            customAlbums = customAlbums,
        )
    }

    fun save(organizer: LibraryOrganizer) {
        val customJson = JSONObject()
        organizer.customAlbums.forEach { (uri, album) -> customJson.put(uri, album) }
        preferences.edit()
            .putStringSet("hidden_buckets", organizer.hiddenBuckets.mapTo(mutableSetOf()) { it.toString() })
            .putStringSet("private_uris", organizer.privateUris)
            .putString("custom_albums", customJson.toString())
            .apply()
    }
}
