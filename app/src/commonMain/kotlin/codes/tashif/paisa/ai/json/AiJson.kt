package codes.tashif.paisa.ai.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/*
 * A small common stand-in for the org.json classes the AI code used on
 * Android. It keeps Android's org.json rules so requests and parsing stay as
 * they were:
 * - keys keep insertion order;
 * - parsed integers become Int (or Long when too big), other numbers Double;
 * - the opt and get accessors coerce numbers, numeric strings and "true"/"false";
 * - optString on a JSON null gives "null", as Android's org.json does;
 * - whole doubles print without a fraction (1.0 prints as 1);
 * - parsing reads the first value and ignores text after it.
 * Parsing and printing go through kotlinx.serialization.
 */

/** Thrown for text that isn't JSON, a missing key or a value of the wrong type. */
class JSONException(message: String, cause: Throwable? = null) : Exception(message, cause)

class JSONObject private constructor(private val map: LinkedHashMap<String, Any>) {

    constructor() : this(LinkedHashMap())

    /** Parses [text], which must hold a JSON object. */
    constructor(text: String) : this(
        (JSONTokener(text).nextValue() as? JSONObject)?.map
            ?: throw JSONException("Value is not a JSON object")
    )

    /** Copies [values], turning lists and maps into JSON arrays and objects and null into [NULL]. */
    constructor(values: Map<*, *>) : this() {
        for ((key, value) in values) {
            if (key == null) throw JSONException("Null key")
            map[key.toString()] = wrap(value)
        }
    }

    /** Sets [name] to [value], or removes it when [value] is null. */
    fun put(name: String, value: Any?): JSONObject {
        if (value == null) {
            map.remove(name)
            return this
        }
        map[name] = checked(wrap(value))
        return this
    }

    fun remove(name: String): Any? = map.remove(name)

    fun has(name: String): Boolean = map.containsKey(name)

    fun isNull(name: String): Boolean = map[name].let { it == null || it === NULL }

    fun length(): Int = map.size

    fun keys(): Iterator<String> = map.keys.toList().iterator()

    fun opt(name: String): Any? = map[name]

    fun get(name: String): Any = map[name] ?: throw JSONException("No value for $name")

    fun optString(name: String, fallback: String = ""): String = asString(opt(name)) ?: fallback

    fun getString(name: String): String = asString(get(name)) ?: throw mismatch(name, "String")

    fun optInt(name: String, fallback: Int = 0): Int = asInt(opt(name)) ?: fallback

    fun getInt(name: String): Int = asInt(get(name)) ?: throw mismatch(name, "int")

    fun optLong(name: String, fallback: Long = 0L): Long = asLong(opt(name)) ?: fallback

    fun getLong(name: String): Long = asLong(get(name)) ?: throw mismatch(name, "long")

    fun optDouble(name: String, fallback: Double = Double.NaN): Double = asDouble(opt(name)) ?: fallback

    fun getDouble(name: String): Double = asDouble(get(name)) ?: throw mismatch(name, "double")

    fun optBoolean(name: String, fallback: Boolean = false): Boolean = asBoolean(opt(name)) ?: fallback

    fun getBoolean(name: String): Boolean = asBoolean(get(name)) ?: throw mismatch(name, "boolean")

    fun optJSONObject(name: String): JSONObject? = opt(name) as? JSONObject

    fun getJSONObject(name: String): JSONObject = get(name) as? JSONObject ?: throw mismatch(name, "JSONObject")

    fun optJSONArray(name: String): JSONArray? = opt(name) as? JSONArray

    fun getJSONArray(name: String): JSONArray = get(name) as? JSONArray ?: throw mismatch(name, "JSONArray")

    internal fun toElement(): JsonObject = JsonObject(map.mapValues { (_, value) -> toElement(value) })

    /** Compact JSON, keys in insertion order. */
    override fun toString(): String = toElement().toString()

    companion object {
        /** JSON null as a value, so [has] can tell it from a missing key. */
        val NULL: Any = JsonNullValue
    }
}

class JSONArray private constructor(private val list: MutableList<Any>) {

    constructor() : this(ArrayList())

    /** Parses [text], which must hold a JSON array. */
    constructor(text: String) : this(
        (JSONTokener(text).nextValue() as? JSONArray)?.list
            ?: throw JSONException("Value is not a JSON array")
    )

    /** Copies [values], turning lists and maps into JSON arrays and objects. */
    constructor(values: Collection<*>) : this(values.mapTo(ArrayList()) { wrap(it) })

    /** Appends [value]; null appends a JSON null. */
    fun put(value: Any?): JSONArray {
        list.add(checked(wrap(value)))
        return this
    }

    fun length(): Int = list.size

    fun opt(index: Int): Any? = list.getOrNull(index)

    fun get(index: Int): Any = list.getOrNull(index) ?: throw JSONException("Index $index out of range [0..${list.size})")

    fun optString(index: Int, fallback: String = ""): String = asString(opt(index)) ?: fallback

    fun getString(index: Int): String = asString(get(index)) ?: throw mismatch(index, "String")

    fun optInt(index: Int, fallback: Int = 0): Int = asInt(opt(index)) ?: fallback

    fun getInt(index: Int): Int = asInt(get(index)) ?: throw mismatch(index, "int")

    fun optLong(index: Int, fallback: Long = 0L): Long = asLong(opt(index)) ?: fallback

    fun optDouble(index: Int, fallback: Double = Double.NaN): Double = asDouble(opt(index)) ?: fallback

    fun getDouble(index: Int): Double = asDouble(get(index)) ?: throw mismatch(index, "double")

