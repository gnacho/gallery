package app.galeria.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class PrivateVault(
    private val resolver: ContentResolver,
    private val files: VaultFileStore,
    private val index: VaultIndexStore,
) {
    constructor(context: Context) : this(
        context.contentResolver,
        VaultFileStore(context, VaultKeys.getOrCreate()),
        VaultIndexStore(context),
    )

    fun entries(): List<VaultEntry> = index.load()

    fun importItem(item: MediaItem): VaultEntry {
        val bytes = requireNotNull(resolver.openInputStream(Uri.parse(item.uri))).use { it.readBytes() }
        val entry = files.write(item, bytes)
        index.replace(listOf(entry) + index.load())
        return entry
    }

    fun read(entry: VaultEntry): ByteArray = files.read(entry)

    fun rollback(entry: VaultEntry) {
        files.delete(entry)
        index.replace(index.load().filterNot { it.fileName == entry.fileName })
    }

    fun restore(entry: VaultEntry): Uri {
        val restored = MediaStoreOperations.importBytes(resolver, entry.item, files.read(entry))
        rollback(entry)
        return restored
    }

    fun deletePermanent(entry: VaultEntry) = rollback(entry)
}

private object VaultKeys {
    private const val Alias = "galeria_private_vault"

    fun getOrCreate(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(Alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(Alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}
