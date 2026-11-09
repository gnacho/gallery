package app.galeria.data

import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryOrganizerStoreTest {
    @Test
    fun restoresTheCompleteOrganization() {
        val context = RuntimeEnvironment.getApplication()
        val expected = LibraryOrganizer(
            hiddenBuckets = setOf(10L, 20L),
            privateUris = setOf("content://media/1"),
            customAlbums = mapOf("content://media/1" to "Viajes"),
        )

        LibraryOrganizerStore(context).save(expected)

        assertEquals(expected, LibraryOrganizerStore(context).load())
    }
}
