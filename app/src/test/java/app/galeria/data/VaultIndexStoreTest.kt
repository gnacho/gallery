package app.galeria.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VaultIndexStoreTest {
    @Test
    fun restoresEncryptedEntriesAndTheirMediaMetadata() {
        val store = VaultIndexStore(RuntimeEnvironment.getApplication())
        val item = MediaItem(
            id = 4,
            uri = "content://media/4",
            displayName = "secreto.mp4",
            mimeType = "video/mp4",
            dateTakenMillis = 1234L,
            width = 1920,
            height = 1080,
            sizeBytes = 9000L,
            bucketId = 7L,
            bucketName = "Camera",
            durationMillis = 5000L,
        )
        store.replace(listOf(VaultEntry(item, "cifrado.bin", 777L)))

        val restored = store.load().single()
        assertEquals("cifrado.bin", restored.fileName)
        assertEquals(777L, restored.importedAt)
        assertEquals(item, restored.item)
    }
}
