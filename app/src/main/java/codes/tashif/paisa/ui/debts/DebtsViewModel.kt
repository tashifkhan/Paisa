package codes.tashif.paisa.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.core.AppNavigator
import codes.tashif.paisa.data.AppTab
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.DebtEntry
import codes.tashif.paisa.data.DebtPerson
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.data.Settings
import codes.tashif.paisa.data.repository.DebtRepository
import codes.tashif.paisa.data.repository.DebtShare
import codes.tashif.paisa.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for the Debts tab. Scoped to MainActivity, so the open person survives
 * switching tabs. The shell reads [openDebtPersonId] to hide the FAB on a person page.
 */
@HiltViewModel
class DebtsViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    settingsRepository: SettingsRepository,
    private val navigator: AppNavigator,
    private val snackbarManager: SnackbarManager
) : ViewModel() {

    val debtPeople: StateFlow<List<DebtPersonBalance>> = debtRepository.debtPeople
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _openDebtPersonId = MutableStateFlow<Int?>(null)
    val openDebtPersonId: StateFlow<Int?> = _openDebtPersonId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val openDebtEntries: StateFlow<List<DebtEntry>> = _openDebtPersonId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else debtRepository.debtEntries(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Where Back goes from a person opened from a transaction's split card.
    private var returnTo: Pair<Int, AppTab>? = null

    fun openDebtPerson(id: Int?) {
        returnTo = null
        _openDebtPersonId.value = id
    }

    /** Opens [personId] from a transaction; Back then returns to that transaction. */
    fun openDebtPersonFromTransaction(personId: Int, transactionId: Int) {
        returnTo = transactionId to navigator.currentTab.value
        _openDebtPersonId.value = personId
        navigator.closeTransaction()
        navigator.selectTab(AppTab.DEBTS)
    }

    /** Closes the person page, going back to the transaction it was opened from, if any. */
    fun closeDebtPerson() {
        val back = returnTo
        returnTo = null
        _openDebtPersonId.value = null
        if (back != null) {
            navigator.selectTab(back.second)
            navigator.openTransaction(back.first)
        }
    }

    fun addDebtPerson(name: String, startingAmount: Double) {
        viewModelScope.launch {
            debtRepository.addDebtPerson(name, startingAmount)
            snackbarManager.show("Added ${name.trim()}")
        }
    }

    /** [amount] > 0: they owe you more. < 0: they owe you less, or you owe them. */
    fun addDebtEntry(personId: Int, amount: Double, note: String) {
        viewModelScope.launch { debtRepository.addDebtEntry(personId, amount, note) }
    }

    /** Changes an entry added by hand. [amount] is signed like [addDebtEntry]. */
    fun updateDebtEntry(id: Int, amount: Double, note: String, entryDate: String) {
        viewModelScope.launch { debtRepository.updateDebtEntry(id, amount, note, entryDate) }
    }

    /**
     * Opens the transaction a debt entry came from. Back from it returns here,
     * since the open person stays set.
     */
    fun openLinkedTransaction(transactionSyncId: String) {
        viewModelScope.launch {
            val id = debtRepository.transactionIdForSyncId(transactionSyncId)
            if (id != null) navigator.openTransaction(id) else snackbarManager.show("That transaction is gone")
        }
    }

    /** A bill split with no transaction. [shares] are signed: > 0 they owe you. */
    fun recordBillSplit(shares: List<DebtShare>, note: String, entryDate: String) {
        viewModelScope.launch {
            debtRepository.recordBillSplit(shares, note, entryDate)
            snackbarManager.show("Split recorded")
        }
    }

    fun deleteDebtEntry(id: Int) {
        viewModelScope.launch { debtRepository.deleteDebtEntry(id) }
    }

    fun settleDebt(person: DebtPerson) {
        viewModelScope.launch {
            debtRepository.settleDebt(person.id)
            snackbarManager.show("Settled up with ${person.name}")
        }
    }

    fun renameDebtPerson(person: DebtPerson, name: String) {
        viewModelScope.launch { debtRepository.renameDebtPerson(person, name) }
    }

    fun deleteDebtPerson(person: DebtPerson) {
        viewModelScope.launch {
            if (_openDebtPersonId.value == person.id) _openDebtPersonId.value = null
            debtRepository.deleteDebtPerson(person)
            snackbarManager.show("Removed ${person.name}")
        }
    }
}
