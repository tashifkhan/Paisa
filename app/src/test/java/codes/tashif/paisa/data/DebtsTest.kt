package codes.tashif.paisa.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import codes.tashif.paisa.data.repository.AccountRepository
import codes.tashif.paisa.data.repository.DebtRepository
import codes.tashif.paisa.data.repository.SettingsRepository
import codes.tashif.paisa.data.repository.TransactionRepository

/** Debts: people, entries, settling up, and tying transactions to people. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DebtsTest {

    private val db: AppDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        AppDatabase::class.java
    ).addCallback(AppDatabase.callback).allowMainThreadQueries().build()
        .also { it.openHelper.writableDatabase }
    private val accountRepo = AccountRepository(db)
    private val transactionRepo = TransactionRepository(db, accountRepo)
    private val settingsRepo = SettingsRepository(db)
    private val debtRepo = DebtRepository(db)

    @After
    fun tearDown() = db.close()

    private fun balances() = runBlocking {
        debtRepo.debtPeople.first().associate { it.person.name to it.balance }
    }

    private fun transaction(type: String, amount: Double, merchant: String): Int = runBlocking {
        val cat = db.categoryDao().getCategoryByName(
            if (type == "expense") "Food & Dining" else "Other", type
        )!!.id
        transactionRepo.insertTransaction(
            Transaction(
                amount = amount, type = type, categoryId = cat, accountId = 2, note = "",
                transactionDate = "2026-11-08T20:00:00", createdAt = "c", updatedAt = "u",
                merchantName = merchant
            )
        ).toInt()
    }

    private fun bankBalance() = runBlocking { accountRepo.getAccountById(2)!!.currentBalance }

    @Test
    fun balanceIsTheSumOfEntries() = runBlocking {
        val radhika = debtRepo.addDebtPerson("Radhika", 5000.0)
        debtRepo.addDebtEntry(radhika, -1500.0, "Paid back part")
        val omar = debtRepo.addDebtPerson("Omar", -2000.0)

        assertEquals(mapOf("Radhika" to 3500.0, "Omar" to -2000.0), balances())
        // Adding the same name again reuses the person.
        assertEquals(radhika, debtRepo.addDebtPerson("radhika"))
        assertEquals(2, debtRepo.debtPeople.first().size)
        assertTrue(omar != radhika)
    }

    @Test
    fun settleBringsTheBalanceToZeroAndKeepsHistory() = runBlocking {
        val id = debtRepo.addDebtPerson("Sarthak", 4000.0)
        debtRepo.settleDebt(id)
        assertEquals(0.0, balances().getValue("Sarthak"), 0.001)
        assertEquals(2, debtRepo.debtEntries(id).first().size)
    }

    @Test
    fun linkingTransactionsLeavesAccountsAlone() = runBlocking {
        // Dinner for two: Radhika owes half.
        val dinner = transaction("expense", 1200.0, "Dinner")
        val before = bankBalance()
        val radhika = debtRepo.linkTransactionToDebt(dinner, null, "Radhika", 600.0)
        assertEquals(600.0, balances().getValue("Radhika"), 0.001)

        // She pays back over UPI; that income is tied to her too.
        val back = transaction("income", 600.0, "Radhika")
        debtRepo.linkTransactionToDebt(back, radhika, null, 600.0)
        assertEquals(0.0, balances().getValue("Radhika"), 0.001)

        // Debts never touched the bank balance beyond the transactions themselves.
        assertEquals(before + 600.0, bankBalance(), 0.001)
        val entries = debtRepo.debtEntries(radhika).first()
        assertEquals(setOf("Dinner", "Radhika"), entries.map { it.note }.toSet())
        assertTrue(entries.all { it.transactionSyncId != null })
    }

    @Test
    fun deletingAPersonRemovesTheirHistory() = runBlocking {
        val id = debtRepo.addDebtPerson("Temp", 100.0)
        debtRepo.deleteDebtPerson(db.debtDao().getPerson(id)!!)
        assertTrue(debtRepo.debtPeople.first().isEmpty())
        assertTrue(debtRepo.debtEntries(id).first().isEmpty())
    }

    @Test
    fun navTabsKeepHomeAndMoreAndCapAtFive() {
        assertEquals(
            listOf(AppTab.HOME, AppTab.ANALYTICS, AppTab.BUDGETS, AppTab.ACCOUNTS, AppTab.MORE),
            NavTabs.parse(Settings.DEFAULT_NAV_TABS)
        )
        // Missing required tabs come back; unknown ids are ignored; order is fixed.
        assertEquals(listOf(AppTab.HOME, AppTab.DEBTS, AppTab.MORE), NavTabs.parse("debts,nonsense"))
        // A full bar won't take a sixth tab.
        val full = NavTabs.parse(Settings.DEFAULT_NAV_TABS)
        assertEquals(full, NavTabs.toggle(full, AppTab.DEBTS, on = true))
        // Swap Budgets for Debts.
        val swapped = NavTabs.toggle(NavTabs.toggle(full, AppTab.BUDGETS, on = false), AppTab.DEBTS, on = true)
        assertEquals("home,analytics,accounts,debts,more", NavTabs.serialize(swapped))
        // Home and More can't be turned off.
        assertEquals(full, NavTabs.toggle(full, AppTab.MORE, on = false))
    }
}
