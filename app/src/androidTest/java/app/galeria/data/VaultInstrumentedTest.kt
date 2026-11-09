package app.galeria.data

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver = context.contentResolver
    private val created = mutableListOf<Uri>()

    @After
    fun cleanUp() {
        created.forEach { resolver.delete(it, null, null) }
    }

    @Test
    fun androidKeyStoreEncryptsAndRestoresOriginalBytes() {
        val clear = SyntheticMedia.jpegBytes()
        val (source, item) = SyntheticMedia.importImage(resolver, clear)
        created += source
        val vault = PrivateVault(context)
        var entry: VaultEntry? = null

        try {
            val imported = vault.importItem(item)
            entry = imported
            val encryptedFile = File(File(context.filesDir, "vault"), imported.fileName)
            val encrypted = encryptedFile.readBytes()
            assertEquals(clear.size + 28, encrypted.size)
            assertFalse(encrypted.copyOfRange(12, encrypted.size).contentEquals(clear))
            assertContentEquals(clear, vault.read(imported))
            assertEquals(imported.fileName, vault.entries().single().fileName)

            val restored = vault.restore(imported)
            created += restored
            entry = null
            assertContentEquals(clear, resolver.openInputStream(restored)!!.use { it.readBytes() })
            assertTrue(vault.entries().isEmpty())
            assertFalse(encryptedFile.exists())
        } finally {
            entry?.let(vault::rollback)
        }
    }
}
