package codes.tashif.paisa.shared

import java.io.ByteArrayOutputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

actual object Crypto {
    private const val TAG_BITS = 128
    private val random = SecureRandom()

    actual fun randomBytes(size: Int): ByteArray = ByteArray(size).also { random.nextBytes(it) }

    actual suspend fun pbkdf2Sha256(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    actual suspend fun aesGcmEncrypt(key: ByteArray, iv: ByteArray, aad: ByteArray, plain: ByteArray): ByteArray =
        cipher(Cipher.ENCRYPT_MODE, key, iv, aad).doFinal(plain)

    actual suspend fun aesGcmDecrypt(key: ByteArray, iv: ByteArray, aad: ByteArray, sealed: ByteArray): ByteArray? =
        try {
            cipher(Cipher.DECRYPT_MODE, key, iv, aad).doFinal(sealed)
        } catch (e: GeneralSecurityException) {
            null
        }

    private fun cipher(mode: Int, key: ByteArray, iv: ByteArray, aad: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
            updateAAD(aad)
        }
}

actual object Gzip {
    actual suspend fun compress(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }

    actual suspend fun decompress(data: ByteArray): ByteArray =
        GZIPInputStream(data.inputStream()).use { it.readBytes() }
}
