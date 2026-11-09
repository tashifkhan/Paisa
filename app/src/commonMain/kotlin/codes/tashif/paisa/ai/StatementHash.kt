package codes.tashif.paisa.ai

/*
 * MD5 as lowercase hex, the same function :parser-core's md5Hex computes,
 * so statement import hashes stay the same. parser-core is JVM-only, so this
 * copy lives in commonMain. The round constants are written out rather than
 * computed with sin(), so every platform gets the same bits.
 */

private val SHIFTS = intArrayOf(
    7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
    5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
    4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
    6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
)

private val K = intArrayOf(
    0xd76aa478.toInt(), 0xe8c7b756.toInt(), 0x242070db.toInt(), 0xc1bdceee.toInt(),
    0xf57c0faf.toInt(), 0x4787c62a.toInt(), 0xa8304613.toInt(), 0xfd469501.toInt(),
    0x698098d8.toInt(), 0x8b44f7af.toInt(), 0xffff5bb1.toInt(), 0x895cd7be.toInt(),
    0x6b901122.toInt(), 0xfd987193.toInt(), 0xa679438e.toInt(), 0x49b40821.toInt(),
    0xf61e2562.toInt(), 0xc040b340.toInt(), 0x265e5a51.toInt(), 0xe9b6c7aa.toInt(),
    0xd62f105d.toInt(), 0x02441453.toInt(), 0xd8a1e681.toInt(), 0xe7d3fbc8.toInt(),
    0x21e1cde6.toInt(), 0xc33707d6.toInt(), 0xf4d50d87.toInt(), 0x455a14ed.toInt(),
    0xa9e3e905.toInt(), 0xfcefa3f8.toInt(), 0x676f02d9.toInt(), 0x8d2a4c8a.toInt(),
    0xfffa3942.toInt(), 0x8771f681.toInt(), 0x6d9d6122.toInt(), 0xfde5380c.toInt(),
    0xa4beea44.toInt(), 0x4bdecfa9.toInt(), 0xf6bb4b60.toInt(), 0xbebfbc70.toInt(),
    0x289b7ec6.toInt(), 0xeaa127fa.toInt(), 0xd4ef3085.toInt(), 0x04881d05.toInt(),
    0xd9d4d039.toInt(), 0xe6db99e5.toInt(), 0x1fa27cf8.toInt(), 0xc4ac5665.toInt(),
    0xf4292244.toInt(), 0x432aff97.toInt(), 0xab9423a7.toInt(), 0xfc93a039.toInt(),
    0x655b59c3.toInt(), 0x8f0ccc92.toInt(), 0xffeff47d.toInt(), 0x85845dd1.toInt(),
    0x6fa87e4f.toInt(), 0xfe2ce6e0.toInt(), 0xa3014314.toInt(), 0x4e0811a1.toInt(),
    0xf7537e82.toInt(), 0xbd3af235.toInt(), 0x2ad7d2bb.toInt(), 0xeb86d391.toInt()
)

/** MD5 of the UTF-8 bytes of [input], as 32 lowercase hex digits. */
internal fun md5Hex(input: String): String =
    md5(input.encodeToByteArray()).joinToString("") { byte -> byte.toUByte().toString(16).padStart(2, '0') }

private fun md5(input: ByteArray): ByteArray {
    val bitLength = input.size.toLong() * 8L
    val paddedLength = ((input.size + 8) / 64 + 1) * 64
    val padded = ByteArray(paddedLength)
    input.copyInto(padded)
    padded[input.size] = 0x80.toByte()
    for (i in 0 until 8) {
        padded[paddedLength - 8 + i] = ((bitLength ushr (8 * i)) and 0xff).toByte()
    }

    var a0 = 0x67452301
    var b0 = 0xefcdab89.toInt()
    var c0 = 0x98badcfe.toInt()
    var d0 = 0x10325476

    val words = IntArray(16)
    var offset = 0
    while (offset < padded.size) {
        for (i in 0 until 16) {
            val index = offset + i * 4
            words[i] = (padded[index].toInt() and 0xff) or
                ((padded[index + 1].toInt() and 0xff) shl 8) or
                ((padded[index + 2].toInt() and 0xff) shl 16) or
                ((padded[index + 3].toInt() and 0xff) shl 24)
        }
        var a = a0
        var b = b0
        var c = c0
        var d = d0
        for (i in 0 until 64) {
            val f: Int
            val g: Int
            when {
                i < 16 -> { f = (b and c) or (b.inv() and d); g = i }
                i < 32 -> { f = (d and b) or (d.inv() and c); g = (5 * i + 1) % 16 }
                i < 48 -> { f = b xor c xor d; g = (3 * i + 5) % 16 }
                else -> { f = c xor (b or d.inv()); g = (7 * i) % 16 }
            }
            val temp = d
            d = c
            c = b
            b += (a + f + K[i] + words[g]).rotateLeft(SHIFTS[i])
            a = temp
        }
        a0 += a
        b0 += b
        c0 += c
        d0 += d
        offset += 64
    }

    return byteArrayOf(
        a0.toByte(), (a0 ushr 8).toByte(), (a0 ushr 16).toByte(), (a0 ushr 24).toByte(),
        b0.toByte(), (b0 ushr 8).toByte(), (b0 ushr 16).toByte(), (b0 ushr 24).toByte(),
        c0.toByte(), (c0 ushr 8).toByte(), (c0 ushr 16).toByte(), (c0 ushr 24).toByte(),
        d0.toByte(), (d0 ushr 8).toByte(), (d0 ushr 16).toByte(), (d0 ushr 24).toByte()
    )
}
