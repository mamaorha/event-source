package co.il.mh.event.source.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*
import javax.crypto.BadPaddingException

class EncryptionTest {
    @Nested
    inner class Aes {
        @Test
        fun `should encrypt and decrypt byte array`() {
            val keyBytes = UUID.randomUUID().toString().substring(0, 16).toByteArray()
            val data = UUID.randomUUID().toString().toByteArray()

            val encrypted = Encryption.aesEncrypt(
                keyBytes = keyBytes,
                data = data
            )

            Assertions.assertFalse(data.contentEquals(encrypted))

            val decrypted = Encryption.aesDecrypt(
                keyBytes = keyBytes,
                data = encrypted
            )

            Assertions.assertTrue(data.contentEquals(decrypted))
        }

        @Test
        fun `should fail if decryption key is not same as encryption`() {
            val keyBytes = UUID.randomUUID().toString().substring(0, 16).toByteArray()

            val encrypted = Encryption.aesEncrypt(
                keyBytes = keyBytes,
                data = UUID.randomUUID().toString().toByteArray()
            )

            Assertions.assertThrows(BadPaddingException::class.java) {
                Encryption.aesDecrypt(
                    keyBytes = UUID.randomUUID().toString().substring(0, 16).toByteArray(),
                    data = encrypted
                )
            }
        }
    }

    @Nested
    inner class Base64 {
        @Test
        fun `should encrypt and decrypt byte array`() {
            val keyBytes = UUID.randomUUID().toString().substring(0, 16).toByteArray()
            val data = UUID.randomUUID().toString()

            val encrypted = Encryption.aesEncryptAsBase64String(
                keyBytes = keyBytes,
                data = data.toByteArray()
            )

            Assertions.assertNotEquals(data, encrypted)

            val decrypted = Encryption.aesDecryptAsBase64String(
                keyBytes = keyBytes,
                base64Data = encrypted
            )

            Assertions.assertEquals(data, String(decrypted))
        }

        @Test
        fun `should fail if decryption key is not same as encryption`() {
            val keyBytes = UUID.randomUUID().toString().substring(0, 16).toByteArray()

            val encrypted = Encryption.aesEncryptAsBase64String(
                keyBytes = keyBytes,
                data = UUID.randomUUID().toString().toByteArray()
            )

            Assertions.assertThrows(BadPaddingException::class.java) {
                Encryption.aesDecryptAsBase64String(
                    keyBytes = UUID.randomUUID().toString().substring(0, 16).toByteArray(),
                    base64Data = encrypted
                )
            }
        }
    }
}