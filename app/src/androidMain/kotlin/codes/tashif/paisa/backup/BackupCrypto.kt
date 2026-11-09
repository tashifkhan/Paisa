package codes.tashif.paisa.backup

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * `.paisa` container. Pure JVM so it can be unit tested.
 *
 * ```
 * "PAISABAK"         8 bytes magic
 * version            1 byte  (= 1)
 * flags              1 byte  (bit 0 = encrypted)
 * if encrypted:
 *   salt             16 bytes
 *   iterations       int32 big-endian (PBKDF2-HMAC-SHA256)
 *   iv               12 bytes
 * body               gzip(json), or AES-256-GCM(gzip(json)) with the header as AAD
 * ```
 */
object BackupCrypto {
    private val MAGIC = "PAISABAK".toByteArray(Charsets.US_ASCII)
    private const val VERSION: Int = 1
    private const val FLAG_ENCRYPTED = 1
    const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    const val DEFAULT_ITERATIONS = 210_000

    /** Upper bound on iterations read from an untrusted header. */
    private const val MAX_ITERATIONS = 5_000_000

    private val random = SecureRandom()

    class WrongPasswordException : IOException("Wrong backup password")
    class NotABackupException(message: String) : IOException(message)

    data class Header(
        val encrypted: Boolean,
        val salt: ByteArray?,
        val iterations: Int,
        val iv: ByteArray?,
        /** Raw header bytes, authenticated as GCM associated data. */
        val bytes: ByteArray
    )

    /** A derived key plus the salt/iterations it came from. */
    data class DerivedKey(val key: ByteArray, val salt: ByteArray, val iterations: Int) {
        fun matches(header: Header): Boolean =
            header.salt != null && header.salt.contentEquals(salt) && header.iterations == iterations
    }

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { random.nextBytes(it) }

    fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int = DEFAULT_ITERATIONS
    ): DerivedKey {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            return DerivedKey(factory.generateSecret(spec).encoded, salt.copyOf(), iterations)
        } finally {
            spec.clearPassword()
        }
    }

    /** Wraps a gzip'd payload into a `.paisa` container, encrypting when [key] is set. */
    fun seal(payload: ByteArray, key: DerivedKey?): ByteArray {
        val header = ByteArrayOutputStream()
        DataOutputStream(header).use { out ->
            out.write(MAGIC)
            out.writeByte(VERSION)
            out.writeByte(if (key != null) FLAG_ENCRYPTED else 0)
            if (key != null) {
                out.write(key.salt)
                out.writeInt(key.iterations)
            }
        }
        if (key == null) return header.toByteArray() + payload

        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        header.write(iv)
        val headerBytes = header.toByteArray()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(headerBytes)
        return headerBytes + cipher.doFinal(payload)
    }

    fun readHeader(data: ByteArray): Header {
        if (data.size < MAGIC.size + 2 || !data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw NotABackupException("Not a Paisa backup file")
        }
        val input = DataInputStream(data.inputStream(MAGIC.size, data.size - MAGIC.size))
        val version = input.readUnsignedByte()
        if (version > VERSION) {
            throw NotABackupException("This backup needs a newer version of Paisa")
        }
        val flags = input.readUnsignedByte()
        val encrypted = flags and FLAG_ENCRYPTED != 0
        if (!encrypted) {
            return Header(false, null, 0, null, data.copyOfRange(0, MAGIC.size + 2))
        }
        val headerLen = MAGIC.size + 2 + SALT_BYTES + 4 + IV_BYTES
        if (data.size < headerLen) throw NotABackupException("Backup file is truncated")
        val salt = ByteArray(SALT_BYTES).also { input.readFully(it) }
        val iterations = input.readInt()
        if (iterations !in 1..MAX_ITERATIONS) throw NotABackupException("Backup header is invalid")
        val iv = ByteArray(IV_BYTES).also { input.readFully(it) }
        return Header(true, salt, iterations, iv, data.copyOfRange(0, headerLen))
    }

    /** Returns the gzip'd payload. Throws [WrongPasswordException] on a bad key or tampering. */
    fun open(data: ByteArray, header: Header, key: DerivedKey?): ByteArray {
        val body = data.copyOfRange(header.bytes.size, data.size)
        if (!header.encrypted) return body
        requireNotNull(key) { "Encrypted backup needs a key" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key.key, "AES"),
            GCMParameterSpec(TAG_BITS, header.iv)
        )
        cipher.updateAAD(header.bytes)
        return try {
            cipher.doFinal(body)
        } catch (e: GeneralSecurityException) {
            throw WrongPasswordException()
        }
    }
}
