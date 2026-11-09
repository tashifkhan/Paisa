package codes.tashif.paisa.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.DebtEntry
import codes.tashif.paisa.data.DebtPerson
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.data.Settings
import codes.tashif.paisa.data.repository.DebtRepository
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

    fun openDebtPerson(id: Int?) {
        _openDebtPersonId.value = id
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
