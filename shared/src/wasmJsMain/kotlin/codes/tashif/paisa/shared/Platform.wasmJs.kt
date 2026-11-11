package codes.tashif.paisa.shared

import kotlinx.coroutines.await
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.js.Promise

// WebCrypto and the Compression Streams API. Both exist in every current
// browser and in Node 18+, which runs the tests.

private fun jsRandomBytes(size: Int): Int8Array =
    js("globalThis.crypto.getRandomValues(new Int8Array(size))")

private fun jsPbkdf2(password: String, salt: Int8Array, iterations: Int): Promise<Int8Array> = js(
    """
    globalThis.crypto.subtle.importKey('raw', new TextEncoder().encode(password), 'PBKDF2', false, ['deriveBits'])
      .then(k => globalThis.crypto.subtle.deriveBits(
        { name: 'PBKDF2', salt: salt, iterations: iterations, hash: 'SHA-256' }, k, 256))
      .then(bits => new Int8Array(bits))
    """
)

private fun jsAesGcm(encrypt: Boolean, key: Int8Array, iv: Int8Array, aad: Int8Array, data: Int8Array): Promise<Int8Array?> = js(
    """
    globalThis.crypto.subtle.importKey('raw', key, 'AES-GCM', false, [encrypt ? 'encrypt' : 'decrypt'])
      .then(k => {
        const params = { name: 'AES-GCM', iv: iv, additionalData: aad, tagLength: 128 };
        return encrypt ? globalThis.crypto.subtle.encrypt(params, k, data)
                       : globalThis.crypto.subtle.decrypt(params, k, data);
      })
      .then(out => new Int8Array(out), err => {
        if (err && err.name === 'OperationError') return null;
        throw err;
      })
    """
)

private fun jsGzip(compress: Boolean, data: Int8Array): Promise<Int8Array> = js(
    """
    new Response(new Blob([data]).stream().pipeThrough(
      compress ? new CompressionStream('gzip') : new DecompressionStream('gzip')))
      .arrayBuffer()
      .then(out => new Int8Array(out))
    """
)

internal fun ByteArray.toInt8Array(): Int8Array {
    val out = Int8Array(size)
    for (i in indices) out[i] = this[i]
    return out
}

internal fun Int8Array.toByteArray(): ByteArray = ByteArray(length) { this[it] }

actual object Crypto {
    actual fun randomBytes(size: Int): ByteArray = jsRandomBytes(size).toByteArray()

    actual suspend fun pbkdf2Sha256(password: String, salt: ByteArray, iterations: Int): ByteArray =
        jsPbkdf2(password, salt.toInt8Array(), iterations).await<Int8Array>().toByteArray()

    actual suspend fun aesGcmEncrypt(key: ByteArray, iv: ByteArray, aad: ByteArray, plain: ByteArray): ByteArray =
        jsAesGcm(true, key.toInt8Array(), iv.toInt8Array(), aad.toInt8Array(), plain.toInt8Array())
            .await<Int8Array?>()!!.toByteArray()

    actual suspend fun aesGcmDecrypt(key: ByteArray, iv: ByteArray, aad: ByteArray, sealed: ByteArray): ByteArray? =
        jsAesGcm(false, key.toInt8Array(), iv.toInt8Array(), aad.toInt8Array(), sealed.toInt8Array())
            .await<Int8Array?>()?.toByteArray()
}

actual object Gzip {
    actual suspend fun compress(data: ByteArray): ByteArray =
        jsGzip(true, data.toInt8Array()).await<Int8Array>().toByteArray()

    actual suspend fun decompress(data: ByteArray): ByteArray =
        jsGzip(false, data.toInt8Array()).await<Int8Array>().toByteArray()
}
