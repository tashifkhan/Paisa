package codes.tashif.paisa.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The EncryptedSharedPreferences file (Android Keystore-backed) that holds AI
 * keys. [load] reads the old single-provider settings, which are only used to
 * migrate to per-provider keys; [prefs] is shared with
 * [codes.tashif.paisa.ai.store.AiKeyStore]. Never write keys into Room or logs.
 */
@Singleton
class AiCredentialsStore internal constructor(internal val prefs: SharedPreferences) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(openPrefs(context))

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

    /** True when the old single-provider entries are still on disk. */
    fun hasLegacy(): Boolean = prefs.contains(KEY_PROVIDER) || prefs.contains(KEY_API_KEY)

    /** Removes the old single-provider entries, leaving per-provider keys alone. Returns whether it saved. */
    fun clearLegacy(): Boolean =
        prefs.edit()
            .remove(KEY_PROVIDER)
            .remove(KEY_API_KEY)
            .remove(KEY_BASE_URL)
            .remove(KEY_MODEL)
            .commit()

    /** Writes old single-provider settings. Only tests need this. */
    internal fun saveLegacy(creds: AiCredentials): Boolean =
        prefs.edit()
            .putString(KEY_PROVIDER, creds.provider.id)
            .putString(KEY_API_KEY, creds.apiKey)
            .putString(KEY_BASE_URL, creds.baseUrl)
            .putString(KEY_MODEL, creds.model)
            .commit()

    companion object {
        private fun openPrefs(context: Context): SharedPreferences = try {
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

        private const val PREFS_NAME = "paisa_ai_credentials"
        private const val PREFS_NAME_FALLBACK = "paisa_ai_credentials_plain"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "model"
    }
}
