package codes.tashif.paisa.ai.store

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import codes.tashif.paisa.ai.AiCredentialsStore

/** [AiKeyValueStore] over one SharedPreferences file. Each [commit] is one `commit()`. */
class SharedPrefsAiStore(private val prefs: SharedPreferences) : AiKeyValueStore {

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun contains(key: String): Boolean = prefs.contains(key)

    override fun strings(): Map<String, String> =
        prefs.all.mapNotNull { (name, value) -> (value as? String)?.let { name to it } }.toMap()

    override fun commit(changes: Map<String, String?>): Boolean {
        val editor = prefs.edit()
        changes.forEach { (key, value) -> if (value == null) editor.remove(key) else editor.putString(key, value) }
        return editor.commit()
    }

    companion object {
        /** The plain prefs file that holds the AI config. */
        fun aiConfig(context: Context): SharedPrefsAiStore = SharedPrefsAiStore(
            context.applicationContext.getSharedPreferences(AiConfigStore.PREFS_NAME, Context.MODE_PRIVATE)
        )

        /** The EncryptedSharedPreferences file that holds AI keys. */
        fun aiCredentials(context: Context): SharedPrefsAiStore = SharedPrefsAiStore(openCredentials(context))

        private fun openCredentials(context: Context): SharedPreferences = try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext,
                AiCredentialsStore.PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback for devices where crypto fails (rare); still app-private
            context.applicationContext.getSharedPreferences(
                AiCredentialsStore.PREFS_NAME_FALLBACK,
                Context.MODE_PRIVATE
            )
        }
    }
}
