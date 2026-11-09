package codes.tashif.paisa.sync

import codes.tashif.paisa.backup.BackupCrypto
import codes.tashif.paisa.backup.BackupLock
import codes.tashif.paisa.backup.BackupPrefs
import codes.tashif.paisa.backup.BackupProvider
import codes.tashif.paisa.backup.BackupStore
import codes.tashif.paisa.backup.CloudPlatform
import codes.tashif.paisa.backup.RemoteBackup
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.sql.sqlRead
import codes.tashif.paisa.data.sql.sqlTransaction
import codes.tashif.paisa.platform.nowMillis
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

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
class SyncManager(
    private val db: AppDatabase,
    private val prefs: BackupPrefs,
    private val platform: CloudPlatform,
    private val storeOverride: BackupStore? = null
) {
    private fun store(): BackupStore = storeOverride
        ?: platform.storeFor(prefs.load())
        ?: throw IOException("Connect Google Drive or Nextcloud first")

    /** On, with a cloud account to sync through. */
    fun isEnabled(): Boolean = prefs.syncEnabled() && prefs.load().provider != BackupProvider.NONE

    /**
     * Turns sync on. If other devices already sync, [password] must match
     * theirs; it is checked by decrypting one of their files.
     */
    suspend fun enable(password: CharArray) = BackupLock.mutex.withLock {
        run {
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
        }
        if (storeOverride == null) platform.reschedule()
    }

    fun disable() {
        prefs.disableSync()
        if (storeOverride == null) platform.reschedule()
    }

    suspend fun syncNow(): SyncReport = BackupLock.mutex.withLock {
        run {
            try {
                val report = runSync()
                prefs.recordSyncSuccess(nowMillis())
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
        if (received > 0) platform.onDataReplaced()
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
        // Apply and record progress together, even if the scheduler cancels us.
        return withContext(NonCancellable) {
            val result = try {
                // Through Room, not the raw handle: Room only tells open queries
                // (the UI, widgets) to re-run when one of its own transactions ends.
                db.sqlTransaction { SyncEngine.apply(it, payload) }
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
        if (!full && !db.sqlRead { SyncEngine.hasPendingChanges(it) }) return 0

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
        val result = db.sqlTransaction {
            SyncEngine.export(it, SyncEngine.Header(me, platform.deviceName, seq, full, nowMillis()))
        }
        val sealed = BackupCrypto.seal(result.payload, key)
        // A cancel between the upload and the bookkeeping would re-send the same
        // number next time, so finish the pair once the upload starts.
        withContext(NonCancellable) {
            store.uploadSync(SyncFileNames.name(me, seq, full), sealed)
            // Only after the upload: a failed upload leaves the changes queued.
            db.sqlTransaction { SyncEngine.clearOutbox(it, result.outbox) }
            prefs.recordUpload(seq, full)
        }
        return seq to result.changes
    }

    companion object {
        /** Deltas before a device folds its history into one full file. */
        private const val COMPACT_AFTER = 40
        private const val WRONG_PASSWORD =
            "A device syncs with a different password. Use the same one everywhere."
    }
}
