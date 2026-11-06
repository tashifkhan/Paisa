package codes.tashif.paisa.backup

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

enum class BackupProvider(val id: String, val label: String) {
    NONE("none", "Not set up"),
    GOOGLE_DRIVE("gdrive", "Google Drive"),
    NEXTCLOUD("nextcloud", "Nextcloud");

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: NONE
    }
}

enum class BackupFrequency(val id: String, val label: String, val days: Long) {
    MANUAL("manual", "Only when I tap \"Back up\"", 0),
    DAILY("daily", "Daily", 1),
    WEEKLY("weekly", "Weekly", 7),
    MONTHLY("monthly", "Monthly", 30);

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: MANUAL
    }
}

data class NextcloudAccount(
    val serverUrl: String,
    val loginName: String,
    val userId: String,
    val appPassword: String
)

data class BackupConfig(
    val provider: BackupProvider,
    val frequency: BackupFrequency,
    val allowCellular: Boolean,
    val encrypted: Boolean,
    val googleEmail: String?,
    val nextcloud: NextcloudAccount?,
    val lastBackupAt: Long,
    val lastBackupSize: Long,
    val lastError: String?
)

/**
 * Backup settings, the Nextcloud app password, and the derived encryption key.
 *
 * Stored in EncryptedSharedPreferences and excluded from Android Auto Backup
 * (see res/xml/backup_rules.xml) so none of it leaves the device.
 */
class BackupPrefs(context: Context) {

    val prefs: SharedPreferences = try {
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
        context.applicationContext.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
    }

    fun load(): BackupConfig {
        val ncServer = prefs.getString(K_NC_SERVER, null)
        val nc = if (ncServer != null) {
            NextcloudAccount(
                serverUrl = ncServer,
                loginName = prefs.getString(K_NC_LOGIN, "") ?: "",
                userId = prefs.getString(K_NC_USER, "") ?: "",
                appPassword = prefs.getString(K_NC_PASSWORD, "") ?: ""
            )
        } else {
            null
        }
        return BackupConfig(
            provider = BackupProvider.fromId(prefs.getString(K_PROVIDER, null)),
            frequency = BackupFrequency.fromId(prefs.getString(K_FREQUENCY, null)),
            allowCellular = prefs.getBoolean(K_CELLULAR, false),
            encrypted = prefs.getString(K_KEY, null) != null,
            googleEmail = prefs.getString(K_GOOGLE_EMAIL, null),
            nextcloud = nc,
            lastBackupAt = prefs.getLong(K_LAST_AT, 0L),
            lastBackupSize = prefs.getLong(K_LAST_SIZE, 0L),
            lastError = prefs.getString(K_LAST_ERROR, null)
        )
    }

    fun setGoogle(email: String?) {
        prefs.edit()
            .putString(K_PROVIDER, BackupProvider.GOOGLE_DRIVE.id)
            .putString(K_GOOGLE_EMAIL, email)
            .remove(K_LAST_ERROR)
            .apply()
    }

    fun setNextcloud(account: NextcloudAccount) {
        prefs.edit()
            .putString(K_PROVIDER, BackupProvider.NEXTCLOUD.id)
            .putString(K_NC_SERVER, account.serverUrl)
            .putString(K_NC_LOGIN, account.loginName)
            .putString(K_NC_USER, account.userId)
            .putString(K_NC_PASSWORD, account.appPassword)
            .remove(K_LAST_ERROR)
            .apply()
    }

    /** Forgets the account. Remote backups stay where they are. */
    fun disconnect() {
        prefs.edit()
            .putString(K_PROVIDER, BackupProvider.NONE.id)
            .remove(K_GOOGLE_EMAIL)
            .remove(K_NC_SERVER)
            .remove(K_NC_LOGIN)
            .remove(K_NC_USER)
            .remove(K_NC_PASSWORD)
            .remove(K_LAST_ERROR)
            .apply()
    }

    fun setFrequency(frequency: BackupFrequency) {
        prefs.edit().putString(K_FREQUENCY, frequency.id).apply()
    }

    fun setAllowCellular(allow: Boolean) {
        prefs.edit().putBoolean(K_CELLULAR, allow).apply()
    }

    fun encryptionKey(): BackupCrypto.DerivedKey? {
        val key = prefs.getString(K_KEY, null) ?: return null
        val salt = prefs.getString(K_SALT, null) ?: return null
        return BackupCrypto.DerivedKey(
            key = Base64.decode(key, Base64.NO_WRAP),
            salt = Base64.decode(salt, Base64.NO_WRAP),
            iterations = prefs.getInt(K_ITERATIONS, BackupCrypto.DEFAULT_ITERATIONS)
        )
    }

    fun setEncryptionKey(key: BackupCrypto.DerivedKey?) {
        val edit = prefs.edit()
        if (key == null) {
            edit.remove(K_KEY).remove(K_SALT).remove(K_ITERATIONS)
        } else {
            edit.putString(K_KEY, Base64.encodeToString(key.key, Base64.NO_WRAP))
                .putString(K_SALT, Base64.encodeToString(key.salt, Base64.NO_WRAP))
                .putInt(K_ITERATIONS, key.iterations)
        }
        edit.apply()
    }

    fun recordSuccess(at: Long, size: Long) {
        prefs.edit()
            .putLong(K_LAST_AT, at)
            .putLong(K_LAST_SIZE, size)
            .remove(K_LAST_ERROR)
            .apply()
    }

    fun recordError(message: String) {
        prefs.edit().putString(K_LAST_ERROR, message).apply()
    }

    companion object {
        const val PREFS_NAME = "paisa_backup"
        const val PREFS_NAME_FALLBACK = "paisa_backup_plain"
        private const val K_PROVIDER = "provider"
        private const val K_FREQUENCY = "frequency"
        private const val K_CELLULAR = "allow_cellular"
        private const val K_GOOGLE_EMAIL = "google_email"
        private const val K_NC_SERVER = "nc_server"
        private const val K_NC_LOGIN = "nc_login"
        private const val K_NC_USER = "nc_user"
        private const val K_NC_PASSWORD = "nc_app_password"
        private const val K_KEY = "enc_key"
        private const val K_SALT = "enc_salt"
        private const val K_ITERATIONS = "enc_iterations"
        private const val K_LAST_AT = "last_backup_at"
        private const val K_LAST_SIZE = "last_backup_size"
        private const val K_LAST_ERROR = "last_error"
    }
}
