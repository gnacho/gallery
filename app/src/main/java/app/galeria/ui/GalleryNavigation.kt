package app.galeria.ui

import app.galeria.data.MediaItem
import app.galeria.data.TimelineGrouper
import app.galeria.data.TimelineSection
import java.time.ZoneId

enum class GalleryDestination(val label: String) {
    Gallery("Galería"),
    Photos("Fotos"),
    Memories("Recuerdos"),
    Create("Crear"),
    Albums("Álbumes"),
}

data class GalleryUiState(
    val destination: GalleryDestination,
    val sections: List<TimelineSection> = emptyList(),
    val viewerItem: MediaItem? = null,
    val permissionDenied: Boolean = false,
)

object GalleryNavigation {
    val railLabels = GalleryDestination.entries.map { it.label }

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
