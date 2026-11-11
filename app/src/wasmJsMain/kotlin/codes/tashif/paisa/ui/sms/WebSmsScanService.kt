package codes.tashif.paisa.ui.sms

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Browsers can't read SMS, so there is never permission and never a scan. */
class WebSmsScanService : SmsScanService {
    override fun scanProgress(): Flow<SmsScanProgress> = flowOf(SmsScanProgress())
    override fun hasReadSmsPermission(): Boolean = false
    override fun startScan(forceFull: Boolean) = Unit
    override fun startScanIfIdle() = Unit
    override fun cancelScan() = Unit
}
