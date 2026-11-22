package sh.taf.billi.ui.settings

import sh.taf.billi.backup.BackupService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sh.taf.billi.ai.AiRepository
import sh.taf.billi.core.PrivacySession
import sh.taf.billi.core.SnackbarManager
import sh.taf.billi.core.AppTab
import sh.taf.billi.core.NavTabs
import sh.taf.billi.data.Settings
import sh.taf.billi.data.repository.CategoryRepository
import sh.taf.billi.data.repository.SettingsRepository
import sh.taf.billi.data.repository.SmsInboxRepository
import sh.taf.billi.data.repository.TransactionRepository
import sh.taf.billi.sms.SmsScanProgress
import sh.taf.billi.sms.SmsScanService
import sh.taf.billi.ui.ai.AiSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sh.taf.billi.export.ExportSink
import sh.taf.billi.export.TransactionExporter

/** Settings rows plus the counts and export actions the More screen shows. */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val privacySession: PrivacySession,
    private val snackbarManager: SnackbarManager,
    aiRepository: AiRepository,
    private val exporter: TransactionExporter,
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
    smsInboxRepository: SmsInboxRepository,
    smsScan: SmsScanService,
    private val backups: BackupService
) : ViewModel() {

    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val categoryCount: StateFlow<Int> = categoryRepository.allCategories
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Category rules plus renames: both live on the Merchant rules screen. */
    val merchantRuleCount: StateFlow<Int> = combine(
        categoryRepository.allMerchantMappings,
        categoryRepository.allMerchantRenames
    ) { mappings, renames -> mappings.size + renames.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val transactionCount: StateFlow<Int> = transactionRepository.allTransactions
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val unrecognizedSmsCount: StateFlow<Int> = smsInboxRepository.pendingUnrecognizedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val smsScanProgress: StateFlow<SmsScanProgress> = smsScan.scanProgress()
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

    /** Applies [transform] to the latest settings row. Prefer it to [updateSettings] for single edits. */
    fun editSettings(transform: (Settings) -> Settings) {
        viewModelScope.launch {
            settingsRepository.update(transform)
        }
    }

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
     * Writes all non-deleted transactions to [sink] as CSV.
     * [onComplete] runs on the main thread.
     */
    fun exportTransactionsToCsv(sink: ExportSink, onComplete: (Boolean) -> Unit) {
        _isExporting.value = true
        viewModelScope.launch {
            val success = exporter.writeCsv(sink)
            _isExporting.value = false
            onComplete(success)
        }
    }

    /** True when backups are sealed with the backup password, so an exported .billi file is too. */
    val backupEncrypted: Boolean get() = backups.loadConfig().encrypted

    /**
     * Writes a full `.billi` backup to [sink], the same file Backup saves.
     * Encrypted when backup encryption is on. [onComplete] runs on the main thread.
     */
    fun exportBackupFile(sink: ExportSink, onComplete: (Boolean) -> Unit) {
        _isExporting.value = true
        viewModelScope.launch {
            val success = try {
                sink.write(backups.createArchive())
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                sink.close()
            }
            _isExporting.value = false
            onComplete(success)
        }
    }

    /**
     * Writes a multi-page PDF statement of all transactions to [sink].
     * [onComplete] runs on the main thread.
     */
    fun exportTransactionsToPdf(sink: ExportSink, onComplete: (Boolean) -> Unit) {
        _isExporting.value = true
        viewModelScope.launch {
            val success = exporter.writePdf(sink)
            _isExporting.value = false
            onComplete(success)
        }
    }
}
