package codes.tashif.paisa.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import codes.tashif.paisa.ai.AiRepository
import codes.tashif.paisa.core.PrivacySession
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.AppTab
import codes.tashif.paisa.data.NavTabs
import codes.tashif.paisa.data.Settings
import codes.tashif.paisa.data.repository.CategoryRepository
import codes.tashif.paisa.data.repository.SettingsRepository
import codes.tashif.paisa.data.repository.SmsInboxRepository
import codes.tashif.paisa.data.repository.TransactionRepository
import codes.tashif.paisa.ui.sms.SmsScanProgress
import codes.tashif.paisa.ui.sms.smsScanProgress
import codes.tashif.paisa.ui.ai.AiSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Settings rows plus the counts and export actions the More screen shows. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val privacySession: PrivacySession,
    private val snackbarManager: SnackbarManager,
    aiRepository: AiRepository,
    private val exporter: TransactionExporter,
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
    smsInboxRepository: SmsInboxRepository,
    workManager: WorkManager
) : ViewModel() {

    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val categoryCount: StateFlow<Int> = categoryRepository.allCategories
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val merchantRuleCount: StateFlow<Int> = categoryRepository.allMerchantMappings
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val transactionCount: StateFlow<Int> = transactionRepository.allTransactions
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val unrecognizedSmsCount: StateFlow<Int> = smsInboxRepository.pendingUnrecognizedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val smsScanProgress: StateFlow<SmsScanProgress> = workManager.smsScanProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SmsScanProgress())

    /** The model statement import would run first, for the AI and import rows. */
    val aiSummary: StateFlow<AiSummary> = combine(
        aiRepository.ladders, aiRepository.providers, aiRepository.catalog, AiSummary::of
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiSummary())

    fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) = snackbarManager.show(message, actionLabel, onAction)

    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            settingsRepository.update(settings)
        }
    }

    fun setCurrency(symbol: String) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(currency = symbol) }
        }
    }

    fun setNavTabs(tabs: List<AppTab>) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(navTabs = NavTabs.serialize(tabs)) }
        }
    }

    fun setHideBalancesByDefault(hide: Boolean) {
        viewModelScope.launch {
            if (settingsRepository.update { it.copy(hideBalancesByDefault = hide) }) {
                privacySession.set(hide)
            }
        }
    }

    /**
     * Saves the app lock preference. The old ViewModel also cleared its
     * isLocked flag here; the app is always unlocked while this screen is
     * showing, so the shell's lock state needs no change.
     */
    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(biometricEnabled = enabled) }
        }
    }

    // --- EXPORT ---
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    /**
     * Writes all non-deleted transactions to [outStream] as CSV.
     * Call from a CreateDocument result; [onComplete] runs on the main thread.
     */
    fun exportTransactionsToCsv(outStream: java.io.OutputStream, onComplete: (Boolean) -> Unit) {
        _isExporting.value = true
        viewModelScope.launch {
            val success = exporter.writeCsv(outStream)
            _isExporting.value = false
            onComplete(success)
        }
    }

    /**
     * Writes a multi-page PDF statement of all transactions to [outStream].
     * [onComplete] runs on the main thread.
     */
    fun exportTransactionsToPdf(outStream: java.io.OutputStream, onComplete: (Boolean) -> Unit) {
        _isExporting.value = true
        viewModelScope.launch {
            val success = exporter.writePdf(outStream)
            _isExporting.value = false
            onComplete(success)
        }
    }
}
