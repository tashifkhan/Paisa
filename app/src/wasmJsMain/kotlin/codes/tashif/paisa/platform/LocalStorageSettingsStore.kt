package codes.tashif.paisa.platform

import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * [SettingsStore] in localStorage under `<name>.`. Values are stored as text.
 * localStorage is readable by any script on the page and isn't encrypted;
 * the page loads no third-party scripts.
 */
class LocalStorageSettingsStore(private val name: String) : SettingsStore {
    private val changed = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    private fun k(key: String) = "$name.$key"

    override fun getString(key: String, default: String?): String? = localStorage.getItem(k(key)) ?: default
    override fun getBoolean(key: String, default: Boolean): Boolean =
        localStorage.getItem(k(key))?.toBooleanStrictOrNull() ?: default
    override fun getLong(key: String, default: Long): Long = localStorage.getItem(k(key))?.toLongOrNull() ?: default
    override fun getInt(key: String, default: Int): Int = localStorage.getItem(k(key))?.toIntOrNull() ?: default

    override fun edit(): SettingsStore.Editor = object : SettingsStore.Editor {
        private val pending = linkedMapOf<String, String?>()
        override fun putString(key: String, value: String?) = apply { pending[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value.toString() }
        override fun putLong(key: String, value: Long) = apply { pending[key] = value.toString() }
        override fun putInt(key: String, value: Int) = apply { pending[key] = value.toString() }
        override fun remove(key: String) = apply { pending[key] = null }
        override fun apply() {
            commit()
        }
        override fun commit(): Boolean = try {
            for ((key, value) in pending) {
                if (value == null) localStorage.removeItem(k(key)) else localStorage.setItem(k(key), value)
            }
            changed.tryEmit(Unit)
            true
        } catch (e: Throwable) {
            false
        }
    }

    override fun changes(): Flow<Unit> = changed.asSharedFlow()
}
