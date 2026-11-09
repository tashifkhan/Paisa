package codes.tashif.paisa.backup

import kotlinx.coroutines.sync.Mutex

/** What backup and sync need from the platform they run on. */
interface CloudPlatform {
    /** Written into backup files. */
    val appVersion: String

    /** Shown to the user's other devices, like "Google Pixel 8" or "Chrome on macOS". */
    val deviceName: String

    /** The remote store [config] points at, or null when no provider is set up. */
    fun storeFor(config: BackupConfig): BackupStore?

    /** Plans scheduled backups and sync again after their settings change. */
    fun reschedule()

    /** Called after a restore or pulled changes, for anything outside the app's own UI. */
    suspend fun onDataReplaced()
}

/** One backup, restore or sync at a time, across the UI and background work. */
object BackupLock {
    val mutex = Mutex()
}
