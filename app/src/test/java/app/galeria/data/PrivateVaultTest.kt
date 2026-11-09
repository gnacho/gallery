package app.galeria.data

import android.content.ContentValues
import android.provider.MediaStore
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrivateVaultTest {
    @Test
    fun importsAndRestoresAnEncryptedMediaItem() {
        val context = RuntimeEnvironment.getApplication()
        val resolver = context.contentResolver
        val source = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "private.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }))
        resolver.openOutputStream(source)!!.use { it.write(byteArrayOf(3, 1, 4, 1, 5)) }
        val item = MediaItem(1, source.toString(), "private.jpg", "image/jpeg", 1L)
        val key = SecretKeySpec(ByteArray(32) { (31 - it).toByte() }, "AES")
        val vault = PrivateVault(resolver, VaultFileStore(context, key), VaultIndexStore(context))

        val entry = vault.importItem(item)
        assertEquals(1, vault.entries().size)
        assertContentEquals(byteArrayOf(3, 1, 4, 1, 5), vault.read(entry))

        val restored = vault.restore(entry)

        assertContentEquals(byteArrayOf(3, 1, 4, 1, 5), resolver.openInputStream(restored)!!.use { it.readBytes() })
        assertEquals(0, vault.entries().size)
    }
}
