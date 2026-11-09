package codes.tashif.paisa.ui.backup

import codes.tashif.paisa.backup.BackupConfig
import codes.tashif.paisa.backup.BackupCrypto
import codes.tashif.paisa.backup.BackupFrequency
import codes.tashif.paisa.backup.BackupProvider
import codes.tashif.paisa.backup.NextcloudAccount
import codes.tashif.paisa.backup.RemoteBackup
import codes.tashif.paisa.backup.RestoreResult
import kotlinx.coroutines.flow.Flow

/** A Nextcloud Login Flow v2 in progress. [handle] is the platform's own flow state. */
class NextcloudLogin(val loginUrl: String, val handle: Any)

/** Counts from one sync run. */
data class SyncSummary(val received: Int, val sent: Int, val devices: Int)

/**
 * Everything [BackupViewModel] needs from the platform: stored backup settings,
 * the database archive, cloud accounts, files and sync. Failures throw
 * kotlinx.io.IOException with a message the screen can show.
 */
interface BackupService {
    /** Null where Google Drive and Nextcloud work, otherwise the message to show instead. */
    val cloudUnavailableMessage: String?

    /** Null where multi-device sync works, otherwise the message to show instead. */
    val syncUnavailableMessage: String?

    /** Remote copies kept per provider. */
    val keptBackups: Int

    fun loadConfig(): BackupConfig

    /** Emits when stored settings change from elsewhere, such as the backup worker. */
    fun configChanges(): Flow<Unit>

    /** Re-plans the periodic backup from the stored settings. */
    fun schedule()

    // Backups

    suspend fun list(): List<RemoteBackup>
    suspend fun download(backup: RemoteBackup): ByteArray
    suspend fun backupNow(): RemoteBackup

    /** Snapshot of the whole database as a sealed `.paisa` file. */
    suspend fun createArchive(): ByteArray

    /** Replaces all data with [data]. Throws PasswordRequiredException when it needs a password. */
    suspend fun restore(data: ByteArray, password: CharArray?): RestoreResult

    suspend fun writeFile(file: BackupFile, bytes: ByteArray)

    /** Reads a picked backup file, refusing ones that are too large. */
    suspend fun readFile(file: BackupFile): ByteArray

    // Accounts

    /** Stores the Google account behind [accessToken]. Returns its email when Google shares it. */
    suspend fun connectGoogle(accessToken: String): String?

    fun normalizeNextcloudServer(input: String): String?

    /** Why [server] can't be used, or null when it's fine. */
    fun nextcloudServerError(server: String): String?

    suspend fun startNextcloudLogin(server: String): NextcloudLogin

    /** Waits until the user approves Paisa in the browser. */
    suspend fun pollNextcloudLogin(login: NextcloudLogin): NextcloudAccount

    suspend fun verifyNextcloud(server: String, loginName: String, appPassword: String): NextcloudAccount

    fun setNextcloud(account: NextcloudAccount)

    /** Signs out of [provider], forgets the account and reschedules backup and sync. */
    suspend fun disconnect(provider: BackupProvider)

    // Settings

    /** Stores [frequency] and reschedules. */
    fun setFrequency(frequency: BackupFrequency)

    /** Stores [allow] and reschedules. */
    fun setAllowCellular(allow: Boolean)

    fun setEncryptionKey(key: BackupCrypto.DerivedKey?)

    // Sync

    suspend fun enableSync(password: CharArray)
    fun disableSync()
    suspend fun syncNow(): SyncSummary
}
