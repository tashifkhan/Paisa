package codes.tashif.paisa.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import codes.tashif.paisa.data.repository.AccountRepository
import codes.tashif.paisa.data.repository.DebtRepository
import codes.tashif.paisa.data.repository.DebtShare
import codes.tashif.paisa.data.repository.SettingsRepository
import codes.tashif.paisa.data.repository.TransactionRepository
import codes.tashif.paisa.ui.debts.split.SplitPerson
import codes.tashif.paisa.ui.debts.split.SplitPlan
import codes.tashif.paisa.ui.debts.split.billEntries
import codes.tashif.paisa.ui.debts.split.computeBill

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
    private val debtRepo = DebtRepository(db)
    private val transactionRepo = TransactionRepository(db, accountRepo, debtRepo)
    private val settingsRepo = SettingsRepository(db)

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

    private fun categoryId(type: String) = runBlocking {
        db.categoryDao().getCategoryByName(if (type == "expense") "Food & Dining" else "Other", type)!!.id
    }

    private fun raw(id: Int) = runBlocking { db.transactionDao().getRawTransactionById(id)!! }

    private fun entryAmounts(personId: Int) = runBlocking {
        debtRepo.debtEntries(personId).first().map { it.amount }
    }

    /** Dinner for three: Radhika and Omar owe 400 each. Returns (dinner, radhika, omar). */
    private fun splitDinner(): Triple<Int, Int, Int> = runBlocking {
        val dinner = transaction("expense", 1200.0, "Dinner")
        val (radhika, omar) = debtRepo.saveTransactionSplit(
            dinner,
            listOf(DebtShare(null, "Radhika", 400.0), DebtShare(null, "Omar", 400.0))
        )
        Triple(dinner, radhika, omar)
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
    fun editingALinkedExpenseRescalesItsEntries() = runBlocking {
        val (dinner, radhika, omar) = splitDinner()
        transactionRepo.updateTransaction(
            raw(dinner).copy(amount = 1500.0, merchantName = "Team dinner", transactionDate = "2026-11-09T21:00:00")
        )
        assertEquals(listOf(500.0), entryAmounts(radhika))
        assertEquals(listOf(500.0), entryAmounts(omar))
        val entry = debtRepo.debtEntries(radhika).first().single()
        assertEquals("2026-11-09T21:00:00", entry.entryDate)
        // The note still matched the old title, so it follows the new one.
        assertEquals("Team dinner", entry.note)
    }

    @Test
    fun flippingTheTypeFlipsTheSigns() = runBlocking {
        val (dinner, radhika, omar) = splitDinner()
        transactionRepo.updateTransaction(raw(dinner).copy(type = "income", categoryId = categoryId("income")))
        assertEquals(mapOf("Radhika" to -400.0, "Omar" to -400.0), balances())
        assertEquals(listOf(-400.0), entryAmounts(omar))
        assertTrue(radhika != omar)
    }

    @Test
    fun deletingTheTransactionRemovesItsEntries() = runBlocking {
        val (dinner, radhika, _) = splitDinner()
        val syncId = raw(dinner).syncId!!
        debtRepo.addDebtEntry(radhika, 100.0, "Coffee")
        transactionRepo.deleteTransaction(dinner)
        assertEquals(mapOf("Radhika" to 100.0, "Omar" to 0.0), balances())
        assertTrue(db.debtDao().getEntriesForTransaction(syncId).isEmpty())
    }

    @Test
    fun entriesOfASoftDeletedTransactionNoLongerCount() = runBlocking {
        val (dinner, radhika, _) = splitDinner()
        val syncId = raw(dinner).syncId!!
        // Another device soft deletes the SMS transaction; its entries arrive later or stay behind.
        db.transactionDao().softDeleteTransaction(dinner, "now")
        assertEquals(mapOf("Radhika" to 0.0, "Omar" to 0.0), balances())
        assertTrue(debtRepo.debtEntries(radhika).first().isEmpty())
        assertEquals(0.0, db.debtDao().getBalance(radhika), 0.001)
        assertNull(debtRepo.transactionIdForSyncId(syncId))
        // Settling ignores them too, so nothing is written.
        debtRepo.settleDebt(radhika)
        assertTrue(debtRepo.debtEntries(radhika).first().isEmpty())
    }

    @Test
    fun deletingAnSmsTransactionRemovesItsEntries() = runBlocking {
        val cat = categoryId("expense")
        val sms = transactionRepo.insertTransaction(
            Transaction(
                amount = 900.0, type = "expense", categoryId = cat, accountId = 2, note = "",
                transactionDate = "2026-11-08T20:00:00", createdAt = "c", updatedAt = "u",
                merchantName = "Cab", source = "sms", transactionHash = "hash-1"
            )
        ).toInt()
        debtRepo.linkTransactionToDebt(sms, null, "Radhika", 450.0)
        transactionRepo.deleteTransaction(sms)
        assertTrue(raw(sms).isDeleted)
        assertTrue(db.debtDao().getEntriesForTransaction(raw(sms).syncId!!).isEmpty())
        assertEquals(0.0, balances().getValue("Radhika"), 0.001)
    }

    @Test
    fun savingASplitTwiceReplacesTheEntries() = runBlocking {
        val (dinner, radhika, omar) = splitDinner()
        debtRepo.saveTransactionSplit(dinner, listOf(DebtShare(radhika, null, 600.0)))
        debtRepo.linkTransactionToDebt(dinner, radhika, null, 600.0)
        assertEquals(listOf(600.0), entryAmounts(radhika))
        assertTrue(entryAmounts(omar).isEmpty())
        assertEquals(1, db.debtDao().getEntriesForTransaction(raw(dinner).syncId!!).size)
        debtRepo.removeTransactionSplit(dinner)
        assertEquals(mapOf("Radhika" to 0.0, "Omar" to 0.0), balances())
    }

    @Test
    fun rescalingKeepsTheSplitTotalToThePaisa() = runBlocking {
        val bill = transaction("expense", 100.0, "Snacks")
        val (a, b, c) = debtRepo.saveTransactionSplit(
            bill,
            listOf(DebtShare(null, "A", 33.34), DebtShare(null, "B", 33.33), DebtShare(null, "C", 33.33))
        )
        fun sumPaise() = listOf(a, b, c).sumOf { p -> entryAmounts(p).sumOf { Math.round(it * 100) } }

        transactionRepo.updateTransaction(raw(bill).copy(amount = 1.0))
        assertEquals(100L, sumPaise())
        assertEquals(listOf(0.34), entryAmounts(a))
        assertEquals(listOf(0.33), entryAmounts(b))

        transactionRepo.updateTransaction(raw(bill).copy(amount = 100.0))
        assertEquals(10_000L, sumPaise())

        // A type flip still flips every sign.
        transactionRepo.updateTransaction(raw(bill).copy(type = "income", categoryId = categoryId("income")))
        assertEquals(-10_000L, sumPaise())
    }

    @Test
    fun splitEntriesGetFixedSyncIdsAndAreUpdatedInPlace() = runBlocking {
        val (dinner, radhika, omar) = splitDinner()
        val txSync = raw(dinner).syncId!!
        val radhikaSync = db.debtDao().getPerson(radhika)!!.syncId!!
        val before = db.debtDao().getEntriesForTransaction(txSync)
        assertEquals(
            setOf(
                DebtRepository.splitEntrySyncId(txSync, radhikaSync),
                DebtRepository.splitEntrySyncId(txSync, db.debtDao().getPerson(omar)!!.syncId!!)
            ),
            before.map { it.syncId }.toSet()
        )
        debtRepo.saveTransactionSplit(dinner, listOf(DebtShare(radhika, null, 700.0)))
        val after = db.debtDao().getEntriesForTransaction(txSync).single()
        // Same row, new amount; Omar's row is gone.
        assertEquals(before.first { it.personId == radhika }.id, after.id)
        assertEquals(700.0, after.amount, 0.001)
    }

    @Test
    fun anOldRandomIdSplitEntryMovesOntoTheFixedId() = runBlocking {
        val dinner = transaction("expense", 1200.0, "Dinner")
        val txSync = raw(dinner).syncId!!
        val radhika = debtRepo.addDebtPerson("Radhika")
        // Written the old way: no syncId, so the trigger picks a random one.
        debtRepo.addDebtEntry(radhika, 400.0, "Dinner", transactionSyncId = txSync)
        assertTrue(!db.debtDao().getEntriesForTransaction(txSync).single().syncId!!.startsWith("split:"))
        assertEquals(listOf(400.0), entryAmounts(radhika))

        debtRepo.saveTransactionSplit(dinner, listOf(DebtShare(radhika, null, 500.0)))
        val entry = db.debtDao().getEntriesForTransaction(txSync).single()
        assertEquals(
            DebtRepository.splitEntrySyncId(txSync, db.debtDao().getPerson(radhika)!!.syncId!!),
            entry.syncId
        )
        assertEquals(listOf(500.0), entryAmounts(radhika))
    }

    @Test
    fun manualEntriesCanBeEdited() = runBlocking {
        val id = debtRepo.addDebtPerson("Sarthak", 4000.0)
        val entry = debtRepo.debtEntries(id).first().single()
        debtRepo.updateDebtEntry(entry.id, -250.0, "Borrowed", "2026-10-01T10:00:00")
        val edited = debtRepo.debtEntries(id).first().single()
        assertEquals(-250.0, edited.amount, 0.001)
        assertEquals("Borrowed", edited.note)
        assertEquals("2026-10-01T10:00:00", edited.entryDate)
    }

    @Test
    fun aBillSomeoneElsePaidRecordsOnlyMyShareOnThem() = runBlocking {
        val omar = debtRepo.addDebtPerson("Omar")
        val payer = SplitPerson(omar, "Omar")
        val plan = SplitPlan(people = listOf(payer, SplitPerson(null, "Radhika")))
        val outcome = plan.computeBill(900.0, payer)
        assertTrue(outcome.isValid)
        debtRepo.recordBillSplit(outcome.billEntries(payer), "Movie", "2026-11-08T20:00:00")
        // I owe Omar my 300. Radhika owes Omar, not me, so she isn't added.
        assertEquals(mapOf("Omar" to -300.0), balances())
        assertEquals("Movie", debtRepo.debtEntries(omar).first().single().note)
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
