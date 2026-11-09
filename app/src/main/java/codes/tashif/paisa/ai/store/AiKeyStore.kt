package codes.tashif.paisa.ai.store

import codes.tashif.paisa.ai.AiCredentialsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * One API key per provider id, in the same EncryptedSharedPreferences file
 * the old single key used. Backup rules already exclude that file, so keys
 * stay on the device. Never log a key.
 */
class AiKeyStore(legacy: AiCredentialsStore) {

    private val prefs = legacy.prefs

    private val _withKeys = MutableStateFlow(readIds())

    /** Provider ids that have a key saved. */
    val withKeys: StateFlow<Set<String>> = _withKeys.asStateFlow()

    fun key(providerId: String): String? =
        prefs.getString(PREFIX + providerId, null)?.takeIf { it.isNotBlank() }

    fun hasKey(providerId: String): Boolean = providerId in _withKeys.value

    /** Saves, or with a blank [key] removes, a key. Returns whether the write reached disk. */
    suspend fun save(providerId: String, key: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return@withContext remove(providerId)
        val saved = prefs.edit().putString(PREFIX + providerId, trimmed).commit()
        _withKeys.value = readIds()
        saved
    }

    suspend fun remove(providerId: String): Boolean = withContext(Dispatchers.IO) {
        val saved = prefs.edit().remove(PREFIX + providerId).commit()
        _withKeys.value = readIds()
        saved
    }

    private fun readIds(): Set<String> =
        prefs.all.entries
            .filter { (name, value) -> name.startsWith(PREFIX) && (value as? String)?.isNotBlank() == true }
            .mapTo(HashSet()) { it.key.removePrefix(PREFIX) }

    private companion object {
        const val PREFIX = "key."
    }
}
