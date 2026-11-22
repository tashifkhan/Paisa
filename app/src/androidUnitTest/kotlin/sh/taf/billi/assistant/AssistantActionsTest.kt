package sh.taf.billi.assistant

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import sh.taf.billi.data.AppDatabase
import sh.taf.billi.data.Budget
import sh.taf.billi.data.repository.AccountRepository
import sh.taf.billi.data.repository.BudgetRepository
import sh.taf.billi.data.repository.CategoryRepository
import sh.taf.billi.data.repository.DebtRepository
import sh.taf.billi.data.repository.SettingsRepository
import sh.taf.billi.data.repository.TransactionRepository
import sh.taf.billi.platform.parseDate
import sh.taf.billi.testing.inMemoryDatabase

/** The actions Gemini calls, on the real seed: Bank Account (listed first, so the default) and Cash, default categories, ₹. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AssistantActionsTest {

    private lateinit var db: AppDatabase
    private lateinit var actions: AssistantActions
    private lateinit var budgets: BudgetRepository
    private lateinit var categories: CategoryRepository

    /** Sunday 22 November 2026, noon. */
    private val now = parseDate("2026-11-22 12:00", "yyyy-MM-dd HH:mm", machine = true)!!

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val accounts = AccountRepository(db)
        categories = CategoryRepository(db)
        budgets = BudgetRepository(db)
        actions = AssistantActions(
            transactions = TransactionRepository(db, accounts, DebtRepository(db)),
            accounts = accounts,
            categories = categories,
            budgets = budgets,
            settings = SettingsRepository(db),
            clock = { now }
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun addsAnExpenseToTheDefaultAccountAndMovesTheBalance(): Unit = runBlocking {
        val saved = actions.addTransaction(240.0, null, "Chaayos", "Food & Dining", null, null, null)

        assertEquals("expense", saved.type)
        assertEquals("Food & Dining", saved.category)
        assertEquals("Bank Account", saved.account)
        assertEquals("2026-11-22", saved.date)
        assertEquals("₹", saved.currency)
        val summary = actions.spending(null, null, null)
        assertEquals(-240.0, summary.accounts.first { it.name == "Bank Account" }.balance, 0.001)
        assertEquals(240.0, summary.expense, 0.001)
        assertEquals("assistant", actions.recentTransactions(1, null, null).single().source)
    }

    @Test
    fun picksTheCategoryFromTheTitleWhenNoneIsNamed(): Unit = runBlocking {
        val saved = actions.addTransaction(180.0, "expense", "Uber", null, "bank", "2026-11-20", "office")
        assertEquals("Transport", saved.category)
        assertEquals("Bank Account", saved.account)
        assertEquals("2026-11-20", saved.date)
    }

    @Test
    fun neverInventsACategoryFromWhatTheAssistantTyped(): Unit = runBlocking {
        val before = categories.allCategoriesList().size
        actions.addTransaction(99.0, null, "Corner shop", "Quantum snacks", null, null, null)
        assertEquals(before, categories.allCategoriesList().size)
    }

    @Test
    fun rejectsBadInputWithAMessage(): Unit = runBlocking {
        fun fails(block: suspend () -> Unit): String =
            try {
                runBlocking { block() }
                error("expected a failure")
            } catch (e: AssistantInputException) {
                e.message!!
            }
        assertTrue(fails { actions.addTransaction(0.0, null, "x", null, null, null, null) }.contains("above zero"))
        assertTrue(fails { actions.addTransaction(10.0, "refund", "x", null, null, null, null) }.contains("expense or income"))
        assertTrue(fails { actions.addTransaction(10.0, null, "x", null, "Amex Platinum", null, null) }.contains("Accounts: Bank Account, Cash"))
        assertTrue(fails { actions.addTransaction(10.0, null, "x", null, null, "22/11/2026", null) }.contains("2026-11-22"))
        assertTrue(fails { actions.spending("2026-11-30", "2026-11-01", null) }.contains("after the end"))
        assertTrue(fails { actions.budgets(13, null) }.contains("1 to 12"))
    }

    @Test
    fun spendingCoversThisMonthByDefaultAndSplitsByCategory(): Unit = runBlocking {
        actions.addTransaction(500.0, null, "Swiggy", "Food & Dining", null, "2026-11-03", null)
        actions.addTransaction(200.0, null, "Uber", "Transport", null, "2026-11-04", null)
        actions.addTransaction(900.0, null, "Swiggy", "Food & Dining", null, "2026-10-28", null)
        actions.addTransaction(50_000.0, "income", "Salary", null, "Bank Account", "2026-11-01", null)

        val month = actions.spending(null, null, null)
        assertEquals("2026-11-01", month.startDate)
        assertEquals("2026-11-22", month.endDate)
        assertEquals(700.0, month.expense, 0.001)
        assertEquals(50_000.0, month.income, 0.001)
        assertEquals("Food & Dining", month.expenseByCategory.first().category)

        val food = actions.spending("2026-10-01", "2026-11-30", "food")
        assertEquals(1400.0, food.expense, 0.001)
        assertEquals(2, food.transactionCount)
    }

    @Test
    fun budgetsReportSpentAndOverBudget(): Unit = runBlocking {
        val foodId = categories.allCategoriesList().first { it.name == "Food & Dining" && it.type == "expense" }.id
        budgets.insertBudget(Budget(categoryId = foodId, budgetAmount = 1000.0, month = 11, year = 2026))
        budgets.insertBudget(Budget(categoryId = null, budgetAmount = 5000.0, month = 11, year = 2026))
        actions.addTransaction(1200.0, null, "Swiggy", "Food & Dining", null, "2026-11-10", null)
        actions.addTransaction(300.0, null, "Uber", "Transport", null, "2026-11-11", null)

        val report = actions.budgets(null, null)
        assertEquals(11, report.month)
        val food = report.budgets.first { it.name == "Food & Dining" }
        assertEquals(1200.0, food.spent, 0.001)
        assertTrue(food.overBudget)
        assertEquals(-200.0, food.remaining, 0.001)
        val overall = report.budgets.first { it.name == "Overall spending" }
        assertEquals(1500.0, overall.spent, 0.001)
        assertFalse(overall.overBudget)
    }

    @Test
    fun recentTransactionsAreNewestFirstAndFilterable(): Unit = runBlocking {
        actions.addTransaction(100.0, null, "Swiggy", null, null, "2026-11-01", null)
        actions.addTransaction(200.0, null, "Zomato", null, null, "2026-11-05", null)
        actions.addTransaction(300.0, "income", "Refund from Swiggy", null, null, "2026-11-06", null)

        val all = actions.recentTransactions(null, null, null)
        assertEquals(listOf("Refund from Swiggy", "Zomato", "Swiggy"), all.map { it.title })
        assertEquals(listOf("Swiggy"), actions.recentTransactions(5, "swiggy", "expense").map { it.title })
        assertEquals(1, actions.recentTransactions(1, null, null).size)
    }

    private suspend fun CategoryRepository.allCategoriesList() = allCategories.first()
}
