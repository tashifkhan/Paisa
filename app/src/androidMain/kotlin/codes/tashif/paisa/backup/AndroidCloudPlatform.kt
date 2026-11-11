package codes.tashif.paisa.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import codes.tashif.paisa.BuildConfig
import codes.tashif.paisa.sync.SyncScheduling
import codes.tashif.paisa.widget.PaisaWidgets
import java.util.concurrent.TimeUnit

/** Backup and sync on Android: Play sign-in for Drive, direct Nextcloud, WorkManager, widgets. */
class AndroidCloudPlatform(context: Context, private val prefs: BackupPrefs) : CloudPlatform {
    private val app = context.applicationContext

    override val appVersion: String = BuildConfig.VERSION_NAME

    override val deviceName: String get() = SyncScheduling.deviceName()

    override fun storeFor(config: BackupConfig): BackupStore? = when (config.provider) {
        BackupProvider.GOOGLE_DRIVE -> GoogleDriveAuth.store(app)
        BackupProvider.NEXTCLOUD -> config.nextcloud?.let { NextcloudStore(it) }
        BackupProvider.NONE -> null
    }

    override fun reschedule() {
        scheduleBackups(app, prefs.load())
        SyncScheduling.schedule(app)
    }

    override suspend fun onDataReplaced() = PaisaWidgets.refresh(app)

    companion object {
        private const val PERIODIC_WORK = "paisa_backup_periodic"

        fun scheduleBackups(context: Context, config: BackupConfig) {
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
