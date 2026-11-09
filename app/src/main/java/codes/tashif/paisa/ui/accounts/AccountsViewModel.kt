package codes.tashif.paisa.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.core.AppNavigator
import codes.tashif.paisa.core.PrivacySession
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.data.Settings
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.data.repository.AccountRepository
import codes.tashif.paisa.data.repository.SettingsRepository
import codes.tashif.paisa.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State and actions for the Accounts tab and the add-account sheet. */
@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository,
    private val navigator: AppNavigator,
    private val snackbarManager: SnackbarManager,
    privacySession: PrivacySession
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Transactions keyed by account id, newest first. The card view looks up the
     * account in focus synchronously, so swiping the carousel never shows a stale list.
     */
    val transactionsByAccount: StateFlow<Map<Int, List<TransactionWithDetails>>> =
        transactionRepository.allTransactions
            .map { txs -> txs.groupBy { it.accountId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val balancesHidden: StateFlow<Boolean> = privacySession.balancesHidden

    fun openTransaction(id: Int) = navigator.openTransaction(id)

    fun addAccount(name: String, type: String, openingBalance: Double, icon: String, color: String) {
        viewModelScope.launch {
            val position = accountRepository.allAccounts.first().maxOfOrNull { it.orderIndex + 1 } ?: 0
            accountRepository.insertAccount(
                Account(
                    name = name.trim(),
                    type = type,
                    openingBalance = openingBalance,
                    currentBalance = openingBalance,
                    icon = icon,
                    color = color,
                    orderIndex = position
                )
            )
            snackbarManager.show("Account added")
        }
    }

    fun deleteAccountCascade(account: Account) {
        viewModelScope.launch {
            accountRepository.deleteAccountCascade(account)
            snackbarManager.show("Deleted ${account.name} and its transactions")
        }
    }

    fun setDefaultAccount(account: Account) {
        viewModelScope.launch {
            accountRepository.setDefaultAccount(account.id)
            snackbarManager.show("${account.name} is now the default account")
        }
    }

    fun reorderAccounts(orderedIds: List<Int>) {
        viewModelScope.launch {
            accountRepository.reorderAccounts(orderedIds)
        }
    }

    fun autoMergeDuplicateAccounts() {
        viewModelScope.launch {
            val merged = accountRepository.autoMergeDuplicateAccounts()
            snackbarManager.show(
                if (merged > 0) "Merged $merged duplicate accounts" else "No duplicates found"
            )
        }
    }

    fun mergeAccounts(targetId: Int, sourceIds: List<Int>) {
        viewModelScope.launch {
            accountRepository.mergeAccounts(targetId, sourceIds)
            snackbarManager.show("Merged ${sourceIds.size} accounts")
        }
    }

    fun renameAccount(account: Account, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == account.name) return
        viewModelScope.launch {
            accountRepository.renameAccount(account.id, trimmed)
            snackbarManager.show("Account renamed to $trimmed")
        }
    }

    fun updateAccountBalance(account: Account, balance: Double) {
        if (!balance.isFinite() || balance == account.currentBalance) return
        viewModelScope.launch {
            accountRepository.updateCurrentBalance(account.id, balance)
            snackbarManager.show("Balance updated for ${account.name}")
        }
    }
}
