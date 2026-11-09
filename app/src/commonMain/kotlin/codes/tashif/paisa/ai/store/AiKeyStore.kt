package codes.tashif.paisa.ai.store

import codes.tashif.paisa.ai.AiCredentialsStore
import codes.tashif.paisa.ai.aiIoDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * One API key per provider id, in the same store the old single key used:
 * the EncryptedSharedPreferences file on Android, which backup rules already
 * exclude, so keys stay on the device. Never log a key.
 */
class AiKeyStore(legacy: AiCredentialsStore) {

    private val prefs = legacy.store

    private val _withKeys = MutableStateFlow(readIds())

    /** Provider ids that have a key saved. */
    val withKeys: StateFlow<Set<String>> = _withKeys.asStateFlow()

    fun key(providerId: String): String? =
        prefs.getString(PREFIX + providerId)?.takeIf { it.isNotBlank() }

    fun hasKey(providerId: String): Boolean = providerId in _withKeys.value

    /** Saves, or with a blank [key] removes, a key. Returns whether the write reached disk. */
    suspend fun save(providerId: String, key: String): Boolean = withContext(aiIoDispatcher) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return@withContext remove(providerId)
        val saved = prefs.commit(mapOf(PREFIX + providerId to trimmed))
        _withKeys.value = readIds()
        saved
    }

    suspend fun remove(providerId: String): Boolean = withContext(aiIoDispatcher) {
        val saved = prefs.commit(mapOf(PREFIX + providerId to null))
        _withKeys.value = readIds()
        saved
    }

    private fun readIds(): Set<String> =
        prefs.strings().entries
            .filter { (name, value) -> name.startsWith(PREFIX) && value.isNotBlank() }
            .mapTo(HashSet()) { it.key.removePrefix(PREFIX) }

    private companion object {
        const val PREFIX = "key."
    }
}
