package codes.tashif.paisa.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.core.AppNavigator
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.data.Category
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.data.Settings
import codes.tashif.paisa.data.Transaction
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.data.repository.AccountRepository
import codes.tashif.paisa.data.repository.CategoryRepository
import codes.tashif.paisa.data.repository.DebtRepository
import codes.tashif.paisa.data.repository.SettingsRepository
import codes.tashif.paisa.data.repository.TransactionRepository
import codes.tashif.paisa.data.repository.nowIso
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Adding, editing, deleting and linking transactions, for the add sheet and the detail screen. */
@HiltViewModel
class TransactionViewModel @Inject constructor(
    accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val debtRepository: DebtRepository,
    settingsRepository: SettingsRepository,
    private val navigator: AppNavigator,
    private val snackbarManager: SnackbarManager
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transactions: StateFlow<List<TransactionWithDetails>> = transactionRepository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val debtPeople: StateFlow<List<DebtPersonBalance>> = debtRepository.debtPeople
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedTransactionId: StateFlow<Int?> = navigator.selectedTransactionId

    fun closeTransaction() = navigator.closeTransaction()

    fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) = snackbarManager.show(message, actionLabel, onAction)

    fun addTransaction(
        amount: Double,
        type: String,
        categoryId: Int,
        accountId: Int,
        note: String,
        transactionDate: String = nowIso(),
        merchantName: String? = null,
        rememberMerchant: Boolean = false,
        personLink: PersonLink? = null
    ) {
        viewModelScope.launch {
            // Title maps to merchantName; note stays a separate free-text field.
            val merchant = merchantName?.trim()?.takeIf { it.isNotBlank() }
            val transactionId = transactionRepository.insertTransaction(
                Transaction(
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    note = note,
                    transactionDate = transactionDate,
                    createdAt = nowIso(),
                    updatedAt = nowIso(),
                    merchantName = merchant,
                    source = "manual"
                )
            )
            personLink?.let { link -> linkToDebt(transactionId.toInt(), link) }
            if (rememberMerchant && merchant != null) {
                val cat = categoryRepository.getCategoryById(categoryId)
                if (cat != null) {
                    categoryRepository.saveMerchantMapping(
                        merchantName = merchant,
                        categoryName = cat.name,
                        categoryType = type,
                        applyToPast = false
                    )
                }
            }
            showSnackbar("Transaction saved")
        }
    }

    /** Ties an existing transaction (a UPI payment, a bill you covered) to someone in Debts. */
    fun linkTransactionToDebt(transactionId: Int, link: PersonLink) {
        viewModelScope.launch { linkToDebt(transactionId, link) }
    }

    private suspend fun linkToDebt(transactionId: Int, link: PersonLink) {
        debtRepository.linkTransactionToDebt(transactionId, link.personId, link.newName, link.amount)
        val name = link.newName ?: debtPeople.value.firstOrNull { it.person.id == link.personId }?.person?.name
        showSnackbar("Recorded in Debts" + (name?.let { " for $it" } ?: ""))
    }

    fun updateTransactionCategory(
        transactionId: Int,
        categoryId: Int,
        rememberMerchant: Boolean
    ) {
        viewModelScope.launch {
            val raw = transactionRepository.getRawTransactionById(transactionId) ?: return@launch
            val cat = categoryRepository.getCategoryById(categoryId) ?: return@launch
            transactionRepository.updateTransaction(
                raw.copy(
                    categoryId = categoryId,
                    type = cat.type,
                    updatedAt = nowIso()
                )
            )
            val merchant = raw.merchantName?.trim().orEmpty()
            if (rememberMerchant && merchant.isNotEmpty()) {
                categoryRepository.saveMerchantMapping(
                    merchantName = merchant,
                    categoryName = cat.name,
                    categoryType = cat.type,
                    applyToPast = true
                )
                showSnackbar("Category updated · always use for $merchant")
            } else {
                showSnackbar("Category updated")
            }
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
            if (navigator.selectedTransactionId.value == id) {
                navigator.closeTransaction()
            }
            showSnackbar("Transaction deleted")
        }
    }

    /**
     * Saves all edits from the detail screen in one write: title (merchant),
     * note, category, and optionally amount/date/account for manual entries.
     * [categoryId] of null means "leave category as is".
     */
    fun saveTransactionEdits(
        transactionId: Int,
        title: String,
        note: String,
        categoryId: Int?,
        rememberMerchant: Boolean,
        amount: Double? = null,
        transactionDate: String? = null,
        accountId: Int? = null
    ) {
        viewModelScope.launch {
            val raw = transactionRepository.getRawTransactionById(transactionId) ?: return@launch
            val newTitle = title.trim().takeIf { it.isNotBlank() }
            val cat = categoryId?.let { categoryRepository.getCategoryById(it) }
            val newAmount = amount?.takeIf { it > 0.0 } ?: raw.amount
            val newDate = transactionDate?.takeIf { it.isNotBlank() } ?: raw.transactionDate
            val newAccountId = accountId ?: raw.accountId
            transactionRepository.updateTransaction(
                raw.copy(
                    merchantName = newTitle,
                    note = note.trim(),
                    categoryId = cat?.id ?: raw.categoryId,
                    type = cat?.type ?: raw.type,
                    amount = newAmount,
                    transactionDate = newDate,
                    accountId = newAccountId,
                    updatedAt = nowIso()
                )
            )
            val merchant = newTitle.orEmpty()
            if (rememberMerchant && merchant.isNotEmpty() && cat != null) {
                categoryRepository.saveMerchantMapping(
                    merchantName = merchant,
                    categoryName = cat.name,
                    categoryType = cat.type,
                    applyToPast = true
                )
                showSnackbar("Saved · always use ${cat.name} for $merchant")
            } else {
                showSnackbar("Transaction updated")
            }
        }
    }
}
