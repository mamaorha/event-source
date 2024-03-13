package co.il.mh.event.source.utils

import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Encryption {
    private const val INITIALIZATION_VECTOR_LENGTH = 16

    fun aesEncrypt(keyBytes: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val secretKey = SecretKeySpec(keyBytes, "AES")

        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val encryptedData = cipher.doFinal(data)
        val initializationVector = cipher.iv

        return initializationVector + encryptedData
    }

    fun aesDecrypt(keyBytes: ByteArray, data: ByteArray): ByteArray {
        val initializationVector = data.copyOfRange(0, INITIALIZATION_VECTOR_LENGTH)
        val cipherData = data.copyOfRange(INITIALIZATION_VECTOR_LENGTH, data.size)
        val ivParameterSpec = IvParameterSpec(initializationVector)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val secretKey = SecretKeySpec(keyBytes, "AES")

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)

        return cipher.doFinal(cipherData)
    }

    fun aesEncryptAsBase64String(keyBytes: ByteArray, data: ByteArray): String {
        return Base64.getEncoder().encodeToString(aesEncrypt(keyBytes = keyBytes, data = data))
    }

    fun aesDecryptAsBase64String(keyBytes: ByteArray, base64Data: String): ByteArray {
        return aesDecrypt(keyBytes = keyBytes, data = Base64.getDecoder().decode(base64Data))
    }
}