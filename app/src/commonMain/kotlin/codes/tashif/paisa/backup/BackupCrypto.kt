package codes.tashif.paisa.backup

import kotlinx.io.IOException

/**
 * The `.paisa` container for backups and sync files. The phone and the web
 * read and write the same bytes:
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
 *
 * Key derivation and the cipher suspend, because WebCrypto is asynchronous.
 */
object BackupCrypto {
    private val MAGIC = "PAISABAK".encodeToByteArray()
    private const val VERSION = 1
    private const val FLAG_ENCRYPTED = 1
    const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    const val DEFAULT_ITERATIONS = 210_000

    /** Upper bound on iterations read from an untrusted header. */
    private const val MAX_ITERATIONS = 5_000_000

    private val PLAIN_HEADER_BYTES = MAGIC.size + 2
    private val ENCRYPTED_HEADER_BYTES = PLAIN_HEADER_BYTES + SALT_BYTES + 4 + IV_BYTES

    class WrongPasswordException : IOException("Wrong backup password")
    class NotABackupException(message: String) : IOException(message)

    class Header internal constructor(
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

        override fun equals(other: Any?): Boolean =
            other is DerivedKey && key.contentEquals(other.key) && salt.contentEquals(other.salt) &&
                iterations == other.iterations

        override fun hashCode(): Int = key.contentHashCode() * 31 + iterations
    }

    fun newSalt(): ByteArray = Crypto.randomBytes(SALT_BYTES)

    suspend fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int = DEFAULT_ITERATIONS
    ): DerivedKey =
        DerivedKey(Crypto.pbkdf2Sha256(password.concatToString(), salt, iterations), salt.copyOf(), iterations)

    /** Wraps a gzip'd payload into a `.paisa` container, encrypting when [key] is set. */
    suspend fun seal(payload: ByteArray, key: DerivedKey?): ByteArray {
        val plain = MAGIC + byteArrayOf(VERSION.toByte(), (if (key != null) FLAG_ENCRYPTED else 0).toByte())
        if (key == null) return plain + payload
        val iv = Crypto.randomBytes(IV_BYTES)
        val n = key.iterations
        val header = plain + key.salt +
            byteArrayOf((n ushr 24).toByte(), (n ushr 16).toByte(), (n ushr 8).toByte(), n.toByte()) + iv
        return header + Crypto.aesGcmEncrypt(key.key, iv, header, payload)
    }

    fun readHeader(data: ByteArray): Header {
        if (data.size < PLAIN_HEADER_BYTES || !data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw NotABackupException("Not a Paisa backup or sync file")
        }
        val version = data[MAGIC.size].toInt() and 0xFF
        if (version > VERSION) throw NotABackupException("This file needs a newer version of Paisa")
        val flags = data[MAGIC.size + 1].toInt() and 0xFF
        if (flags and FLAG_ENCRYPTED == 0) {
            return Header(false, null, 0, null, data.copyOfRange(0, PLAIN_HEADER_BYTES))
        }
        if (data.size < ENCRYPTED_HEADER_BYTES) throw NotABackupException("File is truncated")
        var at = PLAIN_HEADER_BYTES
        val salt = data.copyOfRange(at, at + SALT_BYTES).also { at += SALT_BYTES }
        val iterations = ((data[at].toInt() and 0xFF) shl 24) or
            ((data[at + 1].toInt() and 0xFF) shl 16) or
            ((data[at + 2].toInt() and 0xFF) shl 8) or
            (data[at + 3].toInt() and 0xFF)
        at += 4
        if (iterations !in 1..MAX_ITERATIONS) throw NotABackupException("File header is invalid")
        val iv = data.copyOfRange(at, at + IV_BYTES)
        return Header(true, salt, iterations, iv, data.copyOfRange(0, ENCRYPTED_HEADER_BYTES))
    }

    /** Returns the gzip'd payload. Throws [WrongPasswordException] on a bad key or tampering. */
    suspend fun open(data: ByteArray, header: Header, key: DerivedKey?): ByteArray {
        val body = data.copyOfRange(header.bytes.size, data.size)
        if (!header.encrypted) return body
        requireNotNull(key) { "An encrypted file needs a key" }
        return Crypto.aesGcmDecrypt(key.key, header.iv!!, header.bytes, body)
            ?: throw WrongPasswordException()
    }
}
