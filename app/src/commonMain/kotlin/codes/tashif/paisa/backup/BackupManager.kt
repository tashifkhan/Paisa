package codes.tashif.paisa.backup

import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.sql.sqlTransaction
import codes.tashif.paisa.platform.nowMillis
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException

/** Backups to Google Drive or Nextcloud, and restoring from them or from a file. */
class BackupManager(
    val prefs: BackupPrefs,
    private val db: AppDatabase,
    private val platform: CloudPlatform
) {

    fun store(config: BackupConfig = prefs.load()): BackupStore? = platform.storeFor(config)

    /** Snapshot of the whole database as a sealed `.paisa` file. */
    suspend fun createArchive(): ByteArray {
        val payload = db.sqlTransaction { BackupArchive.export(it, platform.appVersion) }
        return BackupCrypto.seal(payload, prefs.encryptionKey())
    }

    /** Uploads a new backup, then keeps only the newest [KEEP]. */
    suspend fun backupNow(): RemoteBackup = BackupLock.mutex.withLock {
        val store = store() ?: throw IOException("Choose Google Drive or Nextcloud first")
        val now = nowMillis()
        try {
            val bytes = createArchive()
            val uploaded = store.upload(BackupNames.forTime(now), bytes)
            prefs.recordSuccess(now, bytes.size.toLong())
            prune(store, uploaded)
            uploaded
        } catch (e: IOException) {
            prefs.recordError(e.message ?: "Backup failed")
            throw e
        }
    }

    private suspend fun prune(store: BackupStore, keep: RemoteBackup) {
        // Pruning only runs after a successful upload, so the newest good copy survives.
        try {
            store.list()
                .filter { it.id != keep.id }
                .drop(KEEP - 1)
                .forEach { store.delete(it) }
        } catch (e: IOException) {
            // Old copies linger until next time. Not worth failing the backup over.
        }
    }

    suspend fun list(): List<RemoteBackup> = store()?.list() ?: emptyList()

    /** Removes one backup from the connected account. Waits for any running backup or restore. */
    suspend fun delete(backup: RemoteBackup) = BackupLock.mutex.withLock {
        (store() ?: throw IOException("No backup account connected")).delete(backup)
    }

    suspend fun download(backup: RemoteBackup): ByteArray =
        store()?.download(backup) ?: throw IOException("No backup account connected")

    /** Rejects files too big to be a Paisa backup before they are parsed. */
    fun checkSize(bytes: ByteArray): ByteArray {
        if (bytes.size > MAX_FILE_BYTES) throw IOException("Backup file is too large")
        return bytes
    }

    /**
     * Replaces all Paisa data with [data]. Nothing changes unless the file decrypts,
     * parses, and inserts cleanly; any failure rolls the transaction back.
     */
    suspend fun restore(data: ByteArray, password: CharArray? = null): RestoreResult = BackupLock.mutex.withLock {
        val header = BackupCrypto.readHeader(data)
        val key = if (header.encrypted) {
            val stored = prefs.encryptionKey()
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
            // Fresh device: rescan the inbox. Hashes and soft deletes stop duplicates.
            sql.exec("UPDATE settings SET lastSmsScanAt = 0, onboardingCompleted = 1")
        }
        // Everything here is new to the cloud now: re-read every device and send
        // a full copy. Last-write-wins keeps whichever side changed a row last.
        prefs.resetSyncProgress()
        platform.onDataReplaced()
        RestoreResult(snapshot.transactionCount, snapshot.createdAt)
    }

    fun schedule() = platform.reschedule()

    companion object {
        /** Remote copies kept per provider. */
        const val KEEP = 3
        const val MAX_FILE_BYTES = 200 * 1024 * 1024
    }
}
