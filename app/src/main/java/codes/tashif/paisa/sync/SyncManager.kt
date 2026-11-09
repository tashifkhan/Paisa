package codes.tashif.paisa.sync

import android.content.Context
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import codes.tashif.paisa.backup.BackupAuthException
import codes.tashif.paisa.backup.BackupCrypto
import codes.tashif.paisa.backup.BackupManager
import codes.tashif.paisa.backup.BackupPrefs
import codes.tashif.paisa.backup.BackupProvider
import codes.tashif.paisa.backup.BackupStore
import codes.tashif.paisa.backup.RemoteBackup
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.widget.PaisaWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

data class SyncReport(val received: Int, val sent: Int, val devices: Int)

/**
 * Keeps this device and the user's other devices in step through their own
 * Google Drive or Nextcloud. No Paisa server is involved.
 *
 * The local database stays the source of truth and works offline. A sync:
 *  1. lists the change files every device has uploaded,
 *  2. applies the ones from other devices this device hasn't seen yet,
 *  3. uploads one file with everything changed here since the last upload.
 *
 * Every file is encrypted with a key derived from the sync password, so the
 * cloud provider only ever stores ciphertext.
 */
class SyncManager internal constructor(
    context: Context,
    private val db: AppDatabase,
    private val prefs: BackupPrefs,
    private val storeOverride: BackupStore?
) {
    constructor(
        context: Context,
        db: AppDatabase,
        prefs: BackupPrefs
    ) : this(context, db, prefs, null)

    private val app = context.applicationContext
    private val sql get() = db.openHelper.writableDatabase

    private fun store(): BackupStore = storeOverride
        ?: BackupManager.storeFor(app, prefs.load())
        ?: throw IOException("Connect Google Drive or Nextcloud first")

    /**
     * Turns sync on. If other devices already sync, [password] must match
     * theirs; it is checked by decrypting one of their files.
     */
    suspend fun enable(password: CharArray) = BackupManager.lock.withLock {
        withContext(Dispatchers.IO) {
            val store = store()
            val existing = store.listSync()
                .filter { SyncFileNames.parse(it.name)?.deviceId != prefs.syncDeviceId() }
                .minByOrNull { it.sizeBytes }
            val key = if (existing != null) {
                val data = store.downloadSync(existing)
                val header = BackupCrypto.readHeader(data)
                if (!header.encrypted) throw IOException("Found an unencrypted sync file. Turn sync off on the other device and on again.")
                val key = BackupCrypto.deriveKey(password, header.salt!!, header.iterations)
                try {
                    BackupCrypto.open(data, header, key)
                } catch (e: BackupCrypto.WrongPasswordException) {
                    throw IOException("That isn't the sync password your other devices use")
                }
                key
            } else {
                BackupCrypto.deriveKey(password, BackupCrypto.newSalt())
            }
            prefs.enableSync(key)
            enabledCache = true
        }
        if (storeOverride == null) schedule(app)
    }

    fun disable() {
        prefs.disableSync()
        enabledCache = false
        if (storeOverride == null) schedule(app)
    }

    suspend fun syncNow(): SyncReport = BackupManager.lock.withLock {
        withContext(Dispatchers.IO) {
            try {
                val report = runSync()
                prefs.recordSyncSuccess(System.currentTimeMillis())
                report
            } catch (e: IOException) {
                prefs.recordSyncError(e.message ?: "Sync failed")
                throw e
            }
        }
    }

    private suspend fun runSync(): SyncReport {
        val key = prefs.syncKey() ?: throw IOException("Turn on sync first")
        if (!prefs.syncEnabled()) throw IOException("Turn on sync first")
        val store = store()
        val me = prefs.syncDeviceId()

        val files = store.listSync().mapNotNull { f -> SyncFileNames.parse(f.name)?.let { it to f } }
        val problems = mutableListOf<String>()
        val received = pull(store, key, me, files, problems)
        val sent = push(store, key, me, files)
        if (received > 0) PaisaWidgets.refresh(app)
        // Our own changes went up; still tell the user why a device was skipped.
        problems.firstOrNull()?.let { throw IOException(it) }
        val devices = files.map { it.first.deviceId }.filter { it != me }.toSet().size
        return SyncReport(received, sent, devices)
    }

    // --- Pull ---

    private suspend fun pull(
        store: BackupStore,
        key: BackupCrypto.DerivedKey,
        me: String,
        files: List<Pair<SyncFileNames.Parsed, RemoteBackup>>,
        problems: MutableList<String>
    ): Int {
        var received = 0
        val cursors = prefs.syncCursors()
        for ((device, theirs) in files.filter { it.first.deviceId != me }.groupBy { it.first.deviceId }) {
            try {
                received += pullDevice(store, key, device, theirs, cursors[device] ?: 0L)
            } catch (e: SkipDevice) {
                // One device on a newer Paisa or another password shouldn't block the rest.
                problems += e.message.orEmpty()
            }
        }
        return received
    }

    private class SkipDevice(message: String) : IOException(message)

    private suspend fun pullDevice(
        store: BackupStore,
        key: BackupCrypto.DerivedKey,
        device: String,
        theirs: List<Pair<SyncFileNames.Parsed, RemoteBackup>>,
        seen: Long
    ): Int {
        var received = 0
        val sorted = theirs.sortedBy { it.first.seq }
        val latestFull = sorted.lastOrNull { it.first.full }

        // Jump to their newest full copy when we've never read them, or when
        // the files between what we've seen and that copy were cleaned up.
        var from = seen
        if (latestFull != null && latestFull.first.seq > seen) {
            val have = sorted.map { it.first.seq }.toSet()
            val gap = seen == 0L || ((seen + 1) until latestFull.first.seq).any { it !in have }
            if (gap) {
                received += applyFile(store, key, latestFull.second, device)
                from = latestFull.first.seq
            }
        }
        for ((parsed, file) in sorted) {
            if (parsed.seq <= from || parsed.full) continue
            received += applyFile(store, key, file, device)
        }
        return received
    }

    private suspend fun applyFile(
        store: BackupStore,
        key: BackupCrypto.DerivedKey,
        file: RemoteBackup,
        device: String
    ): Int {
        val data = store.downloadSync(file)
        val header = BackupCrypto.readHeader(data)
        if (!header.encrypted || !key.matches(header)) throw SkipDevice(WRONG_PASSWORD)
        val payload = try {
            BackupCrypto.open(data, header, key)
        } catch (e: BackupCrypto.WrongPasswordException) {
            throw SkipDevice(WRONG_PASSWORD)
        }
        // Apply and record progress together, even if WorkManager cancels us.
        return withContext(NonCancellable) {
            val result = try {
                // Through Room, not the raw handle: Room only tells open queries
                // (the UI, widgets) to re-run when one of its own transactions ends.
                db.runInTransaction<SyncEngine.ApplyResult> { SyncEngine.apply(sql, payload) }
            } catch (e: SyncEngine.NewerVersionException) {
                throw SkipDevice(e.message.orEmpty())
            }
            val seq = SyncFileNames.parse(file.name)?.seq ?: result.header.seq
            prefs.setSyncCursor(device, seq)
            prefs.setSyncPeer(device, result.header.deviceName, result.header.createdAt)
            result.applied
        }
    }

    // --- Push ---

    private suspend fun push(
        store: BackupStore,
        key: BackupCrypto.DerivedKey,
        me: String,
        files: List<Pair<SyncFileNames.Parsed, RemoteBackup>>
    ): Int {
        val full = prefs.syncNeedsFull()
        if (!full && !SyncEngine.hasPendingChanges(sql)) return 0

        var (seq, sent) = upload(store, key, me, full)
        if (!full && prefs.syncDeltasSinceFull() >= COMPACT_AFTER) {
            // Fold our history into one full copy so new devices download less.
            seq = upload(store, key, me, full = true).first
        }
        if (prefs.syncDeltasSinceFull() == 0) {
            // We just wrote a full copy; our older files are no longer needed.
            val mine = files.filter { it.first.deviceId == me && it.first.seq < seq }
            for ((_, file) in mine) {
                try {
                    store.deleteSync(file)
                } catch (e: IOException) {
                    // Left for next time. Readers skip what a full copy covers.
                }
            }
        }
        return sent
    }

    /** Returns the file number used and how many changes it carried. */
    private suspend fun upload(
        store: BackupStore,
        key: BackupCrypto.DerivedKey,
        me: String,
        full: Boolean
    ): Pair<Long, Int> {
        val seq = prefs.syncSeq() + 1
        var export: SyncEngine.Export? = null
        db.runInTransaction {
            export = SyncEngine.export(
                sql,
                SyncEngine.Header(me, deviceName(), seq, full, System.currentTimeMillis())
            )
        }
        val result = export!!
        val sealed = BackupCrypto.seal(result.payload, key)
        // A cancel between the upload and the bookkeeping would re-send the same
        // number next time, so finish the pair once the upload starts.
        withContext(NonCancellable) {
            store.uploadSync(SyncFileNames.name(me, seq, full), sealed)
            // Only after the upload: a failed upload leaves the changes queued.
            db.runInTransaction { SyncEngine.clearOutbox(sql, result.outbox) }
            prefs.recordUpload(seq, full)
        }
        return seq to result.changes
    }

    companion object {
        /** Deltas before a device folds its history into one full file. */
        private const val COMPACT_AFTER = 40
        private const val PERIODIC_WORK = "paisa_sync_periodic"
        private const val SOON_WORK = "paisa_sync_soon"
        private const val WRONG_PASSWORD =
            "A device syncs with a different password. Use the same one everywhere."

        /** Read on every database change, so cached instead of opening encrypted prefs. */
        @Volatile
        private var enabledCache: Boolean? = null

        fun isEnabled(context: Context): Boolean =
            enabledCache ?: BackupPrefs(context.applicationContext).let {
                val on = it.syncEnabled() && it.load().provider != BackupProvider.NONE
                enabledCache = on
                on
            }

        fun invalidateCache() {
            enabledCache = null
        }

        fun deviceName(): String =
            listOfNotNull(Build.MANUFACTURER?.replaceFirstChar { it.uppercase() }, Build.MODEL)
                .distinct()
                .joinToString(" ")
                .ifBlank { "Android device" }

        private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Hourly catch-up, so a device left closed still picks changes up. */
        fun schedule(context: Context) {
            val wm = WorkManager.getInstance(context)
            if (!isEnabled(context)) {
                wm.cancelUniqueWork(PERIODIC_WORK)
                wm.cancelUniqueWork(SOON_WORK)
                return
            }
            val request = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(network)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /**
         * Sync shortly. Called after local edits (debounced: each call restarts
         * the delay, so a burst of edits or an SMS rescan uploads once) and when
         * the app comes to the foreground.
         */
        fun requestSoon(context: Context, delaySeconds: Long = 20) {
            if (!isEnabled(context)) return
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setConstraints(network)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(SOON_WORK, ExistingWorkPolicy.REPLACE, request)
        }

        /** Hooks local database changes to [requestSoon]. Call once per process. */
        fun watchLocalChanges(context: Context) {
            val app = context.applicationContext
            val db = AppDatabase.getDatabase(app)
            db.invalidationTracker.addObserver(object :
                androidx.room.InvalidationTracker.Observer(SyncSchema.TABLE_NAMES) {
                override fun onInvalidated(tables: Set<String>) {
                    if (!isEnabled(app)) return
                    // Changes pulled from other devices also invalidate these tables
                    // but never reach the outbox; only real local edits do.
                    val pending = try {
                        SyncEngine.hasPendingChanges(db.openHelper.readableDatabase)
                    } catch (e: Exception) {
                        false
                    }
                    if (pending) requestSoon(app)
                }
            })
        }
    }
}

class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!SyncManager.isEnabled(applicationContext)) return Result.success()
        return try {
            syncManager.syncNow()
            Result.success()
        } catch (e: BackupAuthException) {
            // The user has to sign in again; retrying won't help. The backup screen shows it.
            Result.failure()
        } catch (e: SyncEngine.NewerVersionException) {
            Result.failure()
        } catch (e: IOException) {
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }
}
