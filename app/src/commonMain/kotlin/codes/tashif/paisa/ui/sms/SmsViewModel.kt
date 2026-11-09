package codes.tashif.paisa.ui.sms

import codes.tashif.paisa.platform.nowMillis
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.Settings
import codes.tashif.paisa.data.UnrecognizedSms
import codes.tashif.paisa.data.repository.SettingsRepository
import codes.tashif.paisa.data.repository.SmsInboxRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmsViewModel(
    private val settingsRepository: SettingsRepository,
    private val smsInboxRepository: SmsInboxRepository,
    private val snackbarManager: SnackbarManager,
    private val smsScan: SmsScanService
) : ViewModel() {

    private var lastForegroundScanRequestAt = 0L

    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val smsScanProgress: StateFlow<SmsScanProgress> = smsScan.scanProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SmsScanProgress())

    val unrecognizedSmsCount: StateFlow<Int> = smsInboxRepository.pendingUnrecognizedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val unrecognizedSms: StateFlow<List<UnrecognizedSms>> = smsInboxRepository.pendingUnrecognizedSms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun startSmsScan(forceFull: Boolean = false) {
        if (!smsImportSupported) {
            snackbarManager.show(SMS_UNSUPPORTED_MESSAGE)
            return
        }
        if (!hasReadSmsPermission()) {
            snackbarManager.show("Grant SMS permission to scan your inbox")
            return
        }
        smsScan.startScan(forceFull)
        snackbarManager.show(if (forceFull) "Full SMS scan started" else "SMS scan started")
    }

    /**
     * Incrementally scans whenever the app enters the foreground. KEEP ensures
     * this lifecycle safety scan never cancels a user-requested full rescan.
     */
    fun scanSmsOnAppForeground() {
        if (!hasReadSmsPermission()) return

        val now = nowMillis()
        if (now - lastForegroundScanRequestAt < FOREGROUND_SCAN_DEBOUNCE_MS) return
        lastForegroundScanRequestAt = now

        viewModelScope.launch {
            val current = settingsRepository.getSettings() ?: return@launch
            if (!current.onboardingCompleted || !current.smsScanEnabled) return@launch
            smsScan.startScanIfIdle()
        }
    }

    fun cancelSmsScan() {
        smsScan.cancelScan()
        snackbarManager.show("SMS scan cancelled")
    }

    fun markUnrecognizedReviewed(id: Int) {
        viewModelScope.launch {
            smsInboxRepository.markUnrecognizedReviewed(id)
        }
    }

    fun deleteUnrecognized(id: Int) {
        viewModelScope.launch {
            smsInboxRepository.deleteUnrecognized(id)
        }
    }

    private fun hasReadSmsPermission(): Boolean = smsScan.hasReadSmsPermission()

    companion object {
        private const val FOREGROUND_SCAN_DEBOUNCE_MS = 30_000L

        /** Shown where the platform can't read SMS. */
        const val SMS_UNSUPPORTED_MESSAGE =
            "SMS import needs the Android app. Browsers can't read text messages."
    }
}
