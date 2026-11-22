package sh.taf.billi.assistant

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionDeniedException
import androidx.appfunctions.AppFunctionException
import androidx.appfunctions.AppFunctionIntValueConstraint
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionStringValueConstraint
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import sh.taf.billi.sync.SyncScheduling
import sh.taf.billi.widget.BilliWidgets

/** A transaction Billi just saved. */
@AppFunctionSerializable(isDescribedByKDoc = true)
class SavedTransaction(
    /** Billi's id for the transaction. */
    val id: Int,
    /** The amount, always positive. */
    val amount: Double,
    /** "expense" or "income". */
    val type: String,
    /** The title shown in Billi, usually the merchant. */
    val title: String,
    /** The category Billi filed it under. */
    val category: String,
    /** The account it was saved to. */
    val account: String,
    /** The day of the transaction. */
    val date: LocalDate,
    /** The user's currency symbol, such as ₹. */
    val currency: String
)

/** One account and what it holds now. */
@AppFunctionSerializable(isDescribedByKDoc = true)
class AccountBalance(
    /** The account's name. */
    val name: String,
    /** Cash, Bank Account, UPI, Credit Card, Debit Card, Wallet or Other. */
    val type: String,
    /** The balance. Negative on a credit card means money owed. */
    val balance: Double
)

/** Money spent in one category. */
@AppFunctionSerializable(isDescribedByKDoc = true)
class CategoryTotal(
    /** The category's name. */
    val category: String,
    /** Total spent in it during the period. */
    val amount: Double
)

/** Balances now, plus income and spending over a period. */
@AppFunctionSerializable(isDescribedByKDoc = true)
class SpendingSummary(
    /** The user's currency symbol, such as ₹. */
    val currency: String,
    /** First day counted. */
    val startDate: LocalDate,
    /** Last day counted. */
    val endDate: LocalDate,
    /** Money across cash and bank accounts, without credit cards. */
    val totalBalance: Double,
    /** What's owed on credit cards, as a positive number. */
    val creditCardDebt: Double,
    /** Every account with its balance. */
    val accounts: List<AccountBalance>,
    /** Income during the period. */
    val income: Double,
    /** Spending during the period. */
    val expense: Double,
    /** Spending by category during the period, largest first. */
    val expenseByCategory: List<CategoryTotal>,
    /** How many transactions fall in the period. */
    val transactionCount: Int
)

/** One budget and how much of it is used. */
@AppFunctionSerializable(isDescribedByKDoc = true)
class BudgetStatus(
    /** The budget's name, or its category. */
    val name: String,
    /** The monthly limit. */
    val limit: Double,
    /** Spent so far in the month. */
    val spent: Double,
    /** Limit minus spent. Negative when over budget. */
    val remaining: Double,
    /** True when spending passed the limit. */
    val overBudget: Boolean
)

/** The budgets for one month. */
@AppFunctionSerializable(isDescribedByKDoc = true)
class BudgetReport(
    /** The user's currency symbol, such as ₹. */
    val currency: String,
    /** The month, 1 to 12. */
    val month: Int,
    /** The year. */
    val year: Int,
    /** Each budget set for that month. Empty when the user has none. */
    val budgets: List<BudgetStatus>
)

/** A transaction from the user's history. */
@AppFunctionSerializable(isDescribedByKDoc = true)
class TransactionSummary(
    /** Billi's id for the transaction. */
    val id: Int,
    /** The day of the transaction. */
    val date: LocalDate,
    /** "expense" or "income". */
    val type: String,
    /** The amount, always positive. */
    val amount: Double,
    /** The merchant or title. */
    val title: String,
    /** The category. */
    val category: String,
    /** The account. */
    val account: String,
    /** Where it came from: manual, sms, notification, statement or assistant. */
    val source: String
)

/** Recent transactions, newest first. */
@AppFunctionSerializable(isDescribedByKDoc = true)
class TransactionList(
    /** The user's currency symbol, such as ₹. */
    val currency: String,
    /** The transactions, newest first. */
    val transactions: List<TransactionSummary>
)

