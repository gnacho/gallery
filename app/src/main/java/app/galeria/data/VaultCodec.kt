package app.galeria.data

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class VaultPayload(
    val iv: ByteArray,
    val cipherText: ByteArray,
)

class VaultCodec {
    fun encrypt(clear: ByteArray, key: SecretKey): VaultPayload {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return VaultPayload(cipher.iv, cipher.doFinal(clear))
    }

    fun decrypt(payload: VaultPayload, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, payload.iv))
        return cipher.doFinal(payload.cipherText)
    }
}
