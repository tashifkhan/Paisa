package codes.tashif.paisa.ingest

import codes.tashif.paisa.ingest.IngestResult.Outcome
import codes.tashif.paisa.testing.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** One payment arriving as a UPI app notification and then as a bank SMS becomes one row. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationDedupTest {

    private val fixture = IngestFixture(inMemoryDatabase())
    private val db = fixture.db
    private val ingestor = fixture.ingestor

    private val gpay = "com.google.android.apps.nbu.paisa.user"
    private val notification = "₹500 paid to Swiggy\nUPI Ref: 123456789012"
    private val hdfcSms = "Rs.500.00 debited from A/c XX1234 on 20-Oct-25 to merchant@upi (UPI Ref No 123456789012)"
    private val at = 1_761_000_000_000L

    @After
    fun tearDown() = db.close()

    @Test
    fun bankSmsAfterUpiNotificationKeepsOneRow() = runBlocking {
        val saved = ingestor.processNotification(gpay, notification, at)
        assertEquals(saved.detail, Outcome.SAVED, saved.outcome)
        val appRow = db.transactionDao().getRawTransactionById(saved.transactionId!!.toInt())!!
        assertEquals("notification", appRow.source)
        assertEquals("Swiggy", appRow.merchantName)

        // The bank SMS arrives a minute later and adds the real account to the same row.
        val merged = ingestor.processSms("CP-HDFCBK-S", hdfcSms, at + 60_000)
        assertEquals(merged.detail, Outcome.MERGED, merged.outcome)
        assertEquals(saved.transactionId, merged.transactionId)

        val rows = db.transactionDao().getAllTransactions().first()
        assertEquals(1, rows.size)
        val row = db.transactionDao().getRawTransactionById(rows.single().id)!!
        assertEquals("sms", row.source)
        assertEquals("HDFC Bank", row.bankName)
        assertEquals("1234", row.accountNumber)
        assertEquals("Swiggy", row.merchantName)
    }

    @Test
    fun theEmptyAppAccountGoesAfterAMerge() = runBlocking {
        // Two bank accounts, so Google Pay's payment waits on its holding account.
        fixture.accounts.insertAccount(
            codes.tashif.paisa.data.Account(
                name = "SBI", type = "Bank Account", openingBalance = 0.0, currentBalance = 0.0,
                icon = "account_balance", color = "#A0C4FF", bankName = "SBI", accountLast4 = "9999"
            )
        )
        assertTrue(ingestor.processNotification(gpay, notification, at).needsAccount)
        assertTrue(fixture.accounts.allAccounts.first().any { it.name == "Google Pay" })

        ingestor.processSms("CP-HDFCBK-S", hdfcSms, at + 60_000)

        val names = fixture.accounts.allAccounts.first().map { it.name }
        assertFalse(names.toString(), "Google Pay" in names)
        assertTrue(names.toString(), names.any { it.startsWith("HDFC Bank") })
    }

    @Test
    fun upiNotificationAfterBankSmsIsDropped() = runBlocking {
        assertEquals(Outcome.SAVED, ingestor.processSms("CP-HDFCBK-S", hdfcSms, at).outcome)
        assertEquals(Outcome.DUPLICATE, ingestor.processNotification(gpay, notification, at + 60_000).outcome)
        assertEquals(1, db.transactionDao().getAllTransactions().first().size)
    }

    @Test
    fun aRepostOfTheSameNotificationIsDropped() = runBlocking {
        assertEquals(Outcome.SAVED, ingestor.processNotification(gpay, notification, at).outcome)
        assertEquals(Outcome.DUPLICATE, ingestor.processNotification(gpay, notification, at).outcome)
        assertEquals(1, db.transactionDao().getAllTransactions().first().size)
    }

    @Test
    fun offersFromAPaymentAppNeverReachTheReviewList() = runBlocking {
        val offer = ingestor.processNotification(gpay, "Get ₹100 cashback on your next bill payment!", at)
        assertEquals(Outcome.IGNORED, offer.outcome)
        val reminder = ingestor.processNotification(gpay, "Your electricity bill of ₹1,240 is due tomorrow", at)
        assertEquals(Outcome.IGNORED, reminder.outcome)
        assertTrue(fixture.inbox.pendingUnrecognizedSms.first().isEmpty())
    }

    @Test
    fun anUnreadablePaymentFromAnAppIsKeptForReview() = runBlocking {
        // No ₹, Rs or sign, so the generic reader passes; without the model it waits for review.
        val result = ingestor.processNotification(gpay, "Acme payroll added 5,000 to your account", at)
        assertEquals(result.detail, Outcome.NEEDS_REVIEW, result.outcome)
        val row = fixture.inbox.pendingUnrecognizedSms.first().single()
        assertEquals("Google Pay", row.senderName)
        assertFalse("the model gets a try once it is turned on", row.aiAttempted)
    }
}
