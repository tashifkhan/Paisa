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
import codes.tashif.paisa.backup.BackupProvider
import codes.tashif.paisa.backup.androidBackupPrefs
import codes.tashif.paisa.data.PaisaDatabase
import codes.tashif.paisa.data.sql.sqlRead
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.Volatile

/** WorkManager scheduling for sync on Android: hourly, and soon after local edits. */
object SyncScheduling {
    private const val PERIODIC_WORK = "paisa_sync_periodic"
    private const val SOON_WORK = "paisa_sync_soon"

    /** Read on every database change, so cached instead of opening encrypted prefs. */
    @Volatile
    private var enabledCache: Boolean? = null

    fun isEnabled(context: Context): Boolean =
        enabledCache ?: androidBackupPrefs(context).let {
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
        invalidateCache()
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
        val db = PaisaDatabase.get(app)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            db.invalidationTracker.createFlow(*SyncSchema.TABLE_NAMES, emitInitialState = false)
                .collect {
                    if (!isEnabled(app)) return@collect
                    // Changes pulled from other devices also invalidate these tables
                    // but never reach the outbox; only real local edits do.
                    val pending = try {
                        db.sqlRead { SyncEngine.hasPendingChanges(it) }
                    } catch (e: Exception) {
                        false
                    }
                    if (pending) requestSoon(app)
                }
        }
    }
}

class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!SyncScheduling.isEnabled(applicationContext)) return Result.success()
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
