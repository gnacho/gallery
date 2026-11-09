package app.galeria.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaNotesStoreTest {
    @Test
    fun savesTrimsAndRemovesNotesByUri() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val store = MediaNotesStore(context)
        val uri = "content://media/external/images/media/42"
        store.set(uri, "  Cumpleaños de Ana  ")
        assertEquals("Cumpleaños de Ana", MediaNotesStore(context).all()[uri])
        store.set(uri, "   ")
        assertEquals(emptyMap<String, String>(), MediaNotesStore(context).all())
    }
}
