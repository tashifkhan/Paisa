package sh.taf.billi.ingest

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import sh.taf.billi.ai.local.LocalAiState
import sh.taf.billi.ai.store.MessageReaderMode
import sh.taf.billi.data.UnrecognizedSms
import sh.taf.billi.ingest.IngestResult.Outcome
import sh.taf.billi.testing.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Messages kept for Gemini Nano are saved and removed when the app next opens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PendingAiProcessorTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val engine = FakeEngine()
    private val gpay = "com.google.android.apps.nbu.paisa.user"
    private val payroll = "Acme payroll added 5,000 to your account"

    private var fixture = IngestFixture(inMemoryDatabase(), engine)

    @After
    fun tearDown() = fixture.db.close()

    private fun pending(f: IngestFixture = fixture) =
        PendingAiProcessor(context, f.ingestor, f.inbox, f.reader!!)

    private fun queue(sender: String, body: String, source: String, senderName: String? = null) = runBlocking {
        fixture.inbox.insertUnrecognizedSms(
            UnrecognizedSms(
                sender = sender,
                body = body,
                bodyHash = "hash-${body.take(8)}",
                timestamp = 1_761_000_000_000L,
                createdAt = "c",
                source = source,
                senderName = senderName
            )
        )
    }

    private fun postedTitles(): List<String?> =
        shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications
            .map { it.extras.getString("android.title") }

    @Test
    fun savesAQueuedNotification() = runBlocking {
        queue(gpay, payroll, "notification", "Google Pay")
        engine.reply = """{"isPayment":true,"amount":5000,"direction":"income","merchant":"Acme payroll"}"""

        assertEquals(1, pending().process())

        val row = fixture.db.transactionDao().getAllTransactions().first().single()
        assertEquals(5000.0, row.amount, 0.001)
        assertEquals("income", row.type)
        assertEquals("notification", row.source)
        assertEquals("Acme payroll", row.merchantName)
        assertEquals("Google Pay", row.bankName)
        assertTrue(fixture.inbox.pendingForAi().isEmpty())
        assertTrue(postedTitles().toString(), "Read with AI" in postedTitles())
    }

    @Test
    fun savesAQueuedSmsOnTheBanksAccount() = runBlocking {
        queue("AD-HDFCBK-S", "Acme payroll added 9,000 to your account", "sms")
        engine.reply = """{"isPayment":true,"amount":9000,"direction":"income","merchant":"Acme payroll"}"""

        assertEquals(1, pending().process())

        val row = fixture.db.transactionDao().getAllTransactions().first().single()
        assertEquals("sms", row.source)
        assertEquals("HDFC Bank", row.bankName)
        assertEquals(9000.0, row.amount, 0.001)
    }

    @Test
    fun aModelThatSaysNoKeepsAPaymentLookingMessageForReview() = runBlocking {
        queue(gpay, "Rs 5,000 received from Ravi for rent", "notification", "Google Pay")
        engine.reply = """{"isPayment":false}"""

        assertEquals(0, pending().process())
        assertTrue(fixture.db.transactionDao().getAllTransactions().first().isEmpty())
        assertTrue(fixture.inbox.pendingForAi().isEmpty())
        val row = fixture.inbox.pendingUnrecognizedSms.first().single()
        assertTrue(row.aiAttempted)
        assertEquals("Google Pay", row.senderName)
    }

    @Test
    fun aModelThatSaysNoDropsChatter() = runBlocking {
        queue(gpay, "Your account 5,000 points summary", "notification", "Google Pay")
        engine.reply = """{"isPayment":false}"""

        assertEquals(0, pending().process())
        assertTrue(fixture.inbox.pendingUnrecognizedSms.first().isEmpty())
    }

    @Test
    fun aModelThatCantRunNowLeavesTheMessageQueued() = runBlocking {
        queue(gpay, payroll, "notification", "Google Pay")
        // Still downloading: the model exists but can't run yet.
        engine.setState(LocalAiState.DOWNLOADING)

        assertEquals(0, pending().process())
        assertEquals(1, fixture.inbox.pendingForAi().size)
    }

    @Test
    fun aPhoneWithoutGeminiNanoNeverQueues() = runBlocking {
        // The switch came back on through a restored backup, on a phone with no AICore.
        engine.setState(LocalAiState.UNSUPPORTED)

        val result = fixture.ingestor.processNotification(gpay, payroll, 1_000L)

        assertEquals(Outcome.NEEDS_REVIEW, result.outcome)
        assertEquals(0, engine.calls)
        pending().promptForQueued("Google Pay")
        assertTrue(postedTitles().toString(), "Payment waiting for AI" !in postedTitles())
        val sms = fixture.ingestor.processSms("AD-HDFCBK-S", "Acme payroll added 7,000 to your account", 2_000L)
        assertEquals(Outcome.IGNORED, sms.outcome)
    }

    @Test
    fun liveMessagesAreQueuedWhenTheModelThrows() = runBlocking {
        engine.fails = true

        val notification = fixture.ingestor.processNotification(gpay, payroll, 1_000L)
        assertEquals(Outcome.QUEUED_FOR_AI, notification.outcome)
        val sms = fixture.ingestor.processSms("AD-HDFCBK-S", "Acme payroll added 7,000 to your account", 2_000L)
        assertEquals(Outcome.QUEUED_FOR_AI, sms.outcome)

        val queued = fixture.inbox.pendingForAi()
        assertEquals(2, queued.size)
        assertTrue(queued.any { it.source == "notification" && it.sender == gpay && it.senderName == "Google Pay" })
        assertTrue(queued.any { it.source == "sms" && it.sender == "AD-HDFCBK-S" })

        pending().promptForQueued("Google Pay")
        assertTrue(postedTitles().toString(), "Payment waiting for AI" in postedTitles())

        // Back in front, the model runs and the prompt goes away.
        engine.fails = false
        engine.reply = """{"isPayment":true,"amount":5000,"direction":"income","merchant":"Acme payroll"}"""
        pending().process()
        assertEquals(1, fixture.db.transactionDao().getAllTransactions().first().size)
    }

    @Test
    fun theBulkScanQueuesInsteadOfAskingTheModel() = runBlocking {
        engine.reply = """{"isPayment":true,"amount":7000,"direction":"income"}"""
        val result = fixture.ingestor.processSms(
            "AD-HDFCBK-S", "Acme payroll added 7,000 to your account", 2_000L, askModelNow = false
        )
        assertEquals(Outcome.QUEUED_FOR_AI, result.outcome)
        assertEquals(0, engine.calls)
    }

    @Test
    fun aKnownBanksUnreadableSmsIsDroppedWithoutTheModel() = runBlocking {
        val off = IngestFixture(inMemoryDatabase(), engine, localParser = false)
        try {
            val result = off.ingestor.processSms("AD-HDFCBK-S", "Acme payroll added 7,000 to your account", 2_000L)
            assertEquals(Outcome.IGNORED, result.outcome)
            assertTrue(off.inbox.pendingUnrecognizedSms.first().isEmpty())
        } finally {
            off.db.close()
        }
    }

    @Test
    fun doesNothingWhenTheSwitchIsOff() = runBlocking {
        fixture.db.close()
        fixture = IngestFixture(inMemoryDatabase(), engine, localParser = false)
        queue(gpay, payroll, "notification", "Google Pay")
        engine.reply = """{"isPayment":true,"amount":5000,"direction":"income"}"""

        assertEquals(0, pending().process())
        assertEquals(1, fixture.inbox.pendingForAi().size)
        assertEquals(0, engine.calls)
    }

    private fun cloudFixture(cloud: FakeCloud): IngestFixture {
        fixture.db.close()
        return IngestFixture(inMemoryDatabase(), engine, mode = MessageReaderMode.CLOUD, cloud = cloud).also { fixture = it }
    }

    @Test
    fun cloudReadsWithoutTheOnDeviceModel() = runBlocking {
        engine.setState(LocalAiState.UNSUPPORTED)
        val cloud = FakeCloud("""{"isPayment":true,"amount":5000,"direction":"income","merchant":"Acme payroll"}""")
        val f = cloudFixture(cloud)

        val result = f.ingestor.processNotification(gpay, payroll, 1_000L)

        assertEquals(result.detail, Outcome.SAVED, result.outcome)
        assertEquals(1, cloud.calls)
        assertEquals(0, engine.calls)
        assertEquals("Acme payroll", f.db.transactionDao().getAllTransactions().first().single().merchantName)
    }

    @Test
    fun cloudWithoutAKeyGoesToReviewInsteadOfWaiting() = runBlocking {
        val f = cloudFixture(FakeCloud(ready = false))

        assertEquals(Outcome.NEEDS_REVIEW, f.ingestor.processNotification(gpay, payroll, 1_000L).outcome)
        // No "waiting" alert: nothing can read it until a key is added, and then it gets a try.
        pending(f).promptForQueued("Google Pay")
        assertTrue(postedTitles().toString(), "Payment waiting for AI" !in postedTitles())
        assertEquals(0, pending(f).process())
    }

    @Test
    fun aCloudFailureWaitsAndIsReadLater() = runBlocking {
        val cloud = FakeCloud(fails = true)
        val f = cloudFixture(cloud)

        assertEquals(Outcome.QUEUED_FOR_AI, f.ingestor.processNotification(gpay, payroll, 1_000L).outcome)

        cloud.fails = false
        cloud.reply = """{"isPayment":true,"amount":5000,"direction":"income","merchant":"Acme payroll"}"""
        assertEquals(1, pending(f).process())
        assertTrue(f.inbox.pendingForAi().isEmpty())
    }
}
