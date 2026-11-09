package codes.tashif.paisa.ui.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.Settings
import codes.tashif.paisa.data.UnrecognizedSms
import codes.tashif.paisa.data.repository.SettingsRepository
import codes.tashif.paisa.data.repository.SmsInboxRepository
import codes.tashif.paisa.sms.SmsReaderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val smsInboxRepository: SmsInboxRepository,
    private val snackbarManager: SnackbarManager,
    workManager: WorkManager
) : ViewModel() {

    private var lastForegroundScanRequestAt = 0L

    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val smsScanProgress: StateFlow<SmsScanProgress> = workManager.smsScanProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SmsScanProgress())

    val unrecognizedSmsCount: StateFlow<Int> = smsInboxRepository.pendingUnrecognizedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val unrecognizedSms: StateFlow<List<UnrecognizedSms>> = smsInboxRepository.pendingUnrecognizedSms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun startSmsScan(forceFull: Boolean = false) {
        if (!hasReadSmsPermission()) {
            snackbarManager.show("Grant SMS permission to scan your inbox")
            return
        }
        SmsReaderWorker.enqueue(context, forceFull = forceFull)
        snackbarManager.show(if (forceFull) "Full SMS scan started" else "SMS scan started")
    }

    /**
     * Incrementally scans whenever the app enters the foreground. KEEP ensures
     * this lifecycle safety scan never cancels a user-requested full rescan.
     */
    fun scanSmsOnAppForeground() {
        if (!hasReadSmsPermission()) return

        val now = System.currentTimeMillis()
        if (now - lastForegroundScanRequestAt < FOREGROUND_SCAN_DEBOUNCE_MS) return
        lastForegroundScanRequestAt = now

        viewModelScope.launch {
            val current = settingsRepository.getSettings() ?: return@launch
            if (!current.onboardingCompleted || !current.smsScanEnabled) return@launch
            SmsReaderWorker.enqueue(
                context = context,
                forceFull = false,
                existingWorkPolicy = ExistingWorkPolicy.KEEP
            )
        }
    }

    fun cancelSmsScan() {
        SmsReaderWorker.cancel(context)
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

    private fun hasReadSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val FOREGROUND_SCAN_DEBOUNCE_MS = 30_000L
    }
}
