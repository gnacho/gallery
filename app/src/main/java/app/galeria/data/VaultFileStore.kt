package app.galeria.data

import android.content.Context
import java.io.File
import java.util.UUID
import javax.crypto.SecretKey

class VaultEntry(
    val item: MediaItem,
    val fileName: String,
    val importedAt: Long,
)

class VaultFileStore(
    context: Context,
    private val key: SecretKey,
    private val codec: VaultCodec = VaultCodec(),
) {
    private val directory = File(context.filesDir, "vault")

    fun write(item: MediaItem, clear: ByteArray): VaultEntry {
        check(directory.exists() || directory.mkdirs()) { "No se pudo crear el cofre" }
        val entry = VaultEntry(item, UUID.randomUUID().toString(), System.currentTimeMillis())
        val encrypted = codec.encrypt(clear, key)
        File(directory, entry.fileName).writeBytes(encrypted.iv + encrypted.cipherText)
        return entry
    }

    fun read(entry: VaultEntry): ByteArray {
        val bytes = File(directory, entry.fileName).readBytes()
        require(bytes.size > 12) { "Archivo del cofre incompleto" }
        return codec.decrypt(VaultPayload(bytes.copyOfRange(0, 12), bytes.copyOfRange(12, bytes.size)), key)
    }

    fun delete(entry: VaultEntry): Boolean = File(directory, entry.fileName).delete()
}
