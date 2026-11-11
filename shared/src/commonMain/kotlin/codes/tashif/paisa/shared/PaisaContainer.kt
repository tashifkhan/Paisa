package codes.tashif.paisa.shared

/**
 * The `.paisa` container that wraps both backups and sync change files.
 * Byte for byte the same as `BackupCrypto` in the Android app:
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
 * Everything that touches crypto is `suspend`, because WebCrypto only has an
 * async API.
 */
object PaisaContainer {
    private val MAGIC = "PAISABAK".encodeToByteArray()
    private const val VERSION = 1
    private const val FLAG_ENCRYPTED = 1
    const val SALT_BYTES = 16
    const val IV_BYTES = 12
    const val DEFAULT_ITERATIONS = 210_000

    /** Upper bound on iterations read from an untrusted header. */
    private const val MAX_ITERATIONS = 5_000_000

    private val PLAIN_HEADER_BYTES = MAGIC.size + 2
    private val ENCRYPTED_HEADER_BYTES = PLAIN_HEADER_BYTES + SALT_BYTES + 4 + IV_BYTES

    class Header(
        val encrypted: Boolean,
        val salt: ByteArray?,
        val iterations: Int,
        val iv: ByteArray?,
        /** Raw header bytes, authenticated as GCM associated data. */
        val bytes: ByteArray
    )

    /** A derived AES key plus the salt and iterations it came from. */
    class Key(val bytes: ByteArray, val salt: ByteArray, val iterations: Int) {
        fun matches(header: Header): Boolean =
            header.salt != null && header.salt.contentEquals(salt) && header.iterations == iterations
    }

    suspend fun deriveKey(
        password: String,
        salt: ByteArray = Crypto.randomBytes(SALT_BYTES),
        iterations: Int = DEFAULT_ITERATIONS
    ): Key = Key(Crypto.pbkdf2Sha256(password, salt, iterations), salt.copyOf(), iterations)

    fun readHeader(data: ByteArray): Header {
        if (data.size < PLAIN_HEADER_BYTES || !data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw NotAPaisaFileException("Not a Paisa backup or sync file")
        }
        val version = data[MAGIC.size].toInt() and 0xFF
        if (version > VERSION) throw NotAPaisaFileException("This file needs a newer version of Paisa")
        val flags = data[MAGIC.size + 1].toInt() and 0xFF
        if (flags and FLAG_ENCRYPTED == 0) {
            return Header(false, null, 0, null, data.copyOfRange(0, PLAIN_HEADER_BYTES))
        }
        if (data.size < ENCRYPTED_HEADER_BYTES) throw NotAPaisaFileException("File is truncated")
        var at = PLAIN_HEADER_BYTES
        val salt = data.copyOfRange(at, at + SALT_BYTES).also { at += SALT_BYTES }
        val iterations = ((data[at].toInt() and 0xFF) shl 24) or
            ((data[at + 1].toInt() and 0xFF) shl 16) or
            ((data[at + 2].toInt() and 0xFF) shl 8) or
            (data[at + 3].toInt() and 0xFF)
        at += 4
        if (iterations !in 1..MAX_ITERATIONS) throw NotAPaisaFileException("File header is invalid")
        val iv = data.copyOfRange(at, at + IV_BYTES)
        return Header(true, salt, iterations, iv, data.copyOfRange(0, ENCRYPTED_HEADER_BYTES))
    }

    /** Wraps a gzip'd payload, encrypting it when [key] is set. */
    suspend fun seal(payload: ByteArray, key: Key?): ByteArray {
        val plain = MAGIC + byteArrayOf(VERSION.toByte(), (if (key != null) FLAG_ENCRYPTED else 0).toByte())
        if (key == null) return plain + payload
        val iv = Crypto.randomBytes(IV_BYTES)
        val n = key.iterations
        val header = plain + key.salt +
            byteArrayOf((n ushr 24).toByte(), (n ushr 16).toByte(), (n ushr 8).toByte(), n.toByte()) + iv
        return header + Crypto.aesGcmEncrypt(key.bytes, iv, header, payload)
    }

    /**
     * Returns the gzip'd payload. Throws [WrongPasswordException] when the key
     * is wrong or the file was changed.
     */
    suspend fun open(data: ByteArray, header: Header, key: Key?): ByteArray {
        val body = data.copyOfRange(header.bytes.size, data.size)
        if (!header.encrypted) return body
        requireNotNull(key) { "An encrypted file needs a key" }
        return Crypto.aesGcmDecrypt(key.bytes, header.iv!!, header.bytes, body)
            ?: throw WrongPasswordException()
    }

    /** Reads the header, derives the key from [password] and returns the JSON inside. */
    suspend fun openJson(data: ByteArray, password: String?): String {
        val header = readHeader(data)
        val key = if (header.encrypted) {
            requireNotNull(password) { "This file is encrypted" }
            deriveKey(password, header.salt!!, header.iterations)
        } else {
            null
        }
        return Gzip.decompress(open(data, header, key)).decodeToString()
    }
}

class NotAPaisaFileException(message: String) : Exception(message)

class WrongPasswordException : Exception("Wrong password")
