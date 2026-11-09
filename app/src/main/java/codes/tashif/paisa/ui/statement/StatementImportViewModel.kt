package codes.tashif.paisa.ui.statement

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.ai.AiRepository
import codes.tashif.paisa.ui.ai.AiSummary
import codes.tashif.paisa.ai.ExtractedTransaction
import codes.tashif.paisa.ai.StatementExtractionService
import codes.tashif.paisa.ai.StatementImportUseCase
import codes.tashif.paisa.ai.StatementNotifications
import codes.tashif.paisa.ai.ladder.AiLadderRunner
import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.core.AppLockGuard
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.data.Category
import codes.tashif.paisa.data.Settings
import codes.tashif.paisa.data.repository.AccountRepository
import codes.tashif.paisa.data.repository.CategoryRepository
import codes.tashif.paisa.data.repository.SettingsRepository
import codes.tashif.paisa.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

/**
 * Reads statements with the user's AI provider, shows a preview, and commits
 * the picked rows. Scoped to MainActivity, so extraction keeps going when the
 * user leaves the screen; a notification announces the result.
 */
@HiltViewModel
class StatementImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    accountsRepository: AccountRepository,
    private val categoriesRepository: CategoryRepository,
    private val transactionsRepository: TransactionRepository,
    settingsRepository: SettingsRepository,
    aiRepository: AiRepository,
    private val statementExtraction: StatementExtractionService,
    private val ladderRunner: AiLadderRunner,
    private val statementImport: StatementImportUseCase,
    private val snackbarManager: SnackbarManager,
    private val appLockGuard: AppLockGuard
) : ViewModel() {

    /** The first model that would run, or not ready when no rung has a key. */
    val aiSummary: StateFlow<AiSummary> = combine(
        aiRepository.ladders, aiRepository.providers, aiRepository.catalog, AiSummary::of
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiSummary())

    val accounts: StateFlow<List<Account>> = accountsRepository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = categoriesRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _statementUiState = MutableStateFlow<StatementUiState>(StatementUiState.Idle)
    val statementUiState: StateFlow<StatementUiState> = _statementUiState.asStateFlow()

    /** Call right before opening the file picker so returning doesn't lock the app. */
    fun onExternalActivityLaunched() = appLockGuard.onExternalActivityLaunched()

    fun extractStatements(uris: List<Uri>) {
        if (uris.isEmpty()) return
        // Runs on a background dispatcher in the ViewModel scope: the user can leave
        // the screen (or the app) while chunks process; a notification announces completion.
        viewModelScope.launch(Dispatchers.Default) {
            if (!ladderRunner.canRun(AiService.STATEMENT_IMPORT) &&
                !ladderRunner.canRun(AiService.STATEMENT_PHOTOS)
            ) {
                _statementUiState.value = StatementUiState.Error(
                    "Add a model and save its key in AI settings first."
                )
                return@launch
            }
            _statementUiState.value = StatementUiState.Extracting(totalFiles = uris.size)
            val app = context
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
                val enriched = enrich(merged.values.sortedByDescending { it.date })
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

    /**
     * Fills in categories through the local mapping when the model left them out,
     * and flags rows that look like transactions already in the ledger (e.g.
     * captured earlier from SMS).
     */
    private suspend fun enrich(rows: List<ExtractedTransaction>): List<ExtractedTransaction> =
        rows.map { row ->
            val withCategory = if (row.categoryName.isNullOrBlank()) {
                val name = categoriesRepository.resolveCategoryName(row.merchant, row.type)
                row.copy(categoryName = name)
            } else {
                row.copy(
                    categoryName = categoriesRepository.canonicalizeCategoryName(
                        row.categoryName,
                        row.type
                    )
                )
            }
            val existing = transactionsRepository.findSimilarTransaction(
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

    private fun statementFileName(uri: Uri, fileIndex: Int): String {
        val resolved = runCatching {
            context.contentResolver
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
                snackbarManager.show("Imported ${result.imported} transactions")
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
}
