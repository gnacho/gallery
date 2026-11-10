package app.galeria.data

import android.content.Context

enum class SortOrder { DATE_DESC, DATE_ASC, NAME_ASC }

data class GallerySettings(
    val dynamicColor: Boolean = true,
    val showVideos: Boolean = true,
    val slideshowSeconds: Int = 4,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
)

class GallerySettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("gallery_settings", Context.MODE_PRIVATE)

    fun load() = GallerySettings(
        dynamicColor = preferences.getBoolean("dynamic_color", true),
        showVideos = preferences.getBoolean("show_videos", true),
        slideshowSeconds = preferences.getInt("slideshow_seconds", 4).coerceIn(2, 15),
        sortOrder = runCatching { SortOrder.valueOf(preferences.getString("sort_order", SortOrder.DATE_DESC.name)!!) }
            .getOrDefault(SortOrder.DATE_DESC),
    )

    fun save(settings: GallerySettings) {
        preferences.edit()
            .putBoolean("dynamic_color", settings.dynamicColor)
            .putBoolean("show_videos", settings.showVideos)
            .putInt("slideshow_seconds", settings.slideshowSeconds.coerceIn(2, 15))
            .putString("sort_order", settings.sortOrder.name)
            .apply()
    }
}