/**
 * Billi's actions for Gemini and other assistants, through Android
 * AppFunctions. The compiler generates [BilliAppFunctionService] from this
 * class, and an XML description of each function for the assistant. The
 * app-level instructions are in res/xml/app_function_app_metadata.xml.
 *
 * Only callers the system lets run app functions (on Android 16 and later)
 * reach these, and only while the user leaves "Gemini and assistants" on
 * under More → Privacy & security.
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "BilliAppFunctionService",
    appFunctionXmlFileName = "billi_app_functions"
)
abstract class BilliAppFunctions : AppFunctionService(), KoinComponent {

    private val actions: AssistantActions by inject()
    private val access: AssistantAccess by inject()

    /**
     * Records an expense or an income in Billi.
     *
     * @param amount How much, as a positive number in the user's currency. Must be above zero.
     * @param type "expense" or "income". If null, records an expense.
     * @param title Who was paid, or who paid, such as "Chaayos" or "Salary". If null, Billi uses the category name.
     * @param category The name of an existing Billi category, such as "Food & Dining". If null or not an existing category, Billi picks one from the title.
     * @param account The name of an existing Billi account, such as "Cash" or "HDFC Savings". A partial name matches. If null, uses the default account.
     * @param date The day the money moved. If null, uses today.
     * @param note Extra detail the user gave, saved as the transaction's note.
     * @return The saved transaction, with the category and account Billi used.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun addTransaction(
        amount: Double,
        @AppFunctionStringValueConstraint(enumValues = ["expense", "income"])
        type: String? = null,
        title: String? = null,
        category: String? = null,
        account: String? = null,
        date: LocalDate? = null,
        note: String? = null
    ): SavedTransaction = guarded {
        val saved = actions.addTransaction(amount, type, title, category, account, date?.toString(), note)
        // Nothing else is running when an assistant calls in, so nudge the
        // widgets and sync the way the app would after a save.
        runCatching { BilliWidgets.refresh(applicationContext) }
        runCatching { SyncScheduling.requestSoon(applicationContext, delaySeconds = 5) }
        SavedTransaction(
            id = saved.id,
            amount = saved.amount,
            type = saved.type,
            title = saved.title,
            category = saved.category,
            account = saved.account,
            date = LocalDate.parse(saved.date),
            currency = saved.currency
        )
    }

    /**
     * Gets every account's current balance, and the income and spending
     * between two days, overall or for one category. Read-only.
     *
     * @param startDate First day to count. If null, uses the first day of this month.
     * @param endDate Last day to count. If null, uses today. Must not be before startDate.
     * @param category Only count spending in this category, such as "Food & Dining". If null, counts every category.
     * @return Balances now, plus income, spending and spending by category for the period.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getSpendingSummary(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        category: String? = null
    ): SpendingSummary = guarded {
        val s = actions.spending(startDate?.toString(), endDate?.toString(), category)
        SpendingSummary(
            currency = s.currency,
            startDate = LocalDate.parse(s.startDate),
            endDate = LocalDate.parse(s.endDate),
            totalBalance = s.totalBalance,
            creditCardDebt = s.creditCardDebt,
            accounts = s.accounts.map { AccountBalance(it.name, it.type, it.balance) },
            income = s.income,
            expense = s.expense,
            expenseByCategory = s.expenseByCategory.map { CategoryTotal(it.category, it.amount) },
            transactionCount = s.transactionCount
        )
    }

    /**
     * Gets the user's budgets for one month, with the limit, the amount spent
     * and what is left of each. Read-only.
     *
     * @param month The month, 1 to 12. If 0 or null, uses this month.
     * @param year The year, such as 2026. If 0 or null, uses this year.
     * @return Each budget for that month. The list is empty when the user set none.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getBudgets(
        @AppFunctionIntValueConstraint(enumValues = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12])
        month: Int? = null,
        year: Int? = null
    ): BudgetReport = guarded {
        val b = actions.budgets(month?.takeIf { it != 0 }, year?.takeIf { it != 0 })
        BudgetReport(
            currency = b.currency,
            month = b.month,
            year = b.year,
            budgets = b.budgets.map { BudgetStatus(it.name, it.limit, it.spent, it.remaining, it.overBudget) }
        )
    }

    /**
     * Lists the user's latest transactions, newest first, optionally only
     * those that contain a word. Read-only.
     *
     * @param limit How many to return, 1 to 50. If 0 or null, returns 10.
     * @param query Only transactions whose title, note, category or account contains this text, ignoring case. If null, returns all.
     * @param type "expense" or "income". If null, returns both.
     * @return The matching transactions, newest first.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getRecentTransactions(
        limit: Int? = null,
        query: String? = null,
        @AppFunctionStringValueConstraint(enumValues = ["expense", "income"])
        type: String? = null
    ): TransactionList = guarded {
        val rows = actions.recentTransactions(limit?.takeIf { it != 0 }, query, type)
        TransactionList(
            currency = actions.currencySymbol(),
            transactions = rows.map {
                TransactionSummary(
                    it.id, LocalDate.parse(it.date), it.type, it.amount, it.title, it.category, it.account, it.source
                )
            }
        )
    }

    /**
     * Checks the user's switch, runs off the main thread, and turns every
     * failure into an AppFunctionException the agent can explain.
     */
    private suspend fun <T> guarded(block: suspend () -> T): T {
        if (!access.enabled.value) {
            throw AppFunctionDeniedException(
                "The user turned off assistant access in Billi (More → Privacy & security)."
            )
        }
        return try {
            withContext(Dispatchers.IO) { block() }
        } catch (e: AssistantInputException) {
            throw AppFunctionInvalidArgumentException(e.message)
        } catch (e: AppFunctionException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw AppFunctionAppUnknownException("Billi couldn't finish that: ${e.message ?: e::class.simpleName}")
        }
    }
}
