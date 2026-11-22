package sh.taf.billi.ui.transaction

import sh.taf.billi.notifications.UNASSIGNED_ACCOUNT_TYPE
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sh.taf.billi.core.AppNavigator
import sh.taf.billi.core.SnackbarManager
import sh.taf.billi.data.Account
import sh.taf.billi.data.Category
import sh.taf.billi.data.DebtEntryWithPerson
import sh.taf.billi.data.DebtPersonBalance
import sh.taf.billi.data.Settings
import sh.taf.billi.data.Transaction
import sh.taf.billi.data.TransactionWithDetails
import sh.taf.billi.data.repository.AccountRepository
import sh.taf.billi.data.repository.CategoryRepository
import sh.taf.billi.data.repository.DebtRepository
import sh.taf.billi.data.repository.DebtShare
import sh.taf.billi.data.repository.SettingsRepository
import sh.taf.billi.data.repository.TransactionRepository
import sh.taf.billi.data.repository.nowIso
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Adding, editing, deleting and linking transactions, for the add sheet and the detail screen. */
class TransactionViewModel(
    private val accountRepository: AccountRepository,
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

    /** Who the open transaction is split with, from its linked debt entries. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedSplit: StateFlow<List<DebtEntryWithPerson>> = navigator.selectedTransactionId
        .flatMapLatest { id ->
            flow { emit(id?.let { transactionRepository.getRawTransactionById(it)?.syncId }) }
        }
        .flatMapLatest { syncId -> if (syncId == null) flowOf(emptyList()) else debtRepository.transactionSplit(syncId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun closeTransaction() = navigator.closeTransaction()

    /** Shows the Debts tab. The caller opens the person on the activity's DebtsViewModel. */
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
        debtShares: List<DebtShare> = emptyList()
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
            if (debtShares.isNotEmpty()) saveSplit(transactionId.toInt(), debtShares)
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

    /**
     * Makes [shares] the transaction's split, replacing whatever it had.
     * Amounts are positive; the transaction's type sets the direction.
     */
    fun saveTransactionSplit(transactionId: Int, shares: List<DebtShare>) {
        viewModelScope.launch { saveSplit(transactionId, shares) }
    }

    fun removeTransactionSplit(transactionId: Int) {
        viewModelScope.launch {
            debtRepository.removeTransactionSplit(transactionId)
            showSnackbar("Split removed")
        }
    }

    private suspend fun saveSplit(transactionId: Int, shares: List<DebtShare>) {
        debtRepository.saveTransactionSplit(transactionId, shares)
        val single = shares.singleOrNull()
        val name = single?.newName ?: single?.let { s -> debtPeople.value.firstOrNull { it.person.id == s.personId }?.person?.name }
        showSnackbar(
            when {
                name != null -> "Recorded in Debts for $name"
                else -> "Recorded in Debts for ${shares.size} people"
            }
        )
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

    // --- BULK (multi-select on Home) ---

    /**
     * Applies [change] to each transaction in [ids] in one write, then offers
     * Undo, which writes the originals back. Rows [change] returns unchanged
     * are skipped, so the count in the message is what really moved.
     */
    private fun bulkEdit(ids: Collection<Int>, done: (Int) -> String, change: (Transaction) -> Transaction) {
        viewModelScope.launch {
            val originals = ids.mapNotNull { transactionRepository.getRawTransactionById(it) }
            val now = nowIso()
            val edited = originals.map { change(it) }
                .zip(originals)
                .filter { (new, old) -> new != old }
                .map { (new, _) -> new.copy(updatedAt = now) }
            if (edited.isEmpty()) {
                showSnackbar("Nothing to change")
                return@launch
            }
            val touched = edited.map { it.id }.toSet()
            transactionRepository.updateTransactions(edited)
            showSnackbar(done(edited.size), actionLabel = "Undo") {
                viewModelScope.launch {
                    transactionRepository.updateTransactions(originals.filter { it.id in touched })
                }
            }
        }
    }

    /** Sets each transaction's title. [names] maps id to its new name. */
    fun renameTransactions(names: Map<Int, String>) =
        bulkEdit(names.keys, { "Renamed ${plural(it, "transaction")}" }) { raw ->
            val name = names[raw.id]?.trim().orEmpty()
            if (name.isEmpty()) raw else raw.copy(merchantName = name)
        }

    /** Moves them all to one category. Its type (income or expense) comes along, as for a single edit. */
    fun setCategoryFor(ids: Collection<Int>, categoryId: Int) {
        viewModelScope.launch {
            val cat = categoryRepository.getCategoryById(categoryId) ?: return@launch
            bulkEdit(ids, { "Moved ${plural(it, "transaction")} to ${cat.name}" }) {
                it.copy(categoryId = cat.id, type = cat.type)
            }
        }
    }

    fun setAccountFor(ids: Collection<Int>, accountId: Int, accountName: String) =
        bulkEdit(ids, { "Moved ${plural(it, "transaction")} to $accountName" }) { it.copy(accountId = accountId) }

    fun deleteTransactions(ids: Collection<Int>) {
        viewModelScope.launch {
            val removed = transactionRepository.deleteTransactions(ids)
            if (navigator.selectedTransactionId.value in ids) navigator.closeTransaction()
            showSnackbar("Deleted ${plural(removed, "transaction")}")
        }
    }

    private fun plural(n: Int, word: String) = "$n $word" + if (n == 1) "" else "s"

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
        accountId: Int? = null,
        rememberRename: Boolean = false
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
            // Moved off an app's holding account: drop it once nothing waits there.
            if (newAccountId != raw.accountId) {
                accountRepository.getAccountById(raw.accountId)
                    ?.takeIf { it.type == UNASSIGNED_ACCOUNT_TYPE }
                    ?.let { accountRepository.deleteAccount(it) }
            }
            val merchant = newTitle.orEmpty()
            val oldName = raw.merchantName?.trim().orEmpty()
            val renamed = rememberRename && merchant.isNotEmpty() && oldName.isNotEmpty() && oldName != merchant
            // The rename first, so past rows carry the new name before the category rule matches it.
            if (renamed) categoryRepository.saveMerchantRename(oldName, merchant, applyToPast = true)
            if (rememberMerchant && merchant.isNotEmpty() && cat != null) {
                categoryRepository.saveMerchantMapping(
                    merchantName = merchant,
                    categoryName = cat.name,
                    categoryType = cat.type,
                    applyToPast = true
                )
            }
            showSnackbar(
                when {
                    renamed && rememberMerchant && cat != null ->
                        "Saved · $oldName is now always $merchant, in ${cat.name}"
                    renamed -> "Saved · $oldName is now always $merchant"
                    rememberMerchant && merchant.isNotEmpty() && cat != null ->
                        "Saved · always use ${cat.name} for $merchant"
                    else -> "Transaction updated"
                }
            )
        }
    }
}
