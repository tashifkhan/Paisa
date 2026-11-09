package codes.tashif.paisa.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.ai.QuickAddContext
import codes.tashif.paisa.ai.QuickAddFailure
import codes.tashif.paisa.ai.QuickAddService
import codes.tashif.paisa.ai.SourcedDraft
import codes.tashif.paisa.ai.StatementSource
import codes.tashif.paisa.ai.TransactionDraft
import codes.tashif.paisa.core.AppLockGuard
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.data.Category
import codes.tashif.paisa.data.Transaction
import codes.tashif.paisa.data.repository.AccountRepository
import codes.tashif.paisa.data.repository.CategoryRepository
import codes.tashif.paisa.data.repository.SettingsRepository
import codes.tashif.paisa.data.repository.TransactionRepository
import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.platform.nowMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A file attached to the AI note, with the name to show on its chip. */
data class AttachedFile(val id: Int, val source: StatementSource, val name: String)

/** One drafted transaction on the review page, as the user is editing it. */
data class AiDraftRow(
    val key: Int,
    val selected: Boolean,
    val type: String,
    val amountText: String,
    val title: String,
    val note: String,
    /** yyyy-MM-dd */
    val date: String,
    /** HH:mm */
    val time: String,
    val categoryId: Int?,
    val accountId: Int?,
    /** "Your note" or the file it came from. */
    val origin: String,
    /** Set when something already in the ledger looks like the same payment. */
    val duplicateNote: String? = null,
) {
    val amount: Double? get() = amountText.toDoubleOrNull()?.takeIf { it > 0.0 && it.isFinite() }
}

enum class AiBusy { DRAFTING, REFINING, SAVING }

data class AiAddUiState(
    val note: String = "",
    val files: List<AttachedFile> = emptyList(),
    val busy: AiBusy? = null,
    val reviewing: Boolean = false,
    val rows: List<AiDraftRow> = emptyList(),
    /** Files, or the note, that gave nothing back. */
    val failures: List<QuickAddFailure> = emptyList(),
    val error: String? = null,
) {
    val canDraft: Boolean get() = busy == null && (note.isNotBlank() || files.isNotEmpty())
    val selectedCount: Int get() = rows.count { it.selected }

    /** True when there's work the sheet should come back to. */
    val inProgress: Boolean get() = busy != null || reviewing
}

/**
 * The "With AI" side of the add sheet: a note and files go to the model,
 * the drafts come back for review, and the picked ones are saved. Lives
 * past the sheet, so closing it mid-draft doesn't lose the work.
 */