    fun optBoolean(index: Int, fallback: Boolean = false): Boolean = asBoolean(opt(index)) ?: fallback

    fun getBoolean(index: Int): Boolean = asBoolean(get(index)) ?: throw mismatch(index, "boolean")

    fun optJSONObject(index: Int): JSONObject? = opt(index) as? JSONObject

    fun getJSONObject(index: Int): JSONObject = get(index) as? JSONObject ?: throw mismatch(index, "JSONObject")

    fun optJSONArray(index: Int): JSONArray? = opt(index) as? JSONArray

    fun getJSONArray(index: Int): JSONArray = get(index) as? JSONArray ?: throw mismatch(index, "JSONArray")

    internal fun toElement(): JsonArray = JsonArray(list.map(::toElement))

    override fun toString(): String = toElement().toString()
}

/**
 * Reads one JSON value from text: a [JSONObject], [JSONArray], String,
 * Boolean, number or [JSONObject.NULL]. Text after the first object or
 * array is ignored.
 */
class JSONTokener(private val text: String) {

    fun nextValue(): Any {
        val element = try {
            lenient.parseToJsonElement(firstValue(text))
        } catch (e: IllegalArgumentException) {
            // kotlinx's SerializationException is an IllegalArgumentException.
            throw JSONException(e.message ?: "Malformed JSON", e)
        }
        return fromElement(element)
    }

    private companion object {
        @OptIn(ExperimentalSerializationApi::class)
        val lenient = Json {
            isLenient = true
            allowComments = true
        }

        /** The first object or array in [text] when it starts with one, otherwise [text]. */
        fun firstValue(text: String): String {
            val start = text.indexOfFirst { !it.isWhitespace() }
            if (start < 0) throw JSONException("End of input")
            val open = text[start]
            if (open != '{' && open != '[') return text
            var depth = 0
            var inString = false
            var escape = false
            for (i in start until text.length) {
                val c = text[i]
                if (inString) {
                    when {
                        escape -> escape = false
                        c == '\\' -> escape = true
                        c == '"' -> inString = false
                    }
                    continue
                }
                when (c) {
                    '"' -> inString = true
                    '{', '[' -> depth++
                    '}', ']' -> {
                        depth--
                        if (depth == 0) return text.substring(start, i + 1)
                    }
                }
            }
            return text
        }
    }
}

private object JsonNullValue {
    override fun toString(): String = "null"
}

private fun mismatch(name: Any, type: String) = JSONException("Value at $name is not a $type")

/** Lists, arrays and maps become JSON arrays and objects; null becomes [JSONObject.NULL]. */
private fun wrap(value: Any?): Any = when (value) {
    null -> JSONObject.NULL
    is JSONObject, is JSONArray, is String, is Boolean, is Number, JsonNullValue -> value
    is Char -> value.toString()
    is Collection<*> -> JSONArray(value)
    is Array<*> -> JSONArray(value.toList())
    is Map<*, *> -> JSONObject(value)
    else -> value.toString()
}

private fun checked(value: Any): Any {
    if (value is Double && !value.isFinite()) throw JSONException("Forbidden numeric value: $value")
    if (value is Float && !value.isFinite()) throw JSONException("Forbidden numeric value: $value")
    return value
}

private fun asString(value: Any?): String? = when (value) {
    null -> null
    is String -> value
    else -> value.toString()
}

private fun asBoolean(value: Any?): Boolean? = when (value) {
    is Boolean -> value
    is String -> when {
        value.equals("true", ignoreCase = true) -> true
        value.equals("false", ignoreCase = true) -> false
        else -> null
    }
    else -> null
}

private fun asDouble(value: Any?): Double? = when (value) {
    is Double -> value
    is Number -> value.toDouble()
    is String -> value.trim().toDoubleOrNull()
    else -> null
}

private fun asInt(value: Any?): Int? = when (value) {
    is Int -> value
    is Number -> value.toInt()
    is String -> value.trim().toDoubleOrNull()?.toInt()
    else -> null
}

private fun asLong(value: Any?): Long? = when (value) {
    is Long -> value
    is Number -> value.toLong()
    is String -> value.trim().toDoubleOrNull()?.toLong()
    else -> null
}

private fun toElement(value: Any): JsonElement = when (value) {
    is JSONObject -> value.toElement()
    is JSONArray -> value.toElement()
    JsonNullValue -> JsonNull
    is String -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is Int, is Long, is Short, is Byte -> JsonPrimitive((value as Number).toLong())
    is Number -> {
        // A whole double prints as a whole number, like Android's org.json.
        val d = value.toDouble()
        val whole = d.toLong()
        if (d == whole.toDouble()) JsonPrimitive(whole) else JsonPrimitive(d)
    }
    else -> JsonPrimitive(value.toString())
}

private fun fromElement(element: JsonElement): Any = when (element) {
    is JsonObject -> JSONObject().also { o -> element.forEach { (k, v) -> o.put(k, fromElement(v)) } }
    is JsonArray -> JSONArray().also { a -> element.forEach { a.put(fromElement(it)) } }
    JsonNull -> JSONObject.NULL
    is JsonPrimitive -> if (element.isString) element.content else literal(element.content)
}

/** An unquoted literal: a boolean, an Int, Long or Double, or (lenient) a bare string. */
private fun literal(text: String): Any {
    when (text) {
        "true" -> return true
        "false" -> return false
        "null" -> return JSONObject.NULL
    }
    if ('.' !in text) {
        text.toLongOrNull()?.let { n -> return if (n in Int.MIN_VALUE..Int.MAX_VALUE) n.toInt() else n }
    }
    return text.toDoubleOrNull() ?: text
}
