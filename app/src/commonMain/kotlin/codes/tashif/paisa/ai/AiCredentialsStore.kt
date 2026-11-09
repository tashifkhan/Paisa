package codes.tashif.paisa.ai

import codes.tashif.paisa.ai.store.AiKeyValueStore

/**
 * The store that holds AI keys. On Android that is an EncryptedSharedPreferences
 * file (Android Keystore-backed). [load] reads the old single-provider settings,
 * which are only used to migrate to per-provider keys; [store] is shared with
 * [codes.tashif.paisa.ai.store.AiKeyStore]. Never write keys into Room or logs.
 */
class AiCredentialsStore(internal val store: AiKeyValueStore) {

    fun load(): AiCredentials {
        val provider = AiProvider.fromId(store.getString(KEY_PROVIDER))
        return AiCredentials(
            provider = provider,
            apiKey = store.getString(KEY_API_KEY) ?: "",
            baseUrl = store.getString(KEY_BASE_URL) ?: provider.defaultBaseUrl,
            model = store.getString(KEY_MODEL) ?: provider.defaultModel
        )
    }

    /** True when the old single-provider entries are still on disk. */
    fun hasLegacy(): Boolean = store.contains(KEY_PROVIDER) || store.contains(KEY_API_KEY)

    /** Removes the old single-provider entries, leaving per-provider keys alone. Returns whether it saved. */
    fun clearLegacy(): Boolean =
        store.commit(
            mapOf(KEY_PROVIDER to null, KEY_API_KEY to null, KEY_BASE_URL to null, KEY_MODEL to null)
        )

    /** Writes old single-provider settings. Only tests need this. */
    internal fun saveLegacy(creds: AiCredentials): Boolean =
        store.commit(
            mapOf(
                KEY_PROVIDER to creds.provider.id,
                KEY_API_KEY to creds.apiKey,
                KEY_BASE_URL to creds.baseUrl,
                KEY_MODEL to creds.model,
            )
        )

    companion object {
        /** The Android prefs file, and the web localStorage prefix. */
        const val PREFS_NAME = "paisa_ai_credentials"
        /** The plain prefs file Android falls back to when the Keystore fails. */
        const val PREFS_NAME_FALLBACK = "paisa_ai_credentials_plain"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "model"
    }
}
