package codes.tashif.paisa.core

import codes.tashif.paisa.data.AppTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-wide navigation state: the visible tab, the open transaction, and widget deep links. */
class AppNavigator() {

    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    /** Shows [tab], whether or not it's in the bottom bar. */
    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    private val _selectedTransactionId = MutableStateFlow<Int?>(null)
    val selectedTransactionId: StateFlow<Int?> = _selectedTransactionId.asStateFlow()

    fun openTransaction(id: Int) {
        _selectedTransactionId.value = id
    }

    fun closeTransaction() {
        _selectedTransactionId.value = null
    }

    /**
     * Destination requested by a home screen widget, held until the UI shows it.
     *
     * Kept here rather than read straight from the Intent so it survives the
     * biometric lock screen. The widget tap still lands on the right screen
     * after the user unlocks.
     */
    private val _pendingWidgetDestination = MutableStateFlow<String?>(null)
    val pendingWidgetDestination: StateFlow<String?> = _pendingWidgetDestination.asStateFlow()

    /** A null [destination] is ignored, so a plain launch doesn't clear a pending one. */
    fun requestWidgetDestination(destination: String?) {
        if (destination != null) _pendingWidgetDestination.value = destination
    }

    fun consumeWidgetDestination() {
        _pendingWidgetDestination.value = null
    }

    /**
     * Back to the launch state. This state used to live in the activity's
     * ViewModel and reset whenever a new MainActivity started, so the
     * activity-scoped ViewModel calls this from its init.
     */
    fun startSession() {
        _currentTab.value = AppTab.HOME
        _selectedTransactionId.value = null
        _pendingWidgetDestination.value = null
    }
}
