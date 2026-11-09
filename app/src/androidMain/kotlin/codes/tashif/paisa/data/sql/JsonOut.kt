package codes.tashif.paisa.data.sql

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * A small streaming JSON writer for sync and backup files, with the same
 * output shape as `android.util.JsonWriter`. Doubles are written in their
 * shortest form, so 450.0 may come out as `450` on the web. Readers treat
 * both the same.
 */
class JsonOut {
    private val sb = StringBuilder()

    /** Per open object or array: whether it already has an item. */
    private val hasItems = ArrayDeque<Boolean>()

    /** True right after a key, when the value must not get a comma. */
    private var afterName = false

    private fun beforeItem() {
        if (afterName) {
            afterName = false
            return
        }
        if (hasItems.isNotEmpty()) {
            if (hasItems.last()) sb.append(',')
            hasItems[hasItems.size - 1] = true
        }
    }

    fun beginObject(): JsonOut = apply { beforeItem(); sb.append('{'); hasItems.addLast(false) }
    fun endObject(): JsonOut = apply { hasItems.removeLast(); sb.append('}') }
    fun beginArray(): JsonOut = apply { beforeItem(); sb.append('['); hasItems.addLast(false) }
    fun endArray(): JsonOut = apply { hasItems.removeLast(); sb.append(']') }

    fun name(key: String): JsonOut = apply {
        beforeItem()
        string(key)
        sb.append(':')
        afterName = true
    }

    fun value(v: String?): JsonOut = apply { beforeItem(); if (v == null) sb.append("null") else string(v) }
    fun value(v: Long): JsonOut = apply { beforeItem(); sb.append(v) }
    fun value(v: Boolean): JsonOut = apply { beforeItem(); sb.append(v) }
    fun value(v: Double): JsonOut = apply {
        beforeItem()
        if (v.isNaN() || v.isInfinite()) sb.append("null") else sb.append(v.toString())
    }
    fun nullValue(): JsonOut = apply { beforeItem(); sb.append("null") }

    /** Writes a SQLite value as read by [SqlRow]. Blobs become `{"b64": …}`. */
    fun sqlValue(v: Any?): JsonOut = when (v) {
        null -> nullValue()
        is Long -> value(v)
        is Double -> value(v)
        is ByteArray -> beginObject().name("b64").value(kotlin.io.encoding.Base64.encode(v)).endObject()
        else -> value(v.toString())
    }

    private fun string(s: String) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                ' ' -> sb.append("\\u2028")
                ' ' -> sb.append("\\u2029")
                else -> if (c < ' ') {
                    sb.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                } else {
                    sb.append(c)
                }
            }
        }
        sb.append('"')
    }

    override fun toString(): String = sb.toString()
}

/**
 * A JSON scalar as Kotlin: null, Boolean, Long, Double or String. Numbers
 * without a fraction come back as Long, like `android.util.JsonReader` code
 * that tried `toLongOrNull()` first.
 */
fun JsonElement.toScalar(): Any? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> when {
        isString -> content
        booleanOrNull != null -> booleanOrNull
        else -> content.toLongOrNull() ?: content.toDoubleOrNull()
    }
    is JsonObject -> (this["b64"] as? JsonPrimitive)?.content?.let { kotlin.io.encoding.Base64.decode(it) }
    is JsonArray -> null
}
