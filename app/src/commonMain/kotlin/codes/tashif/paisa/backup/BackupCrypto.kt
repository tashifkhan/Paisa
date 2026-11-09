package codes.tashif.paisa.backup

import codes.tashif.paisa.shared.Crypto
import codes.tashif.paisa.shared.NotAPaisaFileException
import codes.tashif.paisa.shared.PaisaContainer
import kotlinx.io.IOException

/**
 * The `.paisa` container for backups and sync files. The byte format lives in
 * `:shared` ([PaisaContainer]) so the phone and the web read the same files:
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
    const val SALT_BYTES = PaisaContainer.SALT_BYTES
    const val DEFAULT_ITERATIONS = PaisaContainer.DEFAULT_ITERATIONS

    class WrongPasswordException : IOException("Wrong backup password")
    class NotABackupException(message: String) : IOException(message)

    class Header internal constructor(internal val inner: PaisaContainer.Header) {
        val encrypted: Boolean get() = inner.encrypted
        val salt: ByteArray? get() = inner.salt
        val iterations: Int get() = inner.iterations
        val iv: ByteArray? get() = inner.iv

        /** Raw header bytes, authenticated as GCM associated data. */
        val bytes: ByteArray get() = inner.bytes
    }

    /** A derived key plus the salt/iterations it came from. */
    data class DerivedKey(val key: ByteArray, val salt: ByteArray, val iterations: Int) {
        fun matches(header: Header): Boolean =
            header.salt != null && header.salt!!.contentEquals(salt) && header.iterations == iterations

        internal fun toShared() = PaisaContainer.Key(key, salt, iterations)

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
    ): DerivedKey {
        val key = PaisaContainer.deriveKey(password.concatToString(), salt, iterations)
        return DerivedKey(key.bytes, key.salt, key.iterations)
    }

    /** Wraps a gzip'd payload into a `.paisa` container, encrypting when [key] is set. */
    suspend fun seal(payload: ByteArray, key: DerivedKey?): ByteArray =
        PaisaContainer.seal(payload, key?.toShared())

    fun readHeader(data: ByteArray): Header = try {
        Header(PaisaContainer.readHeader(data))
    } catch (e: NotAPaisaFileException) {
        throw NotABackupException(e.message ?: "Not a Paisa backup file")
    }

    /** Returns the gzip'd payload. Throws [WrongPasswordException] on a bad key or tampering. */
    suspend fun open(data: ByteArray, header: Header, key: DerivedKey?): ByteArray = try {
        PaisaContainer.open(data, header.inner, key?.toShared())
    } catch (e: codes.tashif.paisa.shared.WrongPasswordException) {
        throw WrongPasswordException()
    }
}
