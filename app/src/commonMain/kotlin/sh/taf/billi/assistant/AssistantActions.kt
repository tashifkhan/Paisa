package sh.taf.billi.assistant

import kotlinx.coroutines.flow.first
import sh.taf.billi.data.Account
import sh.taf.billi.data.Category
import sh.taf.billi.data.Transaction
import sh.taf.billi.data.TransactionWithDetails
import sh.taf.billi.data.repository.AccountRepository
import sh.taf.billi.data.repository.BudgetRepository
import sh.taf.billi.data.repository.CategoryRepository
import sh.taf.billi.data.repository.SettingsRepository
import sh.taf.billi.data.repository.TransactionRepository
import sh.taf.billi.platform.formatDate
import sh.taf.billi.platform.nowMillis
import sh.taf.billi.platform.parseDate

/** A request the assistant got wrong, with a message it can repeat to the user. */
class AssistantInputException(message: String) : IllegalArgumentException(message)

data class AssistantAddedTransaction(
    val id: Int,
    val amount: Double,
    val type: String,
    val title: String,
    val category: String,
    val account: String,
    val date: String,
    val currency: String
)

data class AssistantAccountBalance(val name: String, val type: String, val balance: Double)

data class AssistantCategoryTotal(val category: String, val amount: Double)

data class AssistantSpending(
    val currency: String,
    val startDate: String,
    val endDate: String,
    /** Cash and bank accounts, without credit cards, as on Home. */
    val totalBalance: Double,
    /** What's owed on credit cards, as a positive number. */
    val creditCardDebt: Double,
    val accounts: List<AssistantAccountBalance>,
    val income: Double,
    val expense: Double,
    /** Expense by category in the period, largest first. */
    val expenseByCategory: List<AssistantCategoryTotal>,
    val transactionCount: Int
)

data class AssistantBudget(
    val name: String,
    val limit: Double,
    val spent: Double,
    val remaining: Double,
    val overBudget: Boolean
)

data class AssistantBudgets(
    val currency: String,
    val month: Int,
    val year: Int,
    val budgets: List<AssistantBudget>
)

data class AssistantTransaction(
    val id: Int,
    val date: String,
    val type: String,
    val amount: Double,
    val title: String,
    val category: String,
    val account: String,
    val source: String
)

/**
 * What an assistant (Gemini through Android AppFunctions) can do in Billi:
 * add a transaction, and read balances, spending, budgets and recent
 * transactions. Platform glue only translates types; every rule lives here so
 * it's tested once.
 */
