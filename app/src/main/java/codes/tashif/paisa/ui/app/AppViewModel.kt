package codes.tashif.paisa.ui.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.core.AppLockGuard
import codes.tashif.paisa.core.AppNavigator
import codes.tashif.paisa.core.PrivacySession
import codes.tashif.paisa.core.SnackbarEvent
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.AppTab
import codes.tashif.paisa.data.NavTabs
import codes.tashif.paisa.data.Settings
import codes.tashif.paisa.data.repository.AccountRepository
import codes.tashif.paisa.data.repository.SettingsRepository
import codes.tashif.paisa.data.repository.TransactionRepository
import codes.tashif.paisa.widget.PaisaWidgets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-wide state for the shell: settings, onboarding, the app lock, nav tabs,
 * navigation and snackbars. Scoped to MainActivity, so its init runs once per
 * activity and starts a fresh navigation and privacy session.
 */
class AppViewModel(
    private val context: Context,
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository,
    private val navigator: AppNavigator,
    private val privacySession: PrivacySession,
    private val lockGuard: AppLockGuard,
    snackbarManager: SnackbarManager
) : ViewModel() {

    init {
        // Navigation and balance visibility live in singletons, so reset them
        // for each new activity.
        navigator.startSession()
        viewModelScope.launch { privacySession.startSession() }
    }

    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The tabs shown in the bottom bar, in order. */
    val navTabs: StateFlow<List<AppTab>> = settingsRepository.settings
        .map { NavTabs.parse(it?.navTabs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NavTabs.parse(null))

    // --- NAVIGATION ---
    val currentTab: StateFlow<AppTab> = navigator.currentTab
    val selectedTransactionId: StateFlow<Int?> = navigator.selectedTransactionId

    /** Destination requested by a home screen widget, held until the UI shows it. */
    val pendingWidgetDestination: StateFlow<String?> = navigator.pendingWidgetDestination

    /** Shows [tab], whether or not it's in the bottom bar. */
    fun selectTab(tab: AppTab) = navigator.selectTab(tab)

    fun closeTransaction() = navigator.closeTransaction()

    fun requestWidgetDestination(destination: String?) =
        navigator.requestWidgetDestination(destination)

    fun consumeWidgetDestination() = navigator.consumeWidgetDestination()

    // --- PRIVACY AND SNACKBAR ---
    val balancesHidden: StateFlow<Boolean> = privacySession.balancesHidden

    val snackbarEvents: Flow<SnackbarEvent> = snackbarManager.events

    // --- APP LOCK ---
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()
    private var appLockSeeded = false

    fun unlockApp() {
        _isLocked.value = false
    }

    /** Call right before launching a picker or sheet so the resulting onStop doesn't lock. */
    fun onExternalActivityLaunched() = lockGuard.onExternalActivityLaunched()

    fun lockApp() {
        if (lockGuard.isExternalActivityPending) return
        if (settings.value?.biometricEnabled == true) {
            _isLocked.value = true
        }
    }

    /**
     * On return to the foreground, applies the deferred lock if the detour
     * outlived the grace period.
     */
    fun onReturnToForeground() {
        if (lockGuard.onReturnToForeground()) lockApp()
    }

    init {
        // The first settings row decides whether this session starts locked.
        viewModelScope.launch {
            settingsRepository.settings.collect { settingsValue ->
                if (settingsValue == null) return@collect
                if (!appLockSeeded) {
                    _isLocked.value = settingsValue.biometricEnabled
                    appLockSeeded = true
                }
            }
        }
        viewModelScope.launch {
            transactionRepository.processRecurringTransactions()
        }
        // Keep home screen widgets in step with in-app edits; account rows carry
        // the running balance, so this covers transaction changes too.
        viewModelScope.launch {
            accountRepository.allAccounts.collect {
                PaisaWidgets.refresh(context)
            }
        }
    }
}
