package app.galeria.data

import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VaultFileStoreTest {
    @Test
    fun storesOnlyCipherTextAndReadsTheOriginalBytes() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val key = SecretKeySpec(ByteArray(32) { (it + 1).toByte() }, "AES")
        val store = VaultFileStore(context, key)
        val clear = byteArrayOf(4, 8, 15, 16, 23, 42)
        val item = MediaItem(9, "content://media/9", "secreto.jpg", "image/jpeg", 1L)

        val entry = store.write(item, clear)

        assertArrayEquals(clear, store.read(entry))
        val stored = java.io.File(context.filesDir, "vault/${entry.fileName}").readBytes()
        assertFalse(stored.toList().windowed(clear.size).any { it == clear.toList() })
        assertTrue(store.delete(entry))
        assertFalse(java.io.File(context.filesDir, "vault/${entry.fileName}").exists())
    }
}
