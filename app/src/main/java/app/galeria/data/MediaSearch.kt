package app.galeria.data

import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

object MediaSearch {
    fun filter(
        items: List<MediaItem>,
        query: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
        notes: Map<String, String> = emptyMap(),
    ): List<MediaItem> {
        val terms = normalize(query).split(Regex("\\s+")).filter { it.isNotBlank() }
        if (terms.isEmpty()) return items
        return items.filter { item ->
            val haystack = buildList {
                add(normalize(item.displayName))
                add(normalize(item.bucketName.orEmpty()))
                add(normalize(item.mimeType))
                add(normalize(notes[item.uri].orEmpty()))
                item.dateTakenMillis?.let { timestamp ->
                    add(Instant.ofEpochMilli(timestamp).atZone(zoneId).year.toString())
                }
            }.joinToString(" ")
            terms.all { term -> haystack.contains(term) }
        }
    }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .trim()
}
