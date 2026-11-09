package codes.tashif.paisa.ui.backup

import codes.tashif.paisa.backup.BackupArchive
import codes.tashif.paisa.backup.BackupConfig
import codes.tashif.paisa.backup.BackupCrypto
import codes.tashif.paisa.backup.BackupFrequency
import codes.tashif.paisa.backup.BackupNames
import codes.tashif.paisa.backup.BackupProvider
import codes.tashif.paisa.backup.NextcloudAccount
import codes.tashif.paisa.backup.PasswordRequiredException
import codes.tashif.paisa.backup.RemoteBackup
import codes.tashif.paisa.backup.RestoreResult
import codes.tashif.paisa.backup.SyncStatus
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.sql.sqlTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException

/**
 * Backup in the browser: save and restore `.paisa` files only. Google Drive,
 * Nextcloud and sync aren't available here yet. Settings such as the
 * encryption key last for this page session.
 */
class WebBackupService(private val db: AppDatabase) : BackupService {

    override val cloudUnavailableMessage: String = CLOUD_UNAVAILABLE
    override val syncUnavailableMessage: String = "Sync is off on the web for now"
    override val keptBackups: Int = 3

    private val lock = Mutex()
    private var frequency = BackupFrequency.MANUAL
    private var allowCellular = false
    private var encryptionKey: BackupCrypto.DerivedKey? = null

    override fun loadConfig(): BackupConfig = BackupConfig(
        provider = BackupProvider.NONE,
        frequency = frequency,
        allowCellular = allowCellular,
        encrypted = encryptionKey != null,
        googleEmail = null,
        nextcloud = null,
        lastBackupAt = 0L,
        lastBackupSize = 0L,
        lastError = null,
        sync = SyncStatus(enabled = false, lastSyncAt = 0L, error = null, peers = emptyList())
    )

    // Nothing else writes these settings in the browser.
    override fun configChanges(): Flow<Unit> = emptyFlow()

    override fun schedule() = Unit

    override suspend fun list(): List<RemoteBackup> = emptyList()

    override suspend fun download(backup: RemoteBackup): ByteArray = throw IOException(CLOUD_UNAVAILABLE)

    override suspend fun backupNow(): RemoteBackup = throw IOException(CLOUD_UNAVAILABLE)

    override suspend fun createArchive(): ByteArray = lock.withLock {
        val payload = db.sqlTransaction { BackupArchive.export(it, APP_VERSION) }
        BackupCrypto.seal(payload, encryptionKey)
    }

    // Same steps as BackupManager.restore on Android, minus sync and widgets.
    override suspend fun restore(data: ByteArray, password: CharArray?): RestoreResult = lock.withLock {
        val header = BackupCrypto.readHeader(data)
        val key = if (header.encrypted) {
            val stored = encryptionKey
            when {
                password != null -> BackupCrypto.deriveKey(password, header.salt!!, header.iterations)
                stored != null && stored.matches(header) -> stored
                else -> throw PasswordRequiredException()
            }
        } else {
            null
        }
        val snapshot = BackupArchive.parse(BackupCrypto.open(data, header, key))
        db.sqlTransaction { sql ->
            BackupArchive.restore(sql, snapshot)
            sql.exec("UPDATE settings SET lastSmsScanAt = 0, onboardingCompleted = 1")
        }
        RestoreResult(snapshot.transactionCount, snapshot.createdAt)
    }

    override suspend fun writeFile(file: BackupFile, bytes: ByteArray) =
        downloadBytes(file.name, bytes, BackupNames.MIME)

    override suspend fun readFile(file: BackupFile): ByteArray {
        val picked = file.file ?: throw IOException("Couldn't open that file")
        if (picked.size.toDouble() > MAX_FILE_BYTES) throw IOException("Backup file is too large")
        return readBytes(picked)
    }

    override suspend fun connectGoogle(accessToken: String): String? = throw IOException(CLOUD_UNAVAILABLE)

    override fun normalizeNextcloudServer(input: String): String? = null

    override fun nextcloudServerError(server: String): String = CLOUD_UNAVAILABLE

    override suspend fun startNextcloudLogin(server: String): NextcloudLogin = throw IOException(CLOUD_UNAVAILABLE)

    override suspend fun pollNextcloudLogin(login: NextcloudLogin): NextcloudAccount =
        throw IOException(CLOUD_UNAVAILABLE)

    override suspend fun verifyNextcloud(
        server: String,
        loginName: String,
        appPassword: String
    ): NextcloudAccount = throw IOException(CLOUD_UNAVAILABLE)

    override fun setNextcloud(account: NextcloudAccount) = Unit

    override suspend fun disconnect(provider: BackupProvider) = Unit

    override fun setFrequency(frequency: BackupFrequency) {
        this.frequency = frequency
    }

    override fun setAllowCellular(allow: Boolean) {
        allowCellular = allow
    }

    override fun setEncryptionKey(key: BackupCrypto.DerivedKey?) {
        encryptionKey = key
    }

    override suspend fun enableSync(password: CharArray) = throw IOException(syncUnavailableMessage)

    override fun disableSync() = Unit

    override suspend fun syncNow(): SyncSummary = throw IOException(syncUnavailableMessage)

    companion object {
        const val CLOUD_UNAVAILABLE = "Google Drive and Nextcloud aren't available on the web yet"
        private const val APP_VERSION = "web"
        private const val MAX_FILE_BYTES = 200.0 * 1024 * 1024
    }
}
