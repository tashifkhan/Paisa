package codes.tashif.paisa.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.widget.PaisaWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/** The backup is encrypted with a key this device doesn't have. Ask for the password. */
class PasswordRequiredException : IOException("This backup is encrypted. Enter its password.")

data class RestoreResult(val transactions: Int, val backupCreatedAt: Long)

class BackupManager(context: Context) {

    private val app = context.applicationContext
    val prefs = BackupPrefs(app)
    private val db = AppDatabase.getDatabase(app)

    fun store(config: BackupConfig = prefs.load()): BackupStore? = when (config.provider) {
        BackupProvider.GOOGLE_DRIVE -> GoogleDriveStore(app)
        BackupProvider.NEXTCLOUD -> config.nextcloud?.let { NextcloudStore(it) }
        BackupProvider.NONE -> null
    }

    /** Snapshot of the whole database as a sealed `.paisa` file. */
    suspend fun createArchive(): ByteArray = withContext(Dispatchers.IO) {
        var payload: ByteArray? = null
        db.runInTransaction { payload = BackupArchive.export(db.openHelper.writableDatabase) }
        BackupCrypto.seal(payload!!, prefs.encryptionKey())
    }

    /** Uploads a new backup, then keeps only the newest [KEEP]. */
    suspend fun backupNow(): RemoteBackup = lock.withLock {
        val store = store() ?: throw IOException("Choose Google Drive or Nextcloud first")
        val now = System.currentTimeMillis()
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

    suspend fun download(backup: RemoteBackup): ByteArray =
        store()?.download(backup) ?: throw IOException("No backup account connected")

    fun readLimited(input: InputStream): ByteArray = input.use {
        val bytes = it.readBytes()
        if (bytes.size > MAX_FILE_BYTES) throw IOException("Backup file is too large")
        bytes
    }

    /**
     * Replaces all Paisa data with [data]. Nothing changes unless the file decrypts,
     * parses, and inserts cleanly; any failure rolls the transaction back.
     */
    suspend fun restore(data: ByteArray, password: CharArray? = null): RestoreResult = lock.withLock {
        withContext(Dispatchers.IO) {
            val header = BackupCrypto.readHeader(data)
            val key = if (header.encrypted) {
                val stored = prefs.encryptionKey()
                when {
                    password != null ->
                        BackupCrypto.deriveKey(password, header.salt!!, header.iterations)
                    stored != null && stored.matches(header) -> stored
                    else -> throw PasswordRequiredException()
                }
            } else {
                null
            }
            val snapshot = BackupArchive.parse(BackupCrypto.open(data, header, key))
            db.runInTransaction {
                val sdb = db.openHelper.writableDatabase
                BackupArchive.restore(sdb, snapshot)
                // Fresh device: rescan the inbox. Hashes and soft deletes stop duplicates.
                sdb.execSQL("UPDATE settings SET lastSmsScanAt = 0, onboardingCompleted = 1")
            }
            PaisaWidgets.refresh(app)
            RestoreResult(snapshot.transactionCount, snapshot.createdAt)
        }
    }

    fun schedule() = schedule(app, prefs.load())

    companion object {
        /** Remote copies kept per provider. */
        const val KEEP = 3
        private const val MAX_FILE_BYTES = 200 * 1024 * 1024
        private const val PERIODIC_WORK = "paisa_backup_periodic"

        /** One backup or restore at a time, across the UI and WorkManager. */
        private val lock = Mutex()

        fun schedule(context: Context, config: BackupConfig) {
            val wm = WorkManager.getInstance(context)
            if (config.provider == BackupProvider.NONE || config.frequency == BackupFrequency.MANUAL) {
                wm.cancelUniqueWork(PERIODIC_WORK)
                return
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (config.allowCellular) NetworkType.CONNECTED else NetworkType.UNMETERED
                )
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<BackupWorker>(config.frequency.days, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
