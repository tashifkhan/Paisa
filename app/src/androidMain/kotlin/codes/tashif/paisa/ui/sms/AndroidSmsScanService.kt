package codes.tashif.paisa.ui.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import codes.tashif.paisa.sms.SmsReaderWorker
import kotlinx.coroutines.flow.Flow

/** Runs scans through [SmsReaderWorker] and reads progress from WorkManager. */
class AndroidSmsScanService(
    private val context: Context,
    private val workManager: WorkManager
) : SmsScanService {

    override fun scanProgress(): Flow<SmsScanProgress> = workManager.smsScanProgress()

    override fun hasReadSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    override fun startScan(forceFull: Boolean) {
        SmsReaderWorker.enqueue(context, forceFull = forceFull)
    }

    // KEEP ensures this never cancels a user-requested full rescan.
    override fun startScanIfIdle() {
        SmsReaderWorker.enqueue(
            context = context,
            forceFull = false,
            existingWorkPolicy = ExistingWorkPolicy.KEEP
        )
    }

    override fun cancelScan() {
        SmsReaderWorker.cancel(context)
    }
}
