package codes.tashif.paisa.shared

/**
 * The crypto the container needs. PBKDF2 encodes the password as UTF-8 on
 * both platforms, which is what Java's `PBEKeySpec` does too.
 */
expect object Crypto {
    fun randomBytes(size: Int): ByteArray

    suspend fun pbkdf2Sha256(password: String, salt: ByteArray, iterations: Int): ByteArray

    suspend fun aesGcmEncrypt(key: ByteArray, iv: ByteArray, aad: ByteArray, plain: ByteArray): ByteArray

    /** Null when authentication fails, which means a wrong key or a changed file. */
    suspend fun aesGcmDecrypt(key: ByteArray, iv: ByteArray, aad: ByteArray, sealed: ByteArray): ByteArray?
}

expect object Gzip {
    suspend fun compress(data: ByteArray): ByteArray

    suspend fun decompress(data: ByteArray): ByteArray
}
