package com.paisa.app.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.paisa.app.sms.SmsReaderWorker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

data class HomeSummary(
    val totalBalance: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val currency: String = "₹"
)

data class SmsScanProgress(
    val isRunning: Boolean = false,
    val total: Int = 0,
    val processed: Int = 0,
    val saved: Int = 0,
    val duplicates: Int = 0,
    val unrecognized: Int = 0
)

class PaisaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = Repository(db)
    private val workManager = WorkManager.getInstance(application)

    // --- NAVIGATION ---
    private val _currentTab = MutableStateFlow(0)
    val currentTab = _currentTab.asStateFlow()

    fun selectTab(index: Int) {
        _currentTab.value = index
    }

    // --- SCROLL (FAB hide) ---
    private val _isScrolling = MutableStateFlow(false)
    val isScrolling = _isScrolling.asStateFlow()

    fun setScrolling(scrolling: Boolean) {
        _isScrolling.value = scrolling
    }

    // --- SNACKBAR ---
    private val _snackbarEvent = MutableSharedFlow<SnackbarEvent>()
    val snackbarEvent = _snackbarEvent.asSharedFlow()

    fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _snackbarEvent.emit(SnackbarEvent(message, actionLabel, onAction))
        }
    }

    // --- DATA STREAMS ---
    val accounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transactions: StateFlow<List<TransactionWithDetails>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val budgets: StateFlow<List<Budget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savingsGoals: StateFlow<List<SavingsGoal>> = repository.allSavingsGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<Settings?> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val unrecognizedSmsCount: StateFlow<Int> = repository.pendingUnrecognizedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val unrecognizedSms: StateFlow<List<UnrecognizedSms>> = repository.pendingUnrecognizedSms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val smsScanProgress: StateFlow<SmsScanProgress> = workManager
        .getWorkInfosForUniqueWorkFlow(SmsReaderWorker.WORK_NAME)
        .map { infos ->
            val info = infos.firstOrNull()
            val running = info?.state == WorkInfo.State.RUNNING ||
                info?.state == WorkInfo.State.ENQUEUED
            val progress = info?.progress
            val output = info?.outputData
            val data = if (info?.state == WorkInfo.State.SUCCEEDED) output else progress
            SmsScanProgress(
                isRunning = running,
                total = data?.getInt(SmsReaderWorker.PROGRESS_TOTAL, 0) ?: 0,
                processed = data?.getInt(SmsReaderWorker.PROGRESS_PROCESSED, 0) ?: 0,
                saved = data?.getInt(SmsReaderWorker.PROGRESS_SAVED, 0) ?: 0,
                duplicates = data?.getInt(SmsReaderWorker.PROGRESS_DUPLICATES, 0) ?: 0,
                unrecognized = data?.getInt(SmsReaderWorker.PROGRESS_UNRECOGNIZED, 0) ?: 0
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SmsScanProgress())

    val homeSummary: StateFlow<HomeSummary> = combine(
        accounts,
        transactions,
        settings
    ) { accs, txs, settingsValue ->
        val currency = settingsValue?.currency ?: "₹"
        val totalBalance = accs.sumOf { it.currentBalance }
        val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val monthTx = txs.filter { it.transactionDate.startsWith(monthPrefix) }
        HomeSummary(
            totalBalance = totalBalance,
            monthIncome = monthTx.filter { it.type == "income" }.sumOf { it.amount },
            monthExpense = monthTx.filter { it.type == "expense" }.sumOf { it.amount },
            currency = currency
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeSummary())

    init {
        viewModelScope.launch {
            repository.processRecurringTransactions()
        }
    }

    // --- MUTATIONS ---
    fun addTransaction(
        amount: Double,
        type: String,
        categoryId: Int,
        accountId: Int,
        note: String,
        transactionDate: String = nowIso()
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    note = note,
                    transactionDate = transactionDate,
                    createdAt = nowIso(),
                    updatedAt = nowIso()
                )
            )
            showSnackbar("Transaction saved")
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            showSnackbar("Transaction deleted")
        }
    }

    fun addAccount(name: String, type: String, openingBalance: Double, icon: String, color: String) {
        viewModelScope.launch {
            repository.insertAccount(
                Account(
                    name = name,
                    type = type,
                    openingBalance = openingBalance,
                    currentBalance = openingBalance,
                    icon = icon,
                    color = color
                )
            )
            showSnackbar("Account added")
        }
    }

    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    // --- SMS ---
    fun startSmsScan(forceFull: Boolean = false) {
        SmsReaderWorker.enqueue(getApplication(), forceFull = forceFull)
        showSnackbar(if (forceFull) "Full SMS scan started" else "SMS scan started")
    }

    fun cancelSmsScan() {
        SmsReaderWorker.cancel(getApplication())
        showSnackbar("SMS scan cancelled")
    }

    fun markUnrecognizedReviewed(id: Int) {
        viewModelScope.launch {
            repository.markUnrecognizedReviewed(id)
        }
    }

    fun deleteUnrecognized(id: Int) {
        viewModelScope.launch {
            repository.deleteUnrecognized(id)
        }
    }

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
}
