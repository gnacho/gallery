package app.galeria.data

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class VaultCodecTest {
    @Test
    fun encryptsAndDetectsTampering() {
        val codec = VaultCodec()
        val key: SecretKey = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")
        val clear = "fotos privadas".encodeToByteArray()

        val encrypted = codec.encrypt(clear, key)

        assertFalse(encrypted.cipherText.contentEquals(clear))
        assertArrayEquals(clear, codec.decrypt(encrypted, key))
        encrypted.cipherText[0] = (encrypted.cipherText[0].toInt() xor 1).toByte()
        assertThrows(AEADBadTagException::class.java) { codec.decrypt(encrypted, key) }
    }
}
