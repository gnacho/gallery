package app.galeria.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryNavigationTest {
    @Test
    fun startsOnPhotos() {
        assertEquals(GalleryDestination.Photos, GalleryNavigation.initial().destination)
    }

    @Test
    fun exposesTheOriginalRailOrder() {
        assertEquals(
            listOf(
                GalleryDestination.Gallery,
                GalleryDestination.Photos,
                GalleryDestination.Memories,
                GalleryDestination.Create,
                GalleryDestination.Albums,
            ),
            GalleryDestination.entries,
        )
    }

    @Test
    fun selectsAnotherDestination() {
        val state = GalleryNavigation.select(GalleryNavigation.initial(), GalleryDestination.Albums)

        assertEquals(GalleryDestination.Albums, state.destination)
    }
}
