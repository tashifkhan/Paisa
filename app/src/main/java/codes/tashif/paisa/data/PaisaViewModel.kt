package codes.tashif.paisa.data

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import codes.tashif.paisa.ai.AiCredentials
import codes.tashif.paisa.ai.AiCredentialsStore
import codes.tashif.paisa.ai.AiProvider
import codes.tashif.paisa.ai.AiTestResult
import codes.tashif.paisa.ai.ExtractedTransaction
import codes.tashif.paisa.ai.LlmClient
import codes.tashif.paisa.ai.StatementExtractionService
import codes.tashif.paisa.ai.StatementImportUseCase
import codes.tashif.paisa.ai.StatementNotifications
import codes.tashif.paisa.sms.SmsReaderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

data class HomeSummary(
    /** Sum across all non-credit-card accounts. */
    val totalBalance: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val currency: String = "₹",
    /** Credit card outstanding (positive number = amount owed). */
    val creditCardDebt: Double = 0.0,
    /** Combined limit across credit cards, 0 when unknown. */
    val creditCardLimit: Double = 0.0,
    val creditCards: List<Account> = emptyList()
)

data class TxFilters(
    val type: String? = null, // income | expense
    /** Empty = any category; non-empty = match any selected (OR). */
    val categoryIds: Set<Int> = emptySet(),
    /** Empty = any account; non-empty = match any selected (OR). */
    val accountIds: Set<Int> = emptySet(),
    val source: String? = null // sms | manual | statement
) {
    val isActive: Boolean
        get() = type != null || categoryIds.isNotEmpty() || accountIds.isNotEmpty() || source != null
}

data class SmsScanProgress(
    val isRunning: Boolean = false,
    val total: Int = 0,
    val processed: Int = 0,
    val saved: Int = 0,
    val duplicates: Int = 0,
    val unrecognized: Int = 0
)

sealed class StatementUiState {
    data object Idle : StatementUiState()
    data class Extracting(
        val completedChunks: Int = 0,
        val totalChunks: Int = 0,
        val currentFile: Int = 1,
        val totalFiles: Int = 1
    ) : StatementUiState()

    data class Preview(
        val rows: List<ExtractedTransaction>,
        val chunksProcessed: Int,
        val filesProcessed: Int = 1,
        val failedFiles: List<String> = emptyList()
    ) : StatementUiState()

    data class Success(val imported: Int, val duplicates: Int, val skipped: Int) : StatementUiState()
    data class Error(val message: String) : StatementUiState()
}

class PaisaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = Repository(db)
    private val workManager = WorkManager.getInstance(application)
    private val aiCredentialsStore = AiCredentialsStore(application)
    private val llmClient = LlmClient()
    private val statementExtraction = StatementExtractionService(llmClient)
    private val statementImport = StatementImportUseCase(repository)
    private var lastForegroundScanRequestAt = 0L

    // --- NAVIGATION ---
    private val _currentTab = MutableStateFlow(0)
    val currentTab = _currentTab.asStateFlow()

    fun selectTab(index: Int) {
        _currentTab.value = index
    }

    /**
     * Destination requested by a home screen widget, held until the UI shows it.
     *
     * Kept in the ViewModel rather than read straight from the Intent so it
     * survives the biometric lock screen — the widget tap still lands on the
     * right screen after the user unlocks.
     */
    private val _pendingWidgetDestination = MutableStateFlow<String?>(null)
    val pendingWidgetDestination: StateFlow<String?> = _pendingWidgetDestination.asStateFlow()

    fun requestWidgetDestination(destination: String?) {
        if (destination != null) _pendingWidgetDestination.value = destination
    }

    fun consumeWidgetDestination() {
        _pendingWidgetDestination.value = null
    }

    // --- PRIVACY: balance visibility (session) ---
    private val _balancesHidden = MutableStateFlow(true)
    val balancesHidden: StateFlow<Boolean> = _balancesHidden.asStateFlow()
    private var balanceVisibilitySeeded = false

    fun toggleBalancesHidden() {
        _balancesHidden.value = !_balancesHidden.value
    }

    fun setBalancesHidden(hidden: Boolean) {
        _balancesHidden.value = hidden
    }

    fun setHideBalancesByDefault(hide: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: return@launch
            repository.updateSettings(current.copy(hideBalancesByDefault = hide))
            _balancesHidden.value = hide
        }
    }

    // --- APP LOCK ---
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()
    private var appLockSeeded = false

    /**
     * Set when we deliberately hand off to another app (file picker, export sheet).
     * That stops our activity, which would otherwise trip the lock and dump the user
     * back on the unlock screen mid-flow.
     */
    private var externalActivityStartedAt: Long? = null

    fun unlockApp() {
        _isLocked.value = false
    }

    /** Call right before launching a picker/sheet so the resulting onStop doesn't lock. */
    fun onExternalActivityLaunched() {
        externalActivityStartedAt = System.currentTimeMillis()
    }

    fun lockApp() {
        if (externalActivityStartedAt != null) return
        if (settings.value?.biometricEnabled == true) {
            _isLocked.value = true
        }
    }

    /**
     * On return to the foreground, honour the deferred lock if the detour outlived the
     * grace period — the user may have wandered off from the picker rather than come back.
     */
    fun onReturnToForeground() {
        val startedAt = externalActivityStartedAt ?: return
        externalActivityStartedAt = null
        if (System.currentTimeMillis() - startedAt > EXTERNAL_ACTIVITY_GRACE_MS) {
            lockApp()
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: return@launch
            repository.updateSettings(current.copy(biometricEnabled = enabled))
            if (enabled) {
                // Stay unlocked for this session after the user just authenticated to enable.
                _isLocked.value = false
            } else {
                _isLocked.value = false
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: return@launch
            repository.updateSettings(current.copy(onboardingCompleted = true))
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: return@launch
            repository.updateSettings(current.copy(onboardingCompleted = false))
        }
    }

    init {
        viewModelScope.launch {
            repository.appSettings.collect { settingsValue ->
                if (settingsValue == null) return@collect
                if (!balanceVisibilitySeeded) {
                    _balancesHidden.value = settingsValue.hideBalancesByDefault
                    balanceVisibilitySeeded = true
                }
                if (!appLockSeeded) {
                    _isLocked.value = settingsValue.biometricEnabled
                    appLockSeeded = true
                }
            }
        }
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

    val merchantMappings: StateFlow<List<MerchantMapping>> = repository.allMerchantMappings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _aiCredentials = MutableStateFlow(aiCredentialsStore.load())
    val aiCredentials: StateFlow<AiCredentials> = _aiCredentials.asStateFlow()

    private val _statementUiState = MutableStateFlow<StatementUiState>(StatementUiState.Idle)
    val statementUiState: StateFlow<StatementUiState> = _statementUiState.asStateFlow()

    private val _aiTestResult = MutableStateFlow<AiTestResult?>(null)
    val aiTestResult: StateFlow<AiTestResult?> = _aiTestResult.asStateFlow()

    private val _selectedTransactionId = MutableStateFlow<Int?>(null)
    val selectedTransactionId: StateFlow<Int?> = _selectedTransactionId.asStateFlow()

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
        txs.filter { tx ->
            (filters.type == null || tx.type == filters.type) &&
                (filters.categoryIds.isEmpty() || tx.categoryId in filters.categoryIds) &&
                (filters.accountIds.isEmpty() || tx.accountId in filters.accountIds) &&
                (filters.source == null || tx.source == filters.source) &&
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
            "%.0f".format(tx.amount)
        ).any { it.contains(query, ignoreCase = true) }
    }

    val smsScanProgress: StateFlow<SmsScanProgress> = workManager
        .getWorkInfosForUniqueWorkFlow(SmsReaderWorker.WORK_NAME)
        .map { infos ->
            // REPLACE creates a new WorkSpec. Prefer active work so an older
            // completed generation cannot make the home rescan button look idle.
            val info = infos.firstOrNull {
                it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
            } ?: infos.firstOrNull()
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
        val (creditCards, bankAccounts) = accs.partition { it.type == "Credit Card" }
        val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
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

    init {
        viewModelScope.launch {
            repository.processRecurringTransactions()
        }
        // Keep home screen widgets in step with in-app edits; account rows carry
        // the running balance, so this covers transaction changes too.
        viewModelScope.launch {
            repository.allAccounts.collect {
                codes.tashif.paisa.widget.PaisaWidgets.refresh(getApplication())
            }
        }
    }

    // --- MUTATIONS ---
    fun addTransaction(
        amount: Double,
        type: String,
        categoryId: Int,
        accountId: Int,
        note: String,
        transactionDate: String = nowIso(),
        merchantName: String? = null,
        rememberMerchant: Boolean = false
    ) {
        viewModelScope.launch {
            // Title maps to merchantName; note stays a separate free-text field.
            val merchant = merchantName?.trim()?.takeIf { it.isNotBlank() }
            repository.insertTransaction(
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
            if (rememberMerchant && merchant != null) {
                val cat = repository.getCategoryById(categoryId)
                if (cat != null) {
                    repository.saveMerchantMapping(
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

    fun openTransaction(id: Int) {
        _selectedTransactionId.value = id
    }

    fun closeTransaction() {
        _selectedTransactionId.value = null
    }

    fun updateTransactionCategory(
        transactionId: Int,
        categoryId: Int,
        rememberMerchant: Boolean
    ) {
        viewModelScope.launch {
            val raw = repository.getRawTransactionById(transactionId) ?: return@launch
            val cat = repository.getCategoryById(categoryId) ?: return@launch
            repository.updateTransaction(
                raw.copy(
                    categoryId = categoryId,
                    type = cat.type,
                    updatedAt = nowIso()
                )
            )
            val merchant = raw.merchantName?.trim().orEmpty()
            if (rememberMerchant && merchant.isNotEmpty()) {
                repository.saveMerchantMapping(
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
            repository.deleteTransaction(id)
            if (_selectedTransactionId.value == id) {
                _selectedTransactionId.value = null
            }
            showSnackbar("Transaction deleted")
        }
    }

    fun addAccount(name: String, type: String, openingBalance: Double, icon: String, color: String) {
        viewModelScope.launch {
            val position = accounts.value.maxOfOrNull { it.orderIndex + 1 } ?: 0
            repository.insertAccount(
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
            showSnackbar("Account added")
        }
    }

    fun deleteAccountCascade(account: Account) {
        viewModelScope.launch {
            repository.deleteAccountCascade(account)
            showSnackbar("Deleted ${account.name} and its transactions")
        }
    }

    fun setDefaultAccount(account: Account) {
        viewModelScope.launch {
            repository.setDefaultAccount(account.id)
            showSnackbar("${account.name} is now the default account")
        }
    }

    fun reorderAccounts(orderedIds: List<Int>) {
        viewModelScope.launch {
            repository.reorderAccounts(orderedIds)
        }
    }

    fun autoMergeDuplicateAccounts() {
        viewModelScope.launch {
            val merged = repository.autoMergeDuplicateAccounts()
            showSnackbar(
                if (merged > 0) "Merged $merged duplicate accounts" else "No duplicates found"
            )
        }
    }

    fun mergeAccounts(targetId: Int, sourceIds: List<Int>) {
        viewModelScope.launch {
            repository.mergeAccounts(targetId, sourceIds)
            showSnackbar("Merged ${sourceIds.size} accounts")
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
            val raw = repository.getRawTransactionById(transactionId) ?: return@launch
            val newTitle = title.trim().takeIf { it.isNotBlank() }
            val cat = categoryId?.let { repository.getCategoryById(it) }
            val newAmount = amount?.takeIf { it > 0.0 } ?: raw.amount
            val newDate = transactionDate?.takeIf { it.isNotBlank() } ?: raw.transactionDate
            val newAccountId = accountId ?: raw.accountId
            repository.updateTransaction(
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
                repository.saveMerchantMapping(
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

    fun renameAccount(account: Account, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == account.name) return
        viewModelScope.launch {
            repository.updateAccount(account.copy(name = trimmed))
            showSnackbar("Account renamed to $trimmed")
        }
    }

    fun updateAccountBalance(account: Account, balance: Double) {
        if (!balance.isFinite() || balance == account.currentBalance) return
        viewModelScope.launch {
            repository.updateCurrentBalance(account.id, balance)
            showSnackbar("Balance updated for ${account.name}")
        }
    }

    fun addBudget(categoryId: Int?, amount: Double, name: String?) {
        viewModelScope.launch {
            val cal = java.util.Calendar.getInstance()
            repository.insertBudget(
                Budget(
                    categoryId = categoryId,
                    budgetAmount = amount,
                    month = cal.get(java.util.Calendar.MONTH) + 1,
                    year = cal.get(java.util.Calendar.YEAR),
                    budgetName = name?.trim()?.takeIf { it.isNotBlank() }
                )
            )
            showSnackbar("Budget added")
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
            showSnackbar("Budget deleted")
        }
    }

    // --- CATEGORIES ---
    fun addCategory(name: String, type: String, color: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.insertCategory(
                Category(
                    name = trimmed,
                    type = type,
                    icon = "category",
                    color = color,
                    orderIndex = categories.value.size
                )
            )
            showSnackbar("Category added")
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            val deleted = repository.deleteCategory(category)
            if (deleted) {
                showSnackbar("Category deleted")
            } else {
                showSnackbar("${category.name} has transactions — reassign them first")
            }
        }
    }

    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    // --- MERCHANT MAPPINGS ---
    fun saveMerchantMapping(merchantName: String, categoryName: String, categoryType: String) {
        viewModelScope.launch {
            repository.saveMerchantMapping(merchantName, categoryName, categoryType, applyToPast = true)
            showSnackbar("Saved rule for $merchantName")
        }
    }

    fun deleteMerchantMapping(merchantName: String) {
        viewModelScope.launch {
            repository.deleteMerchantMapping(merchantName)
            showSnackbar("Removed mapping")
        }
    }

    // --- AI CREDENTIALS ---
    fun saveAiCredentials(credentials: AiCredentials) {
        aiCredentialsStore.save(credentials)
        _aiCredentials.value = aiCredentialsStore.load()
        showSnackbar("AI settings saved")
    }

    fun clearAiCredentials() {
        aiCredentialsStore.clear()
        _aiCredentials.value = aiCredentialsStore.load()
        showSnackbar("AI key cleared")
    }

    fun testAiConnection(credentials: AiCredentials) {
        viewModelScope.launch {
            _aiTestResult.value = AiTestResult("Testing…", isError = false)
            val result = llmClient.testConnection(credentials)
            _aiTestResult.value = result.fold(
                onSuccess = {
                    AiTestResult("Connected · reply: ${it.take(80)}", isError = false)
                },
                onFailure = {
                    AiTestResult(
                        "Failed: ${it.message ?: "unknown error"}",
                        isError = true
                    )
                }
            )
        }
    }

    fun clearAiTestResult() {
        _aiTestResult.value = null
    }

    // --- STATEMENT IMPORT ---
    fun extractStatements(uris: List<Uri>) {
        if (uris.isEmpty()) return
        // Runs on a background dispatcher in the ViewModel scope: the user can leave
        // the screen (or the app) while chunks process; a notification announces completion.
        viewModelScope.launch(Dispatchers.Default) {
            val creds = _aiCredentials.value
            if (!creds.isConfigured) {
                _statementUiState.value = StatementUiState.Error(
                    "Add an API key under AI provider settings first."
                )
                return@launch
            }
            _statementUiState.value = StatementUiState.Extracting(totalFiles = uris.size)
            val app = getApplication<Application>()
            val merged = LinkedHashMap<String, ExtractedTransaction>()
            var chunksProcessed = 0
            val failedFiles = mutableListOf<String>()
            var lastErrorMessage: String? = null

            uris.forEachIndexed { fileIndex, uri ->
                try {
                    runCatching {
                        app.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    val result = statementExtraction.extractFromUri(
                        context = app,
                        uri = uri,
                        credentials = creds,
                        onProgress = { completed, total ->
                            _statementUiState.value = StatementUiState.Extracting(
                                completedChunks = completed,
                                totalChunks = total,
                                currentFile = fileIndex + 1,
                                totalFiles = uris.size
                            )
                        }
                    )
                    chunksProcessed += result.chunksProcessed
                    // Identical rows appearing in two files (e.g. overlapping
                    // statement periods) collapse into one preview entry.
                    for (tx in result.transactions) {
                        merged.putIfAbsent(StatementExtractionService.dedupeKey(tx), tx)
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastErrorMessage = e.message
                    failedFiles += statementFileName(uri, fileIndex)
                }
            }

            if (merged.isEmpty()) {
                val message = lastErrorMessage
                    ?: "No transactions found. Try a clearer PDF/CSV or another model."
                _statementUiState.value = StatementUiState.Error(message)
                StatementNotifications.notifyExtractionFailed(app, message)
            } else {
                // Enrich categories via local mapping when model omitted them,
                // and flag rows that look like transactions already in the ledger
                // (e.g. captured earlier from SMS).
                val enriched = merged.values.sortedByDescending { it.date }.map { row ->
                    val withCategory = if (row.categoryName.isNullOrBlank()) {
                        val name = repository.resolveCategoryName(row.merchant, row.type)
                        row.copy(categoryName = name)
                    } else {
                        row.copy(
                            categoryName = repository.canonicalizeCategoryName(
                                row.categoryName,
                                row.type
                            )
                        )
                    }
                    val existing = repository.findSimilarTransaction(
                        amount = row.amount,
                        type = row.type,
                        dateIso = row.date
                    )
                    if (existing != null) {
                        val sourceLabel = when (existing.source) {
                            "sms" -> "SMS entry"
                            "statement" -> "earlier import"
                            else -> "manual entry"
                        }
                        withCategory.copy(
                            likelyDuplicate = true,
                            selected = false,
                            duplicateNote = "Matches $sourceLabel" +
                                " on ${existing.transactionDate.take(10)}"
                        )
                    } else {
                        withCategory
                    }
                }
                _statementUiState.value = StatementUiState.Preview(
                    rows = enriched,
                    chunksProcessed = chunksProcessed,
                    filesProcessed = uris.size - failedFiles.size,
                    failedFiles = failedFiles
                )
                StatementNotifications.notifyExtractionDone(
                    app,
                    found = enriched.size,
                    duplicates = enriched.count { it.likelyDuplicate }
                )
            }
        }
    }

    private fun statementFileName(uri: Uri, fileIndex: Int): String {
        val resolved = runCatching {
            getApplication<Application>().contentResolver
                .query(uri, null, null, null, null)?.use { cursor ->
                    val col = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (col >= 0 && cursor.moveToFirst()) cursor.getString(col) else null
                }
        }.getOrNull()
        return resolved ?: "File ${fileIndex + 1}"
    }

    fun updateStatementPreview(rows: List<ExtractedTransaction>) {
        val current = _statementUiState.value
        if (current is StatementUiState.Preview) {
            _statementUiState.value = current.copy(rows = rows)
        }
    }

    fun setAllStatementRowsSelected(selected: Boolean) {
        val current = _statementUiState.value
        if (current is StatementUiState.Preview) {
            _statementUiState.value = current.copy(
                rows = current.rows.map { it.copy(selected = selected) }
            )
        }
    }

    fun commitStatementImport(accountId: Int?) {
        viewModelScope.launch {
            val preview = _statementUiState.value as? StatementUiState.Preview ?: return@launch
            try {
                val targetAccount = accountId
                    ?: statementImport.ensureImportAccount()
                val result = statementImport.commit(preview.rows, targetAccount)
                _statementUiState.value = StatementUiState.Success(
                    imported = result.imported,
                    duplicates = result.duplicates,
                    skipped = result.skipped
                )
                showSnackbar("Imported ${result.imported} transactions")
            } catch (e: Exception) {
                _statementUiState.value = StatementUiState.Error(
                    e.message ?: "Import failed"
                )
            }
        }
    }

    fun resetStatementUi() {
        _statementUiState.value = StatementUiState.Idle
    }

    // --- SMS ---
    fun startSmsScan(forceFull: Boolean = false) {
        val application = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(application, Manifest.permission.READ_SMS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            showSnackbar("Grant SMS permission to scan your inbox")
            return
        }
        SmsReaderWorker.enqueue(application, forceFull = forceFull)
        showSnackbar(if (forceFull) "Full SMS scan started" else "SMS scan started")
    }

    /**
     * Incrementally scans whenever the app enters the foreground. KEEP ensures
     * this lifecycle safety scan never cancels a user-requested full rescan.
     */
    fun scanSmsOnAppForeground() {
        val application = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(application, Manifest.permission.READ_SMS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val now = System.currentTimeMillis()
        if (now - lastForegroundScanRequestAt < FOREGROUND_SCAN_DEBOUNCE_MS) return
        lastForegroundScanRequestAt = now

        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: return@launch
            if (!current.onboardingCompleted || !current.smsScanEnabled) return@launch
            SmsReaderWorker.enqueue(
                context = application,
                forceFull = false,
                existingWorkPolicy = ExistingWorkPolicy.KEEP
            )
        }
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

    fun defaultModelFor(provider: AiProvider): String = provider.defaultModel

    fun defaultBaseUrlFor(provider: AiProvider): String = provider.defaultBaseUrl

    // --- EXPORT ---
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    /**
     * Writes all non-deleted transactions to [outStream] as CSV.
     * Call from a CreateDocument result; [onComplete] runs on the main thread.
     */
    fun exportTransactionsToCsv(outStream: java.io.OutputStream, onComplete: (Boolean) -> Unit) {
        _isExporting.value = true
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val txList = repository.allTransactions.first()
                    java.io.BufferedWriter(java.io.OutputStreamWriter(outStream)).use { writer ->
                        writer.write(
                            "Transaction ID,Date,Type,Amount,Currency,Note,Category,Account," +
                                "Merchant,Bank,Source,Reference,Tags\n"
                        )
                        txList.forEach { tx ->
                            writer.write(
                                listOf(
                                    tx.id.toString(),
                                    tx.transactionDate,
                                    tx.type,
                                    tx.amount.toString(),
                                    tx.currency,
                                    csvEscape(tx.note),
                                    csvEscape(tx.categoryName),
                                    csvEscape(tx.accountName),
                                    csvEscape(tx.merchantName.orEmpty()),
                                    csvEscape(tx.bankName.orEmpty()),
                                    tx.source,
                                    csvEscape(tx.reference.orEmpty()),
                                    csvEscape(tx.tags)
                                ).joinToString(",") + "\n"
                            )
                        }
                    }
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                } finally {
                    try {
                        outStream.close()
                    } catch (_: Exception) {
                    }
                }
            }
            _isExporting.value = false
            onComplete(success)
        }
    }

    /**
     * Writes a multi-page PDF statement of all transactions to [outStream].
     * [onComplete] runs on the main thread.
     */
    fun exportTransactionsToPdf(outStream: java.io.OutputStream, onComplete: (Boolean) -> Unit) {
        _isExporting.value = true
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val currency = repository.appSettings.first()?.currency ?: "₹"
                    val txList = repository.allTransactions.first().sortedBy { it.transactionDate }
                    val pdfDocument = android.graphics.pdf.PdfDocument()

                    val pageWidth = 595 // A4
                    val pageHeight = 842
                    val margin = 40f
                    val rowHeight = 24f
                    val headerHeight = 35f
                    val yStart = 140f
                    val rowsPerPage =
                        ((pageHeight - yStart - margin) / rowHeight).toInt().coerceAtLeast(1)

                    val brandColor = android.graphics.Color.parseColor("#0B6E4F")
                    val chunks =
                        if (txList.isEmpty()) listOf(emptyList()) else txList.chunked(rowsPerPage)

                    chunks.forEachIndexed { pageIndex, pageTxList ->
                        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo
                            .Builder(pageWidth, pageHeight, pageIndex + 1)
                            .create()
                        val page = pdfDocument.startPage(pageInfo)
                        val canvas = page.canvas

                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                        }
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 10f
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.create(
                                "sans-serif",
                                android.graphics.Typeface.NORMAL
                            )
                        }
                        val boldTextPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 10f
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.create(
                                "sans-serif",
                                android.graphics.Typeface.BOLD
                            )
                        }
                        val titlePaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#121212")
                            textSize = 24f
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.create(
                                "sans-serif",
                                android.graphics.Typeface.BOLD
                            )
                        }
                        val subtitlePaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 10f
                            isAntiAlias = true
                        }

                        if (pageIndex == 0) {
                            canvas.drawText("PAISA", margin, 54f, titlePaint)
                            val generatedAt = SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                Locale.getDefault()
                            ).format(Date())
                            canvas.drawText(
                                "Generated on: $generatedAt",
                                margin,
                                78f,
                                subtitlePaint
                            )
                            canvas.drawText(
                                "Total Transactions: ${txList.size}",
                                margin,
                                92f,
                                subtitlePaint
                            )
                            paint.color = brandColor
                            canvas.drawRect(margin, 116f, pageWidth - margin, 120f, paint)
                        } else {
                            canvas.drawText(
                                "PAISA - Transactions Statement (Page ${pageIndex + 1})",
                                margin,
                                44f,
                                boldTextPaint
                            )
                            paint.color = brandColor
                            canvas.drawRect(margin, 52f, pageWidth - margin, 54f, paint)
                        }

                        val headerY = if (pageIndex == 0) yStart else 70f
                        paint.color = android.graphics.Color.parseColor("#EEEEEE")
                        canvas.drawRect(
                            margin,
                            headerY,
                            pageWidth - margin,
                            headerY + headerHeight,
                            paint
                        )

                        val colDateX = margin + 5f
                        val colTimeX = margin + 75f
                        val colAccountX = margin + 135f
                        val colCategoryX = margin + 235f
                        val colTypeX = margin + 345f
                        val colAmountX = margin + 425f

                        val headerTextY = headerY + 22f
                        canvas.drawText("Date", colDateX, headerTextY, boldTextPaint)
                        canvas.drawText("Time", colTimeX, headerTextY, boldTextPaint)
                        canvas.drawText("Account", colAccountX, headerTextY, boldTextPaint)
                        canvas.drawText("Category", colCategoryX, headerTextY, boldTextPaint)
                        canvas.drawText("Type", colTypeX, headerTextY, boldTextPaint)
                        canvas.drawText(
                            "Amount ($currency)",
                            colAmountX,
                            headerTextY,
                            boldTextPaint
                        )

                        var currentY = headerY + headerHeight
                        pageTxList.forEachIndexed { index, tx ->
                            if (index % 2 == 0) {
                                paint.color = android.graphics.Color.parseColor("#F9F9F9")
                                canvas.drawRect(
                                    margin,
                                    currentY,
                                    pageWidth - margin,
                                    currentY + rowHeight,
                                    paint
                                )
                            }

                            val rowTextY = currentY + 16f
                            val parts = tx.transactionDate.split("T")
                            val datePart = parts.getOrNull(0).orEmpty()
                            val timePart = parts.getOrNull(1)?.take(5).orEmpty()

                            canvas.drawText(datePart, colDateX, rowTextY, textPaint)
                            canvas.drawText(timePart, colTimeX, rowTextY, textPaint)
                            canvas.drawText(
                                truncateForPdf(tx.accountName, 12),
                                colAccountX,
                                rowTextY,
                                textPaint
                            )
                            canvas.drawText(
                                truncateForPdf(tx.categoryName, 15),
                                colCategoryX,
                                rowTextY,
                                textPaint
                            )

                            val typePaint = android.graphics.Paint(textPaint).apply {
                                color = if (tx.type.equals("income", ignoreCase = true)) {
                                    android.graphics.Color.parseColor("#00796B")
                                } else {
                                    android.graphics.Color.parseColor("#D32F2F")
                                }
                                typeface = android.graphics.Typeface.create(
                                    "sans-serif",
                                    android.graphics.Typeface.BOLD
                                )
                            }
                            canvas.drawText(
                                tx.type.uppercase(Locale.getDefault()),
                                colTypeX,
                                rowTextY,
                                typePaint
                            )
                            canvas.drawText(
                                String.format(
                                    Locale.getDefault(),
                                    "%s%.2f",
                                    currency,
                                    tx.amount
                                ),
                                colAmountX,
                                rowTextY,
                                textPaint
                            )

                            currentY += rowHeight
                        }

                        val footerY = pageHeight - 20f
                        canvas.drawText(
                            "Page ${pageIndex + 1} of ${chunks.size}",
                            pageWidth - margin - 60f,
                            footerY,
                            subtitlePaint
                        )
                        pdfDocument.finishPage(page)
                    }

                    pdfDocument.writeTo(outStream)
                    pdfDocument.close()
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                } finally {
                    try {
                        outStream.close()
                    } catch (_: Exception) {
                    }
                }
            }
            _isExporting.value = false
            onComplete(success)
        }
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun truncateForPdf(value: String, maxLen: Int): String =
        if (value.length > maxLen) value.take(maxLen - 2) + ".." else value

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

    private companion object {
        /** How long a picker/export detour may last before we lock on return anyway. */
        const val EXTERNAL_ACTIVITY_GRACE_MS = 2 * 60 * 1000L
        const val FOREGROUND_SCAN_DEBOUNCE_MS = 30_000L
    }
}
