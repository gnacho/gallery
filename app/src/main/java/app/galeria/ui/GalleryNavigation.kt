package app.galeria.ui

import androidx.annotation.StringRes
import app.galeria.R
import app.galeria.data.MediaItem
import app.galeria.data.TimelineGrouper
import app.galeria.data.TimelineSection
import java.time.ZoneId

enum class GalleryDestination(@StringRes val labelRes: Int) {
    Gallery(R.string.gallery),
    Photos(R.string.photos),
    Memories(R.string.memories),
    Create(R.string.create),
    Albums(R.string.albums),
}

data class GalleryUiState(
    val destination: GalleryDestination,
    val sections: List<TimelineSection> = emptyList(),
    val viewerItem: MediaItem? = null,
    val permissionDenied: Boolean = false,
)

object GalleryNavigation {
    fun initial() = GalleryUiState(GalleryDestination.Photos)

    fun select(state: GalleryUiState, destination: GalleryDestination) = state.copy(destination = destination)

    fun mediaLoaded(state: GalleryUiState, items: List<MediaItem>, zoneId: ZoneId) = state.copy(
        sections = TimelineGrouper.group(items, zoneId),
        permissionDenied = false,
    )

    fun permissionDenied(state: GalleryUiState) = state.copy(permissionDenied = true, viewerItem = null)

    fun openViewer(state: GalleryUiState, item: MediaItem) = state.copy(viewerItem = item)

    fun closeViewer(state: GalleryUiState) = state.copy(viewerItem = null)
}