class AssistantActions(
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val budgets: BudgetRepository,
    private val settings: SettingsRepository,
    private val clock: () -> Long = ::nowMillis
) {

    suspend fun addTransaction(
        amount: Double,
        type: String?,
        title: String?,
        category: String?,
        account: String?,
        date: String?,
        note: String?
    ): AssistantAddedTransaction {
        if (amount.isNaN() || amount.isInfinite() || amount <= 0.0) {
            throw AssistantInputException("The amount has to be a number above zero.")
        }
        if (amount > MAX_AMOUNT) throw AssistantInputException("That amount is too large to save.")
        val kind = when (type?.trim()?.lowercase()) {
            null, "", "expense", "spent", "debit", "paid" -> "expense"
            "income", "received", "credit", "earned" -> "income"
            else -> throw AssistantInputException("Type must be expense or income.")
        }
        val merchant = title?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_TEXT)
        val chosenAccount = pickAccount(account)
        val chosenCategory = pickCategory(category, merchant, kind)
        val day = date?.trim()?.takeIf { it.isNotEmpty() }?.let(::parseDay)
        val now = clock()
        val transactionDate = if (day == null) {
            formatDate(now, ISO)
        } else {
            // Keep the time of day, so a back-dated entry still sorts sensibly.
            "$day" + formatDate(now, "'T'HH:mm:ss")
        }
        val stamp = formatDate(now, ISO)
        val id = transactions.insertTransaction(
            Transaction(
                amount = amount,
                type = kind,
                categoryId = chosenCategory.id,
                accountId = chosenAccount.id,
                note = note?.trim().orEmpty().take(MAX_NOTE),
                transactionDate = transactionDate,
                createdAt = stamp,
                updatedAt = stamp,
                merchantName = merchant,
                source = SOURCE
            )
        ).toInt()
        return AssistantAddedTransaction(
            id = id,
            amount = amount,
            type = kind,
            title = merchant ?: chosenCategory.name,
            category = chosenCategory.name,
            account = chosenAccount.name,
            date = transactionDate.take(10),
            currency = currencySymbol()
        )
    }

    /**
     * Balances now, and income and expense between [startDate] and [endDate]
     * (yyyy-MM-dd, both inclusive). Without dates it covers this month so far.
     * [category] narrows the spending figures to one category.
     */
    suspend fun spending(startDate: String?, endDate: String?, category: String?): AssistantSpending {
        val (start, end) = range(startDate, endDate)
        val all = transactions.allTransactions.first()
        val categoryFilter = category?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
            matchCategory(name, null)
                ?: throw AssistantInputException("Billi has no category called \"$name\".")
        }
        val inRange = all.filter { tx ->
            !tx.isDeleted && tx.transactionDate.take(10) in start..end &&
                (categoryFilter == null || tx.categoryId == categoryFilter.id)
        }
        val accountList = accounts.getAllAccountsList()
        val (cards, others) = accountList.partition { it.type == CREDIT_CARD }
        val expenses = inRange.filter { it.type == "expense" }
        return AssistantSpending(
            currency = currencySymbol(),
            startDate = start,
            endDate = end,
            totalBalance = others.sumOf { it.currentBalance }.money(),
            creditCardDebt = cards.sumOf { -it.currentBalance }.coerceAtLeast(0.0).money(),
            accounts = accountList.map { AssistantAccountBalance(it.name, it.type, it.currentBalance.money()) },
            income = inRange.filter { it.type == "income" }.sumOf { it.amount }.money(),
            expense = expenses.sumOf { it.amount }.money(),
            expenseByCategory = expenses.groupBy { it.categoryName }
                .map { (name, rows) -> AssistantCategoryTotal(name, rows.sumOf { it.amount }.money()) }
                .sortedByDescending { it.amount },
            transactionCount = inRange.size
        )
    }

    /** Budget against spending for [month] (1 to 12) of [year]. Defaults to this month. */
    suspend fun budgets(month: Int?, year: Int?): AssistantBudgets {
        val now = clock()
        val m = month?.takeIf { it != 0 } ?: formatDate(now, "M").toInt()
        val y = year?.takeIf { it != 0 } ?: formatDate(now, "yyyy").toInt()
        if (m !in 1..12) throw AssistantInputException("Month must be from 1 to 12.")
        if (y !in 1970..9999) throw AssistantInputException("That year doesn't look right.")
        val prefix = "${y.toString().padStart(4, '0')}-${m.toString().padStart(2, '0')}"
        val monthExpenses = transactions.allTransactions.first()
            .filter { !it.isDeleted && it.type == "expense" && it.transactionDate.startsWith(prefix) }
        val categoryNames = categories.allCategories.first().associate { it.id to it.name }
        val rows = budgets.getBudgetsForPeriod(m, y).first().map { budget ->
            // Same rule as the Budgets tab: no category means all spending.
            val spent = monthExpenses
                .filter { budget.categoryId == null || it.categoryId == budget.categoryId }
                .sumOf { it.amount }
            AssistantBudget(
                name = budget.budgetName ?: budget.categoryId?.let(categoryNames::get) ?: "Overall spending",
                limit = budget.budgetAmount.money(),
                spent = spent.money(),
                remaining = (budget.budgetAmount - spent).money(),
                overBudget = spent > budget.budgetAmount
            )
        }
        return AssistantBudgets(currencySymbol(), m, y, rows)
    }

    /**
     * The newest transactions, at most [limit] (1 to 50, default 10).
     * [query] matches the title, note, category or account. [type] is
     * expense or income.
     */
    suspend fun recentTransactions(limit: Int?, query: String?, type: String?): List<AssistantTransaction> {
        val count = (limit?.takeIf { it > 0 } ?: DEFAULT_RECENT).coerceAtMost(MAX_RECENT)
        val kind = when (type?.trim()?.lowercase()) {
            null, "", "all", "any" -> null
            "expense", "expenses" -> "expense"
            "income" -> "income"
            else -> throw AssistantInputException("Type must be expense, income or left out.")
        }
        val needle = query?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        return transactions.allTransactions.first()
            .asSequence()
            .filter { !it.isDeleted && (kind == null || it.type == kind) }
            .filter { needle == null || it.matches(needle) }
            .sortedByDescending { it.transactionDate }
            .take(count)
            .map { tx ->
                AssistantTransaction(
                    id = tx.id,
                    date = tx.transactionDate.take(10),
                    type = tx.type,
                    amount = tx.amount,
                    title = tx.merchantName?.takeIf { it.isNotBlank() } ?: tx.note.takeIf { it.isNotBlank() } ?: tx.categoryName,
                    category = tx.categoryName,
                    account = tx.accountName,
                    source = tx.source
                )
            }
            .toList()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    suspend fun currencySymbol(): String = settings.getSettings()?.currency ?: "₹"

    private suspend fun pickAccount(name: String?): Account {
        val list = accounts.getAllAccountsList()
        if (list.isEmpty()) throw AssistantInputException("Billi has no accounts yet. Add one in the app first.")
        val wanted = name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
            ?: return list.firstOrNull { it.isDefault } ?: list.first()
        return list.firstOrNull { it.name.lowercase() == wanted }
            ?: list.firstOrNull { it.bankName?.lowercase() == wanted }
            ?: list.firstOrNull { it.name.lowercase().contains(wanted) || wanted.contains(it.name.lowercase()) }
            ?: list.firstOrNull { it.bankName?.lowercase()?.let { bank -> bank.contains(wanted) || wanted.contains(bank) } == true }
            ?: list.firstOrNull { it.accountLast4 != null && wanted.endsWith(it.accountLast4) }
            ?: throw AssistantInputException(
                "Billi has no account called \"${name.trim()}\". Accounts: ${list.joinToString { it.name }}."
            )
    }

    /**
     * The named category if Billi has it; otherwise what the merchant rules
     * and keyword map say, as an SMS would get. Never invents a category from
     * whatever the assistant typed.
     */
    private suspend fun pickCategory(name: String?, merchant: String?, type: String): Category {
        name?.trim()?.takeIf { it.isNotEmpty() }?.let { wanted ->
            matchCategory(wanted, type)?.let { return it }
        }
        val guess = categories.resolveCategoryName(merchant ?: name ?: "", type)
        val id = categories.findOrCreateCategory(guess, type)
        return categories.getCategoryById(id)
            ?: throw AssistantInputException("Couldn't pick a category. Try naming one.")
    }

    private suspend fun matchCategory(name: String, type: String?): Category? {
        val wanted = name.lowercase()
        val list = categories.allCategories.first().filter { type == null || it.type == type }
        return list.firstOrNull { it.name.lowercase() == wanted }
            ?: list.firstOrNull { it.name.lowercase().contains(wanted) || wanted.contains(it.name.lowercase()) }
    }

    private fun range(startDate: String?, endDate: String?): Pair<String, String> {
        val today = formatDate(clock(), DAY)
        val end = endDate?.trim()?.takeIf { it.isNotEmpty() }?.let(::parseDay) ?: today
        val start = startDate?.trim()?.takeIf { it.isNotEmpty() }?.let(::parseDay)
            ?: (formatDate(clock(), "yyyy-MM") + "-01").let { if (it > end) end.take(8) + "01" else it }
        if (start > end) throw AssistantInputException("The start date is after the end date.")
        return start to end
    }

    /** Checks a yyyy-MM-dd date and returns it in that form. */
    private fun parseDay(text: String): String {
        val day = text.take(10)
        val millis = parseDate(day, DAY, machine = true)
            ?: throw AssistantInputException("Dates must look like 2026-11-22.")
        return formatDate(millis, DAY, machine = true)
    }

    private fun TransactionWithDetails.matches(needle: String): Boolean =
        merchantName?.lowercase()?.contains(needle) == true ||
            note.lowercase().contains(needle) ||
            categoryName.lowercase().contains(needle) ||
            accountName.lowercase().contains(needle)

    private fun Double.money(): Double = kotlin.math.round(this * 100.0) / 100.0

    companion object {
        /** Shown on transactions an assistant added, like "sms" or "statement". */
        const val SOURCE = "assistant"
        private const val ISO = "yyyy-MM-dd'T'HH:mm:ss"
        private const val DAY = "yyyy-MM-dd"
        private const val CREDIT_CARD = "Credit Card"
        private const val DEFAULT_RECENT = 10
        private const val MAX_RECENT = 50
        private const val MAX_TEXT = 120
        private const val MAX_NOTE = 2_000
        private const val MAX_AMOUNT = 1_000_000_000.0
    }
}
