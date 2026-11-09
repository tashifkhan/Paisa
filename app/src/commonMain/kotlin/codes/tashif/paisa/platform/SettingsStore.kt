package codes.tashif.paisa.platform

import kotlinx.coroutines.flow.Flow

/**
 * Small typed key-value settings, like SharedPreferences. Android backs it
 * with SharedPreferences, so existing files and keys keep working; the web
 * backs it with localStorage.
 */
interface SettingsStore {
    fun getString(key: String, default: String? = null): String?
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getLong(key: String, default: Long): Long
    fun getInt(key: String, default: Int): Int

    fun edit(): Editor

    /** Emits after any change, from this process or another writer. */
    fun changes(): Flow<Unit>

    interface Editor {
        fun putString(key: String, value: String?): Editor
        fun putBoolean(key: String, value: Boolean): Editor
        fun putLong(key: String, value: Long): Editor
        fun putInt(key: String, value: Int): Editor
        fun remove(key: String): Editor

        /** Writes in the background. */
        fun apply()

        /** Writes now. Returns whether it reached storage. */
        fun commit(): Boolean
    }
}
