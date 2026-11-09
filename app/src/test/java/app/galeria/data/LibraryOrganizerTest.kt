package app.galeria.data

import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryOrganizerTest {
    @Test
    fun hidesBucketsAndPrivateItemsFromTheNormalTimeline() {
        val visible = MediaItemFactory.from(MediaRecord(id = 1, kind = MediaKind.Image, bucketId = 10))
        val hidden = MediaItemFactory.from(MediaRecord(id = 2, kind = MediaKind.Image, bucketId = 20))
        val private = MediaItemFactory.from(MediaRecord(id = 3, kind = MediaKind.Image, bucketId = 10))
        val organizer = LibraryOrganizer(
            hiddenBuckets = setOf(20),
            privateUris = setOf(private.uri),
        )

        assertEquals(listOf(visible), organizer.visible(listOf(visible, hidden, private)))
    }

    @Test
    fun renamesAndDeletesCustomAlbumsWithoutDeletingTheirMedia() {
        val first = MediaItemFactory.from(MediaRecord(id = 1, kind = MediaKind.Image, bucketId = 10, bucketName = "Camera"))
        val second = MediaItemFactory.from(MediaRecord(id = 2, kind = MediaKind.Image, bucketId = 20, bucketName = "Download"))
        val organizer = LibraryOrganizer().assign(listOf(first, second), "Viajes")

        val renamed = organizer.renameAlbum("Viajes", "Escapadas")
        assertEquals(listOf("Escapadas", "Escapadas"), renamed.withCustomAlbums(listOf(first, second)).map { it.bucketName })

        val deleted = renamed.deleteAlbum("Escapadas")
        assertEquals(emptyMap(), deleted.customAlbums)
        assertEquals(listOf("Camera", "Download"), deleted.withCustomAlbums(listOf(first, second)).map { it.bucketName })
    }

    @Test
    fun assignsItemsToCustomAlbumsWithoutMovingTheirFiles() {
        val first = MediaItemFactory.from(MediaRecord(id = 1, kind = MediaKind.Image, bucketId = 10, bucketName = "Camera"))
        val second = MediaItemFactory.from(MediaRecord(id = 2, kind = MediaKind.Image, bucketId = 20, bucketName = "Download"))
        val organizer = LibraryOrganizer(customAlbums = mapOf(first.uri to "Viajes", second.uri to "Viajes"))

        val albums = AlbumGrouper.group(organizer.withCustomAlbums(listOf(first, second)))

        assertEquals(1, albums.size)
        assertEquals("Viajes", albums.single().name)
        assertEquals(2, albums.single().count)
    }
}
