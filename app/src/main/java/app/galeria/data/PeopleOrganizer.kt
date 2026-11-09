package app.galeria.data

import android.content.Context

data class PersonGroup(
    val name: String,
    val items: List<MediaItem>,
)

object PeopleOrganizer {
    fun groups(items: List<MediaItem>, assignments: Map<String, Set<String>>): List<PersonGroup> {
        return assignments.entries
            .sortedBy { it.key }
            .map { (name, uris) -> PersonGroup(name, items.filter { it.uri in uris }) }
    }
}

class PeopleStore(context: Context, preferencesName: String = "gallery_people") {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    fun assignments(): Map<String, Set<String>> {
        return preferences.all.mapNotNull { (name, value) ->
            @Suppress("UNCHECKED_CAST")
            (value as? Set<String>)?.let { name to it }
        }.toMap()
    }

    fun assign(name: String, items: List<MediaItem>) {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || items.isEmpty()) return
        val updated = preferences.getStringSet(cleanName, emptySet()).orEmpty() + items.map { it.uri }
        preferences.edit().putStringSet(cleanName, updated).apply()
    }
}
