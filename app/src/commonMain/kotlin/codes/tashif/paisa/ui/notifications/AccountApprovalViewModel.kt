package codes.tashif.paisa.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.notifications.AccountApproval
import codes.tashif.paisa.notifications.AwaitingAccount
import codes.tashif.paisa.data.repository.SettingsRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Payments from UPI and bank apps that wait for the user to say which account paid. */
class AccountApprovalViewModel(
    private val approval: AccountApproval,
    private val snackbar: SnackbarManager,
    settings: SettingsRepository
) : ViewModel() {

    val currency: StateFlow<String> = settings.settings.map { it?.currency ?: "₹" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "₹")

    val awaiting: StateFlow<List<AwaitingAccount>> = approval.awaiting
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val choices: StateFlow<List<Account>> = approval.choices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun assign(item: AwaitingAccount, account: Account, remember: Boolean) {
        viewModelScope.launch {
            val moved = approval.assign(item.transaction.id, account.id, remember)
            snackbar.show(
                when {
                    moved > 1 -> "Moved $moved ${item.appName} payments to ${account.name}"
                    remember -> "${item.appName} payments now go to ${account.name}"
                    else -> "Moved to ${account.name}"
                }
            )
        }
    }
}
