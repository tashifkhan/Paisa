package codes.tashif.paisa.backup

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import codes.tashif.paisa.platform.SharedPrefsSettingsStore

/** The same file as always: EncryptedSharedPreferences, or plain prefs if the keystore fails. */
fun androidBackupPrefs(context: Context): BackupPrefs {
    val app = context.applicationContext
    val prefs = try {
        val masterKey = MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            app,
            BackupPrefs.PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        app.getSharedPreferences(BackupPrefs.PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
    }
    return BackupPrefs(SharedPrefsSettingsStore(prefs))
}

/** Plain prefs under [name], for tests. */
fun testBackupPrefs(context: Context, name: String): BackupPrefs =
    BackupPrefs(SharedPrefsSettingsStore(context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)))
