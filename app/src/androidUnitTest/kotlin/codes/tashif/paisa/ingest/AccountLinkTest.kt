package codes.tashif.paisa.ingest

import codes.tashif.paisa.data.Account
import codes.tashif.paisa.notifications.UNASSIGNED_ACCOUNT_TYPE
import codes.tashif.paisa.testing.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Which account an app's payment lands on, the review list, and merchant renames. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountLinkTest {

    private val fixture = IngestFixture(inMemoryDatabase())
    private val ingestor = fixture.ingestor
    private val gpay = "com.google.android.apps.nbu.paisa.user"
    private val hdfcApp = "com.snapwork.hdfc"
    private var at = 1_761_000_000_000L

    @After
    fun tearDown() = fixture.db.close()

    private fun account(name: String, bank: String?, last4: String?, type: String = "Bank Account"): Int = runBlocking {
        fixture.accounts.insertAccount(
            Account(
                name = name, type = type, openingBalance = 0.0, currentBalance = 0.0,
                icon = "account_balance", color = "#A0C4FF", bankName = bank, accountLast4 = last4
            )
        ).toInt()
    }

    /** A Google Pay payment, a new one each call so nothing is a duplicate. */
    private suspend fun pay(text: String): IngestResult {
        at += 3_600_000
        return ingestor.processNotification(gpay, text, at)
    }

    private suspend fun accountOf(result: IngestResult): Account =
        fixture.accounts.getAccountById(fixture.db.transactionDao().getRawTransactionById(result.transactionId!!.toInt())!!.accountId)!!

    @Test
    fun withOneBankAccountAUpiPaymentGoesThere() = runBlocking {
        val result = pay("₹250 paid to Swiggy")
        assertEquals(IngestResult.Outcome.SAVED, result.outcome)
        assertFalse(result.needsAccount)
        // The seeded "Bank Account" is the only one.
        assertEquals("Bank Account", accountOf(result).name)
    }

    @Test
    fun withTwoBankAccountsThePaymentWaitsForAChoice() = runBlocking {
        account("SBI Savings", "SBI", "9999")
        val result = pay("₹250 paid to Swiggy")
        assertTrue(result.needsAccount)
        val holding = accountOf(result)
        assertEquals("Google Pay", holding.name)
        assertEquals(UNASSIGNED_ACCOUNT_TYPE, holding.type)
        assertEquals(1, fixture.approval.awaiting.first().size)
        assertEquals("Google Pay", fixture.approval.awaiting.first().single().appName)
        // The holding account is never offered as a choice.
        assertTrue(fixture.approval.choices.first().none { it.type == UNASSIGNED_ACCOUNT_TYPE })
    }

    @Test
    fun choosingWithAlwaysMovesTheWaitingOnesAndLinksTheApp() = runBlocking {
        val sbi = account("SBI Savings", "SBI", "9999")
        val first = pay("₹250 paid to Swiggy")
        pay("₹120 paid to Chaayos")

        val moved = fixture.approval.assign(first.transactionId!!.toInt(), sbi, remember = true)

        assertEquals(2, moved)
        assertTrue(fixture.approval.awaiting.first().isEmpty())
        assertTrue("holding account is gone", fixture.accounts.allAccounts.first().none { it.type == UNASSIGNED_ACCOUNT_TYPE })
        assertEquals(sbi, fixture.links.linkedAccount(gpay, null))
        // The next payment goes straight to SBI.
        val next = pay("₹90 paid to Uber")
        assertFalse(next.needsAccount)
        assertEquals(sbi, accountOf(next).id)
    }

    @Test
    fun choosingOnceLeavesTheRestWaiting() = runBlocking {
        val sbi = account("SBI Savings", "SBI", "9999")
        val first = pay("₹250 paid to Swiggy")
        pay("₹120 paid to Chaayos")

        assertEquals(1, fixture.approval.assign(first.transactionId!!.toInt(), sbi, remember = false))
        assertEquals(1, fixture.approval.awaiting.first().size)
        assertNull(fixture.links.linkedAccount(gpay, null))
    }

    @Test
    fun accountDigitsPickTheirOwnLinkBeforeTheDefault() = runBlocking {
        val sbi = account("SBI Savings", "SBI", "9999")
        val icici = account("ICICI Salary", "ICICI", "4321")
        fixture.links.setLink(gpay, null, sbi)
        fixture.links.setLink(gpay, "5678", icici)

        assertEquals(icici, accountOf(pay("₹300 paid to Zomato from A/c XX5678")).id)
        assertEquals(sbi, accountOf(pay("₹300 paid to Zomato")).id)
        // Digits that match exactly one account need no link.
        assertEquals(icici, accountOf(pay("₹300 paid to Zomato from A/c XX4321")).id)
    }

    @Test
    fun aBankAppUsesTheOnlyAccountAtThatBank() = runBlocking {
        account("SBI Savings", "SBI", "9999")
        val hdfc = account("HDFC Salary", "HDFC Bank", "1234")
        at += 3_600_000
        val result = ingestor.processNotification(hdfcApp, "Rs.500.00 debited from your account to swiggy@upi", at)
        assertEquals(result.detail, IngestResult.Outcome.SAVED, result.outcome)
        assertFalse(result.needsAccount)
        assertEquals(hdfc, accountOf(result).id)
    }

    @Test
    fun aRenameRuleAppliesToNewAndPastRows() = runBlocking {
        val old = pay("₹250 paid to SWIGGY LTD")
        assertEquals("Swiggy Ltd", fixture.db.transactionDao().getRawTransactionById(old.transactionId!!.toInt())!!.merchantName)

        fixture.categories.saveMerchantRename("Swiggy Ltd", "Swiggy")

        assertEquals("Swiggy", fixture.db.transactionDao().getRawTransactionById(old.transactionId!!.toInt())!!.merchantName)
        val next = pay("₹180 paid to SWIGGY LTD")
        assertEquals("Swiggy", fixture.db.transactionDao().getRawTransactionById(next.transactionId!!.toInt())!!.merchantName)
        assertEquals("Swiggy", fixture.categories.displayNameFor("swiggy ltd"))
        assertEquals(1, fixture.categories.allMerchantRenames.first().size)
    }

    @Test
    fun renamingBackToTheSameNameDropsTheRule() = runBlocking {
        fixture.categories.saveMerchantRename("Swiggy Ltd", "Swiggy")
        fixture.categories.saveMerchantRename("Swiggy Ltd", "Swiggy Ltd")
        assertTrue(fixture.categories.allMerchantRenames.first().isEmpty())
    }
}
