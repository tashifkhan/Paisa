package sh.taf.billi.ui.home

import sh.taf.billi.platform.formatFixed
import sh.taf.billi.platform.formatDate
import sh.taf.billi.platform.nowMillis
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sh.taf.billi.core.AppNavigator
import sh.taf.billi.core.PrivacySession
import sh.taf.billi.data.Account
import sh.taf.billi.core.AppTab
import sh.taf.billi.data.Category
import sh.taf.billi.data.DebtPersonBalance
import sh.taf.billi.data.Settings
import sh.taf.billi.data.TransactionWithDetails
import sh.taf.billi.data.repository.AccountRepository
import sh.taf.billi.data.repository.CategoryRepository
import sh.taf.billi.data.repository.DebtRepository
import sh.taf.billi.data.repository.SettingsRepository
import sh.taf.billi.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** State for the Home tab: the summary cards, search and filters, and the day-grouped list. */
class HomeViewModel(
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
    debtRepository: DebtRepository,
    settingsRepository: SettingsRepository,
    private val navigator: AppNavigator,
    private val privacySession: PrivacySession
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

    val balancesHidden: StateFlow<Boolean> = privacySession.balancesHidden

    fun setBalancesHidden(hidden: Boolean) = privacySession.set(hidden)

    // --- SEARCH & FILTERS ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _txFilters = MutableStateFlow(TxFilters())
    val txFilters: StateFlow<TxFilters> = _txFilters.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTxFilters(filters: TxFilters) {
        _txFilters.value = filters
    }

    fun clearTxFilters() {
        _txFilters.value = TxFilters()
        _searchQuery.value = ""
    }

    val filteredTransactions: StateFlow<List<TransactionWithDetails>> = combine(
        transactions,
        searchQuery,
        txFilters
    ) { txs, query, filters ->
        val q = query.trim()
        // Worked out per run so relative presets like "This month" stay current.
        val bounds = filters.dateBounds()
        txs.filter { tx ->
            (filters.type == null || tx.type == filters.type) &&
                (filters.categoryIds.isEmpty() || tx.categoryId in filters.categoryIds) &&
                (filters.accountIds.isEmpty() || tx.accountId in filters.accountIds) &&
                (filters.sources.isEmpty() || tx.source in filters.sources) &&
                (bounds == null || tx.transactionDate.take(10).let { it >= bounds.first && it <= bounds.second }) &&
                (filters.minAmount == null || tx.amount >= filters.minAmount) &&
                (filters.maxAmount == null || tx.amount <= filters.maxAmount) &&
                (q.isEmpty() || matchesQuery(tx, q))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun matchesQuery(tx: TransactionWithDetails, query: String): Boolean {
        return listOfNotNull(
            tx.merchantName,
            tx.note,
            tx.categoryName,
            tx.accountName,
            tx.bankName,
            tx.reference,
            tx.amount.toString(),
            formatFixed(tx.amount, 0)
        ).any { it.contains(query, ignoreCase = true) }
    }

    val homeSummary: StateFlow<HomeSummary> = combine(
        accounts,
        transactions,
        settings
    ) { accs, txs, settingsValue ->
        val currency = settingsValue?.currency ?: "₹"
        val (creditCards, bankAccounts) = accs.partition { it.type == "Credit Card" }
        val monthPrefix = formatDate(nowMillis(), "yyyy-MM")
        val monthTx = txs.filter { it.transactionDate.startsWith(monthPrefix) }
        HomeSummary(
            totalBalance = bankAccounts.sumOf { it.currentBalance },
            monthIncome = monthTx.filter { it.type == "income" }.sumOf { it.amount },
            monthExpense = monthTx.filter { it.type == "expense" }.sumOf { it.amount },
            currency = currency,
            // A negative card balance means money owed; flip it into a debt figure.
            creditCardDebt = creditCards.sumOf { -it.currentBalance }.coerceAtLeast(0.0),
            creditCardLimit = creditCards.sumOf { it.creditLimit ?: 0.0 },
            creditCards = creditCards
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeSummary())

    // --- NAVIGATION ---
    fun selectTab(tab: AppTab) = navigator.selectTab(tab)

    fun openTransaction(id: Int) = navigator.openTransaction(id)

    /** The transaction open in the detail pane, to highlight its row. */
    val selectedTransactionId = navigator.selectedTransactionId
}
