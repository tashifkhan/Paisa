package codes.tashif.paisa.backup

import codes.tashif.paisa.platform.SettingsStore
import codes.tashif.paisa.platform.randomHexId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64

/**
 * Backup settings, the Nextcloud app password, the derived encryption key and
 * sync progress.
 *
 * On Android the store is EncryptedSharedPreferences, excluded from Auto
 * Backup (see res/xml/backup_rules.xml), so none of it leaves the device. On
 * the web it is localStorage.
 */
class BackupPrefs(val store: SettingsStore) {
    private val prefs get() = store

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
            lastError = prefs.getString(K_LAST_ERROR, null),
            sync = SyncStatus(
                enabled = syncEnabled(),
                lastSyncAt = prefs.getLong(K_SYNC_LAST_AT, 0L),
                error = prefs.getString(K_SYNC_ERROR, null),
                peers = syncPeers().values.map { it.first }.sorted()
            )
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

    /** Forgets the account and stops sync. Remote backups stay where they are. */
    fun disconnect() {
        disableSync()
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
            key = Base64.decode(key),
            salt = Base64.decode(salt),
            iterations = prefs.getInt(K_ITERATIONS, BackupCrypto.DEFAULT_ITERATIONS)
        )
    }

    fun setEncryptionKey(key: BackupCrypto.DerivedKey?) {
        val edit = prefs.edit()
        if (key == null) {
            edit.remove(K_KEY).remove(K_SALT).remove(K_ITERATIONS)
        } else {
            edit.putString(K_KEY, Base64.encode(key.key))
                .putString(K_SALT, Base64.encode(key.salt))
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

    // --- Sync ---
    //
    // Kept in this file because it is encrypted and excluded from Android Auto
    // Backup: a device id copied onto a second phone would make both phones
    // look like one device and they would skip each other's changes.

    fun syncEnabled(): Boolean = prefs.getBoolean(K_SYNC_ENABLED, false) && syncKey() != null

    /** This install's id in sync file names. New on every install. */
    fun syncDeviceId(): String {
        prefs.getString(K_SYNC_DEVICE, null)?.let { return it }
        val id = randomHexId().take(16)
        prefs.edit().putString(K_SYNC_DEVICE, id).commit()
        return id
    }

    fun syncKey(): BackupCrypto.DerivedKey? {
        val key = prefs.getString(K_SYNC_KEY, null) ?: return null
        val salt = prefs.getString(K_SYNC_SALT, null) ?: return null
        return BackupCrypto.DerivedKey(
            key = Base64.decode(key),
            salt = Base64.decode(salt),
            iterations = prefs.getInt(K_SYNC_ITERATIONS, BackupCrypto.DEFAULT_ITERATIONS)
        )
    }

    /** Turns sync on with [key]. The next sync pulls everything and sends a full copy. */
    fun enableSync(key: BackupCrypto.DerivedKey) {
        prefs.edit()
            .putString(K_SYNC_KEY, Base64.encode(key.key))
            .putString(K_SYNC_SALT, Base64.encode(key.salt))
            .putInt(K_SYNC_ITERATIONS, key.iterations)
            .putBoolean(K_SYNC_ENABLED, true)
            .putBoolean(K_SYNC_NEEDS_FULL, true)
            .remove(K_SYNC_CURSORS)
            .remove(K_SYNC_ERROR)
            .commit()
    }

    /** Stops syncing and forgets the sync password. Data here and in the cloud stays. */
    fun disableSync() {
        prefs.edit()
            .putBoolean(K_SYNC_ENABLED, false)
            .remove(K_SYNC_KEY)
            .remove(K_SYNC_SALT)
            .remove(K_SYNC_ITERATIONS)
            .remove(K_SYNC_CURSORS)
            .remove(K_SYNC_PEERS)
            .remove(K_SYNC_ERROR)
            .commit()
    }

    /** Last uploaded file number for this device. Never reused, even after a restore. */
    fun syncSeq(): Long = prefs.getLong(K_SYNC_SEQ, 0L)

    fun syncNeedsFull(): Boolean = prefs.getBoolean(K_SYNC_NEEDS_FULL, true)

    fun syncDeltasSinceFull(): Int = prefs.getInt(K_SYNC_DELTAS, 0)

    fun recordUpload(seq: Long, full: Boolean) {
        prefs.edit()
            .putLong(K_SYNC_SEQ, seq)
            .putBoolean(K_SYNC_NEEDS_FULL, false)
            .putInt(K_SYNC_DELTAS, if (full) 0 else syncDeltasSinceFull() + 1)
            .commit()
    }

    /** After a restore: re-read every device and send a full copy of what we now hold. */
    fun resetSyncProgress() {
        prefs.edit()
            .remove(K_SYNC_CURSORS)
            .putBoolean(K_SYNC_NEEDS_FULL, true)
            .commit()
    }

    /** Highest file number applied, per other device. */
    fun syncCursors(): Map<String, Long> {
        val raw = prefs.getString(K_SYNC_CURSORS, null) ?: return emptyMap()
        return try {
            Json.parseToJsonElement(raw).jsonObject.mapValues { it.value.jsonPrimitive.long }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun setSyncCursor(deviceId: String, seq: Long) {
        val all = syncCursors() + (deviceId to seq)
        val json = buildJsonObject { all.forEach { (k, v) -> put(k, v) } }
        prefs.edit().putString(K_SYNC_CURSORS, json.toString()).commit()
    }

    /** deviceId to (name, last file time). */
    fun syncPeers(): Map<String, Pair<String, Long>> {
        val raw = prefs.getString(K_SYNC_PEERS, null) ?: return emptyMap()
        return try {
            Json.parseToJsonElement(raw).jsonObject.mapValues { (_, v) ->
                val o = v.jsonObject
                (o["name"]?.jsonPrimitive?.content ?: "") to (o["at"]?.jsonPrimitive?.longOrNull ?: 0L)
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun setSyncPeer(deviceId: String, name: String, at: Long) {
        val existing = try {
            Json.parseToJsonElement(prefs.getString(K_SYNC_PEERS, null) ?: "{}").jsonObject
        } catch (e: Exception) {
            JsonObject(emptyMap())
        }
        val json = JsonObject(existing + (deviceId to buildJsonObject { put("name", name); put("at", at) }))
        prefs.edit().putString(K_SYNC_PEERS, json.toString()).apply()
    }

    fun recordSyncSuccess(at: Long) {
        prefs.edit().putLong(K_SYNC_LAST_AT, at).remove(K_SYNC_ERROR).apply()
    }

    fun recordSyncError(message: String) {
        prefs.edit().putString(K_SYNC_ERROR, message).apply()
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
        private const val K_SYNC_ENABLED = "sync_enabled"
        private const val K_SYNC_DEVICE = "sync_device_id"
        private const val K_SYNC_KEY = "sync_key"
        private const val K_SYNC_SALT = "sync_salt"
        private const val K_SYNC_ITERATIONS = "sync_iterations"
        private const val K_SYNC_SEQ = "sync_seq"
        private const val K_SYNC_NEEDS_FULL = "sync_needs_full"
        private const val K_SYNC_DELTAS = "sync_deltas_since_full"
        private const val K_SYNC_CURSORS = "sync_cursors"
        private const val K_SYNC_PEERS = "sync_peers"
        private const val K_SYNC_LAST_AT = "sync_last_at"
        private const val K_SYNC_ERROR = "sync_error"
    }
}
