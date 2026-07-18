package codes.tashif.paisa.ai

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores BYOK API keys in EncryptedSharedPreferences (Android Keystore-backed).
 * Never write keys into Room or logs.
 */
class AiCredentialsStore(context: Context) {

    private val prefs = try {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback for devices where crypto fails (rare); still app-private
        context.applicationContext.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
    }

    fun load(): AiCredentials {
        val provider = AiProvider.fromId(prefs.getString(KEY_PROVIDER, null))
        return AiCredentials(
            provider = provider,
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            baseUrl = prefs.getString(KEY_BASE_URL, provider.defaultBaseUrl)
                ?: provider.defaultBaseUrl,
            model = prefs.getString(KEY_MODEL, provider.defaultModel)
                ?: provider.defaultModel
        )
    }

    fun save(credentials: AiCredentials) {
        prefs.edit()
            .putString(KEY_PROVIDER, credentials.provider.id)
            .putString(KEY_API_KEY, credentials.apiKey.trim())
            .putString(KEY_BASE_URL, credentials.baseUrl.trim().trimEnd('/'))
            .putString(KEY_MODEL, credentials.model.trim())
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "paisa_ai_credentials"
        private const val PREFS_NAME_FALLBACK = "paisa_ai_credentials_plain"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "model"
    }
}
