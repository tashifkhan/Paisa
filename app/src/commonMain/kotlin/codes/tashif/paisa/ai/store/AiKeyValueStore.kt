package codes.tashif.paisa.ai.store

/**
 * A small string key-value file. On Android it wraps the SharedPreferences
 * files the AI settings always used; on the web it is localStorage.
 */
interface AiKeyValueStore {
    fun getString(key: String): String?

    fun contains(key: String): Boolean

    /** Every entry whose value is a string. */
    fun strings(): Map<String, String>

    /**
     * Writes [changes] in one go. A null value removes its key. Returns
     * whether the write reached storage.
     */
    fun commit(changes: Map<String, String?>): Boolean
}
