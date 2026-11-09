package app.galeria.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class VaultIndexStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("private_vault", Context.MODE_PRIVATE)

    fun load(): List<VaultEntry> {
        val array = JSONArray(preferences.getString("entries", "[]") ?: "[]")
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.getJSONObject(index)
                val item = MediaItem(
                    id = value.getLong("id"),
                    uri = value.getString("uri"),
                    displayName = value.getString("name"),
                    mimeType = value.getString("mime"),
                    dateTakenMillis = value.nullableLong("date"),
                    width = value.optInt("width"),
                    height = value.optInt("height"),
                    sizeBytes = value.optLong("size"),
                    bucketId = value.optLong("bucket_id"),
                    bucketName = if (value.isNull("bucket")) null else value.optString("bucket"),
                    durationMillis = value.nullableLong("duration"),
                    isFavorite = value.optBoolean("favorite"),
                    isTrashed = value.optBoolean("trashed"),
                )
                add(VaultEntry(item, value.getString("file"), value.getLong("imported")))
            }
        }
    }

    fun replace(entries: List<VaultEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            val item = entry.item
            array.put(JSONObject()
                .put("id", item.id)
                .put("uri", item.uri)
                .put("name", item.displayName)
                .put("mime", item.mimeType)
                .put("date", item.dateTakenMillis ?: JSONObject.NULL)
                .put("width", item.width)
                .put("height", item.height)
                .put("size", item.sizeBytes)
                .put("bucket_id", item.bucketId)
                .put("bucket", item.bucketName ?: JSONObject.NULL)
                .put("duration", item.durationMillis ?: JSONObject.NULL)
                .put("favorite", item.isFavorite)
                .put("trashed", item.isTrashed)
                .put("file", entry.fileName)
                .put("imported", entry.importedAt))
        }
        preferences.edit().putString("entries", array.toString()).apply()
    }

    private fun JSONObject.nullableLong(key: String): Long? = if (isNull(key)) null else optLong(key)
}
