package codes.tashif.paisa.ui.sms

import kotlinx.coroutines.flow.Flow

/**
 * The inbox scanner behind [SmsViewModel]. On Android it is the SMS reader
 * worker; platforms that can't read SMS report no permission and do nothing.
 */
interface SmsScanService {
    /** Progress of the current or last scan. */
    fun scanProgress(): Flow<SmsScanProgress>

    fun hasReadSmsPermission(): Boolean

    /** Starts a scan, replacing any running one. */
    fun startScan(forceFull: Boolean)

    /** Starts an incremental scan unless one is already queued or running. */
    fun startScanIfIdle()

    fun cancelScan()
}