class AiAddViewModel(
    private val quickAdd: QuickAddService,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository,
    private val snackbarManager: SnackbarManager,
    private val appLockGuard: AppLockGuard,
) : ViewModel() {

    private val _state = MutableStateFlow(AiAddUiState())
    val state: StateFlow<AiAddUiState> = _state.asStateFlow()

    private var nextKey = 0
    private var job: Job? = null

    fun canRun(): Boolean = quickAdd.canRun()

    /** Call right before opening the file picker so returning doesn't lock the app. */
    fun onExternalActivityLaunched() = appLockGuard.onExternalActivityLaunched()

    fun setNote(note: String) = _state.update { it.copy(note = note, error = null) }

    fun addFiles(sources: List<StatementSource>) {
        viewModelScope.launch {
            val added = sources.map { source ->
                val name = runCatching { source.displayName() }.getOrNull() ?: source.name ?: "File"
                AttachedFile(nextKey++, source, name)
            }
            _state.update { it.copy(files = it.files + added, error = null) }
        }
    }

    fun removeFile(id: Int) = _state.update { s -> s.copy(files = s.files.filterNot { it.id == id }) }

    fun draft() {
        val current = _state.value
        if (!current.canDraft) return
        if (!quickAdd.canRun()) {
            _state.update { it.copy(error = NO_MODEL) }
            return
        }
        _state.update { it.copy(busy = AiBusy.DRAFTING, error = null, failures = emptyList()) }
        job = viewModelScope.launch {
            try {
                val context = buildContext()
                val result = quickAdd.draft(current.note, current.files.map { it.source }, context)
                if (result.drafts.isEmpty()) {
                    val message = result.failures.singleOrNull()?.message?.takeIf { current.files.size <= 1 }
                        ?: result.failures.takeIf { it.isNotEmpty() }?.joinToString("\n") { "${it.origin}: ${it.message}" }
                        ?: "The AI didn't find any transactions in that. Add an amount, or try a clearer photo."
                    _state.update { it.copy(busy = null, error = message) }
                    return@launch
                }
                val rows = toRows(result.drafts)
                _state.update {
                    it.copy(busy = null, reviewing = true, rows = rows, failures = result.failures)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = null, error = e.message ?: "Couldn't reach the AI") }
            }
        }
    }

    /** Asks the model to change the drafts, like "the uber one was on my HDFC card". */
    fun refine(instruction: String) {
        val current = _state.value
        if (instruction.isBlank() || current.busy != null || current.rows.isEmpty()) return
        _state.update { it.copy(busy = AiBusy.REFINING, error = null) }
        job = viewModelScope.launch {
            try {
                val accounts = accountRepository.allAccounts.first()
                val categories = categoryRepository.allCategories.first()
                val drafts = current.rows.map { it.toDraft(accounts, categories) }
                val refined = quickAdd.refine(instruction, drafts, buildContext())
                if (refined.isEmpty()) {
                    _state.update {
                        it.copy(busy = null, error = "The AI sent back nothing. Your drafts are as they were.")
                    }
                    return@launch
                }
                val origin = current.rows.map { it.origin }.distinct().singleOrNull() ?: "Refined"
                val rows = toRows(refined.map { SourcedDraft(it, origin) })
                _state.update { it.copy(busy = null, rows = rows) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = null, error = e.message ?: "Couldn't reach the AI") }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.update { it.copy(busy = null) }
    }

    fun updateRow(row: AiDraftRow) = _state.update { s ->
        s.copy(rows = s.rows.map { if (it.key == row.key) row else it })
    }

    fun removeRow(key: Int) = _state.update { s ->
        val rows = s.rows.filterNot { it.key == key }
        s.copy(rows = rows, reviewing = rows.isNotEmpty())
    }

    fun setAllSelected(selected: Boolean) = _state.update { s ->
        s.copy(rows = s.rows.map { it.copy(selected = selected) })
    }

    /** Back from review to the note, keeping the note and files. */
    fun backToNote() = _state.update { it.copy(reviewing = false, rows = emptyList(), failures = emptyList()) }

    fun clearError() = _state.update { it.copy(error = null) }

    /** Saves the picked rows. [onSaved] runs once they're all in. */
    fun save(onSaved: () -> Unit) {
        val current = _state.value
        if (current.busy != null) return
        val picked = current.rows.filter { it.selected }
        if (picked.isEmpty()) {
            _state.update { it.copy(error = "Pick at least one transaction to save") }
            return
        }
        val bad = picked.indexOfFirst { it.amount == null || it.categoryId == null || it.accountId == null }
        if (bad >= 0) {
            val row = picked[bad]
            val what = when {
                row.amount == null -> "an amount"
                row.categoryId == null -> "a category"
                else -> "an account"
            }
            _state.update { it.copy(error = "\"${row.title.ifBlank { "Untitled" }}\" needs $what") }
            return
        }
        _state.update { it.copy(busy = AiBusy.SAVING, error = null) }
        viewModelScope.launch {
            try {
                val now = formatDate(nowMillis(), "yyyy-MM-dd'T'HH:mm:ss", machine = true)
                for (row in picked) {
                    transactionRepository.insertTransaction(
                        Transaction(
                            amount = row.amount!!,
                            type = row.type,
                            categoryId = row.categoryId!!,
                            accountId = row.accountId!!,
                            note = row.note.trim(),
                            transactionDate = "${row.date}T${row.time}:00",
                            createdAt = now,
                            updatedAt = now,
                            merchantName = row.title.trim().takeIf { it.isNotBlank() },
                            source = "manual"
                        )
                    )
                }
                snackbarManager.show(
                    if (picked.size == 1) "Transaction saved" else "Saved ${picked.size} transactions"
                )
                _state.value = AiAddUiState()
                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = null, error = e.message ?: "Couldn't save") }
            }
        }
    }

    /** Throws away the note, files and drafts. */
    fun reset() {
        job?.cancel()
        _state.value = AiAddUiState()
    }

    private suspend fun buildContext(): QuickAddContext {
        val categories = categoryRepository.allCategories.first()
        val accounts = accountRepository.allAccounts.first()
        val millis = nowMillis()
        return QuickAddContext(
            today = formatDate(millis, "yyyy-MM-dd", machine = true),
            weekday = formatDate(millis, "EEEE", machine = true),
            currency = settingsRepository.settings.first()?.currency ?: "₹",
            expenseCategories = categories.filter { it.type == "expense" }.map { it.name },
            incomeCategories = categories.filter { it.type == "income" }.map { it.name },
            accounts = accounts.map(::describe),
        )
    }

    private suspend fun toRows(drafts: List<SourcedDraft>): List<AiDraftRow> {
        val categories = categoryRepository.allCategories.first()
        val accounts = accountRepository.allAccounts.first()
        val fallbackAccount = (accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull())?.id
        val defaultTime = formatDate(nowMillis(), "HH:mm", machine = true)
        return drafts.map { (draft, origin) ->
            val existing = transactionRepository.findSimilarTransaction(
                amount = draft.amount,
                type = draft.type,
                dateIso = draft.date,
                windowDays = 1
            )
            val duplicateNote = existing?.let {
                val title = it.merchantName ?: it.note.ifBlank { "a transaction" }
                "Looks like $title on ${it.transactionDate.take(10)}"
            }
            AiDraftRow(
                key = nextKey++,
                selected = existing == null,
                type = draft.type,
                amountText = formatAmount(draft.amount),
                title = draft.title,
                note = draft.note,
                date = draft.date,
                time = draft.time ?: defaultTime,
                categoryId = matchCategory(draft, categories),
                accountId = matchAccount(draft.accountName, accounts) ?: fallbackAccount,
                origin = origin,
                duplicateNote = duplicateNote,
            )
        }
    }

    private suspend fun matchCategory(draft: TransactionDraft, categories: List<Category>): Int? {
        val ofType = categories.filter { it.type == draft.type }
        fun byName(name: String?): Category? =
            name?.let { n -> ofType.firstOrNull { it.name.equals(n.trim(), ignoreCase = true) } }
        val named = byName(draft.categoryName)
            ?: byName(draft.categoryName?.let { categoryRepository.canonicalizeCategoryName(it, draft.type) })
            ?: byName(categoryRepository.resolveCategoryName(draft.title, draft.type))
        return (named ?: ofType.firstOrNull())?.id
    }

    private fun matchAccount(name: String?, accounts: List<Account>): Int? {
        val wanted = name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        return (
            accounts.firstOrNull { it.name.lowercase() == wanted }
                ?: accounts.firstOrNull { describe(it).lowercase() == wanted }
                ?: accounts.firstOrNull { wanted.contains(it.name.lowercase()) || it.name.lowercase().contains(wanted) }
            )?.id
    }

    private fun AiDraftRow.toDraft(accounts: List<Account>, categories: List<Category>) = TransactionDraft(
        amount = amount ?: 0.0,
        type = type,
        title = title,
        note = note,
        date = date,
        time = time,
        categoryName = categories.firstOrNull { it.id == categoryId }?.name,
        accountName = accounts.firstOrNull { it.id == accountId }?.name,
    )

    companion object {
        const val NO_MODEL = "Add a model and save its key in AI settings first."

        private fun describe(account: Account): String {
            val details = listOfNotNull(
                account.type,
                account.bankName,
                account.accountLast4?.let { "ending $it" }
            ).joinToString(", ")
            return "${account.name} ($details)"
        }

        fun formatAmount(amount: Double): String =
            if (amount == kotlin.math.floor(amount)) amount.toLong().toString() else {
                val cents = kotlin.math.round(amount * 100).toLong()
                "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
            }
    }
}
