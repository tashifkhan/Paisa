package com.pennywiseai.parser.notification

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.bank.BankParserFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class NotificationCoreTest {

    private val gpay = NotificationApps.find("com.google.android.apps.nbu.paisa.user")!!
    private val phonepe = NotificationApps.find("com.phonepe.app")!!

    @Test
    fun everyBankAppSenderHasAParser() {
        NotificationApps.banks.forEach { app ->
            assertTrue(
                "${app.name} sender ${app.parserSender} has no parser",
                BankParserFactory.getParsers(app.parserSender!!).isNotEmpty()
            )
        }
    }

    @Test
    fun packagesAreUnique() {
        assertEquals(NotificationApps.all.size, NotificationApps.all.map { it.packageName }.toSet().size)
    }

    @Test
    fun readsPaidTo() {
        val p = PaymentNotificationParser.parse(gpay, "₹250 paid to Swiggy\nUPI Ref: 412345678901", 0L)!!
        assertEquals(BigDecimal("250"), p.amount)
        assertEquals(TransactionType.EXPENSE, p.type)
        assertEquals("Swiggy", p.merchant)
        assertEquals("412345678901", p.reference)
        assertEquals("Google Pay", p.bankName)
    }

    @Test
    fun readsPaidAmountToName() {
        val p = PaymentNotificationParser.parse(phonepe, "Paid ₹1,200.50 to Arjun Kumar successfully", 0L)!!
        assertEquals(BigDecimal("1200.50"), p.amount)
        assertEquals("Arjun Kumar", p.merchant)
        assertEquals(TransactionType.EXPENSE, p.type)
    }

    @Test
    fun readsSentYouAsIncome() {
        val p = PaymentNotificationParser.parse(gpay, "Priya sent you ₹500", 0L)!!
        assertEquals(TransactionType.INCOME, p.type)
        assertEquals("Priya", p.merchant)
    }

    @Test
    fun readsReceivedFrom() {
        val p = PaymentNotificationParser.parse(phonepe, "Received ₹300 from Rahul Sharma", 0L)!!
        assertEquals(TransactionType.INCOME, p.type)
        assertEquals("Rahul Sharma", p.merchant)
    }

    @Test
    fun readsTitleThenBody() {
        val p = PaymentNotificationParser.parse(
            gpay,
            "Payment successful\n₹250 paid to Swiggy\nUPI Ref: 412345678901",
            0L
        )!!
        assertEquals(BigDecimal("250"), p.amount)
        assertEquals("Swiggy", p.merchant)
        assertEquals(TransactionType.EXPENSE, p.type)
    }

    @Test
    fun readsSentToAndReceivedWithoutASenderName() {
        val sent = PaymentNotificationParser.parse(gpay, "Sent ₹200 to Ramesh on Google Pay", 0L)!!
        assertEquals(TransactionType.EXPENSE, sent.type)
        assertEquals("Ramesh", sent.merchant)

        val received = PaymentNotificationParser.parse(
            phonepe,
            "You have received ₹1,000 from Anil",
            0L
        )!!
        assertEquals(TransactionType.INCOME, received.type)
        assertEquals("Anil", received.merchant)
    }

    @Test
    fun readsADebitWithNoPayeeAsUnknown() {
        val p = PaymentNotificationParser.parse(
            phonepe,
            "₹500 debited from your account XX4455",
            0L
        )!!
        assertEquals(TransactionType.EXPENSE, p.type)
        assertEquals("4455", p.accountLast4)
        assertNull(p.merchant)
    }

    @Test
    fun keepsCardInAMerchantName() {
        val p = PaymentNotificationParser.parse(
            gpay,
            "Rs.456.78 paid to Metro Card UPI Ref: 555566667777",
            0L
        )!!
        assertEquals("Metro Card", p.merchant)
    }

    @Test
    fun readsSignsBeforeTheAmount() {
        val plus = PaymentNotificationParser.parse(gpay, "+₹500 from Acme payroll", 0L)!!
        assertEquals(TransactionType.INCOME, plus.type)
        assertEquals(BigDecimal("500"), plus.amount)

        val minus = PaymentNotificationParser.parse(gpay, "-₹500 to Swiggy", 0L)!!
        assertEquals(TransactionType.EXPENSE, minus.type)
        assertEquals(BigDecimal("500"), minus.amount)
    }

    @Test
    fun readsABareSignedAmount() {
        val p = PaymentNotificationParser.parse(phonepe, "-500 to Metro Card", 0L)!!
        assertEquals(TransactionType.EXPENSE, p.type)
        assertEquals(BigDecimal("500"), p.amount)
        assertEquals("Metro Card", p.merchant)
    }

    @Test
    fun readsCreditAndDebitWords() {
        val credited = PaymentNotificationParser.parse(
            phonepe,
            "INR 1,200.50 credited to your account from Acme",
            0L
        )!!
        assertEquals(TransactionType.INCOME, credited.type)
        assertEquals(BigDecimal("1200.50"), credited.amount)

        val debited = PaymentNotificationParser.parse(
            gpay,
            "Rs.99 debited for a subscription",
            0L
        )!!
        assertEquals(TransactionType.EXPENSE, debited.type)
        assertEquals("subscription", debited.merchant)
    }

    @Test
    fun skipsBalanceOnlyMessages() {
        assertNull(
            PaymentNotificationParser.parse(gpay, "Available balance ₹5,000.00", 0L)
        )
        assertNull(
            PaymentNotificationParser.parse(gpay, "Avl Bal: -₹500", 0L)
        )
    }

    @Test
    fun picksThePaymentAmountOverTheBalance() {
        val first = PaymentNotificationParser.parse(
            gpay,
            "Avl Bal Rs.5,000.00. Rs.200.00 debited to Swiggy",
            0L
        )!!
        assertEquals(BigDecimal("200.00"), first.amount)
        assertEquals("Swiggy", first.merchant)

        val second = PaymentNotificationParser.parse(
            gpay,
            "Rs.200.00 debited to Swiggy. Avl Bal Rs.5,000.00",
            0L
        )!!
        assertEquals(BigDecimal("200.00"), second.amount)
    }

    @Test
    fun aDateIsNotASignedAmount() {
        assertNull(
            PaymentNotificationParser.parse(gpay, "Payment received on 20-Oct-25", 0L)
        )
    }

    @Test
    fun readsTowardsAndForAsThePayee() {
        val towards = PaymentNotificationParser.parse(gpay, "₹300 paid towards Metro Card", 0L)!!
        assertEquals("Metro Card", towards.merchant)

        val forName = PaymentNotificationParser.parse(gpay, "₹120 paid for groceries", 0L)!!
        assertEquals("groceries", forName.merchant)
    }

    @Test
    fun customAppsJoinTheRegistry() {
        val custom = NotificationApp(
            packageName = "com.example.bank",
            name = "Example Bank",
            kind = NotificationAppKind.BANK
        )
        try {
            assertTrue(NotificationApps.addCustom(custom))
            assertEquals("Example Bank", NotificationApps.find("com.example.bank")?.name)
            assertTrue(NotificationApps.find("com.example.bank")?.custom == true)
            assertTrue(NotificationApps.all.any { it.packageName == "com.example.bank" })

            // A built-in can't be replaced by a custom entry.
            assertTrue(
                !NotificationApps.addCustom(
                    NotificationApp("com.snapwork.hdfc", "Fake HDFC", NotificationAppKind.PAYMENT)
                )
            )
            assertEquals("HDFC Bank", NotificationApps.find("com.snapwork.hdfc")?.name)
        } finally {
            NotificationApps.removeCustom("com.example.bank")
        }
        assertNull(NotificationApps.find("com.example.bank"))
    }

    @Test
    fun skipsAccountAsPayee() {
        val p = PaymentNotificationParser.parse(phonepe, "Paid ₹99 from HDFC Bank XX1234 to Zomato", 0L)!!
        assertEquals("Zomato", p.merchant)
        assertEquals("1234", p.accountLast4)
    }

    @Test
    fun ignoresOffersRequestsAndFailures() {
        listOf(
            "Get ₹100 cashback on your next payment!",
            "Arjun requested ₹500 from you",
            "Payment of ₹250 to Swiggy failed",
            "Your electricity bill of ₹1,340 is due tomorrow. Pay now",
            "Payment of ₹250 is pending",
            "Scratch card won: ₹10"
        ).forEach { assertNull(it, PaymentNotificationParser.parse(gpay, it, 0L)) }
    }

    @Test
    fun needsAnAmountAndADirection() {
        assertNull(PaymentNotificationParser.parse(gpay, "Your UPI ID is ready", 0L))
        assertNull(PaymentNotificationParser.parse(gpay, "₹250 at Swiggy", 0L))
    }

    @Test
    fun messageJoinsTitleAndDropsRepeats() {
        val m = NotificationText.message(
            title = "Payment successful",
            bigText = "₹250 paid to Swiggy. UPI Ref 412345678901",
            text = "₹250 paid to Swiggy"
        )
        assertEquals("Payment successful\n₹250 paid to Swiggy. UPI Ref 412345678901", m)
    }

    @Test
    fun skipsSummariesAndOngoing() {
        assertTrue(NotificationText.skips(NotificationText.FLAG_GROUP_SUMMARY))
        assertTrue(NotificationText.skips(NotificationText.FLAG_ONGOING_EVENT))
        assertTrue(!NotificationText.skips(0))
    }

    private fun charge(
        channel: Channel,
        at: Long = 0L,
        amount: Double = 250.0,
        bank: String? = "HDFC Bank",
        merchant: String? = "Swiggy",
        last4: String? = null,
        ref: String? = null
    ) = Charge(amount, "expense", at, channel, bank, merchant, last4, ref)

    @Test
    fun bankSmsAfterUpiAppEnrichesTheAppRow() {
        val app = charge(Channel.PAYMENT_APP, bank = "Google Pay")
        val sms = charge(Channel.SMS, at = 90_000L, merchant = "swiggy@ybl", last4 = "1234")
        assertEquals(CrossChannelDedup.Verdict.ENRICH_EXISTING, CrossChannelDedup.verdict(sms, app))
    }

    @Test
    fun bankSmsAfterBankAppEnrichesTheAppRow() {
        val app = charge(Channel.BANK_APP, at = 60_000L)
        val sms = charge(Channel.SMS, at = 90_000L, last4 = "1234")
        assertEquals(CrossChannelDedup.Verdict.ENRICH_EXISTING, CrossChannelDedup.verdict(sms, app))
        // The other way round the app has nothing the SMS lacks, so it's a plain repeat.
        assertEquals(CrossChannelDedup.Verdict.DUPLICATE, CrossChannelDedup.verdict(app, sms))
    }

    @Test
    fun upiAppAfterBankSmsIsADuplicate() {
        val sms = charge(Channel.SMS)
        val app = charge(Channel.PAYMENT_APP, at = 60_000L, bank = "PhonePe", merchant = "Swiggy Ltd")
        assertEquals(CrossChannelDedup.Verdict.DUPLICATE, CrossChannelDedup.verdict(app, sms))
    }

    @Test
    fun upiAppTooFarApartIsDistinct() {
        val sms = charge(Channel.SMS)
        val app = charge(Channel.PAYMENT_APP, at = CrossChannelDedup.PAYMENT_APP_WINDOW_MS + 1)
        assertEquals(CrossChannelDedup.Verdict.DISTINCT, CrossChannelDedup.verdict(app, sms))
    }

    @Test
    fun sameChannelTwinsAreTwoPurchases() {
        assertEquals(
            CrossChannelDedup.Verdict.DISTINCT,
            CrossChannelDedup.verdict(charge(Channel.SMS, at = 30_000L), charge(Channel.SMS))
        )
    }

    @Test
    fun sharedUpiReferenceMatchesEvenWithinOneChannel() {
        val a = charge(Channel.SMS, ref = "412345678901")
        val b = charge(Channel.SMS, at = 3 * 60 * 60_000L, ref = "412345678901")
        assertEquals(CrossChannelDedup.Verdict.DUPLICATE, CrossChannelDedup.verdict(b, a))
    }

    @Test
    fun twoSmsWithOneBankReferenceAreOneCharge() {
        // A single SBI debit can arrive in two texts with the same 15-digit reference.
        val a = charge(Channel.SMS, amount = 875.0, bank = "State Bank of India", last4 = "1234", ref = "123456789012345")
        val b = charge(Channel.SMS, at = 20_000L, amount = 875.0, bank = "State Bank of India", last4 = "1234", ref = "123456789012345")
        assertEquals(CrossChannelDedup.Verdict.DUPLICATE, CrossChannelDedup.verdict(b, a))
    }

    @Test
    fun shortReferencesProveNothing() {
        val a = charge(Channel.SMS, ref = "123456")
        val b = charge(Channel.SMS, at = 30_000L, ref = "123456")
        assertEquals(CrossChannelDedup.Verdict.DISTINCT, CrossChannelDedup.verdict(b, a))
    }

    @Test
    fun differentKindsOfReferenceFallBackToTheOtherRules() {
        // A UPI app's RRN and the bank app's own number for one charge.
        val sms = charge(Channel.SMS, ref = "412345678901")
        val app = charge(Channel.BANK_APP, at = 60_000L, ref = "TXN99887766554")
        assertEquals(CrossChannelDedup.Verdict.DUPLICATE, CrossChannelDedup.verdict(app, sms))
    }

    @Test
    fun differentUpiReferencesNeverMatch() {
        val a = charge(Channel.PAYMENT_APP, ref = "412345678901")
        val b = charge(Channel.SMS, at = 1_000L, ref = "412345678902")
        assertEquals(CrossChannelDedup.Verdict.DISTINCT, CrossChannelDedup.verdict(b, a))
    }

    @Test
    fun bankAppNeedsSameBankAndMerchant() {
        val sms = charge(Channel.SMS)
        assertEquals(
            CrossChannelDedup.Verdict.DUPLICATE,
            CrossChannelDedup.verdict(charge(Channel.BANK_APP, at = 60_000L, merchant = "SWIGGY"), sms)
        )
        assertEquals(
            CrossChannelDedup.Verdict.DISTINCT,
            CrossChannelDedup.verdict(charge(Channel.BANK_APP, at = 60_000L, merchant = "Zomato"), sms)
        )
        assertEquals(
            CrossChannelDedup.Verdict.DISTINCT,
            CrossChannelDedup.verdict(charge(Channel.BANK_APP, at = 60_000L, bank = "ICICI Bank"), sms)
        )
    }

    @Test
    fun differentAccountsOrAmountsAreDistinct() {
        val sms = charge(Channel.SMS, last4 = "1111")
        assertEquals(
            CrossChannelDedup.Verdict.DISTINCT,
            CrossChannelDedup.verdict(charge(Channel.PAYMENT_APP, last4 = "2222"), sms)
        )
        assertEquals(
            CrossChannelDedup.Verdict.DISTINCT,
            CrossChannelDedup.verdict(charge(Channel.PAYMENT_APP, amount = 251.0), sms)
        )
    }

    @Test
    fun manualEntriesAreLeftAlone() {
        assertEquals(
            CrossChannelDedup.Verdict.DISTINCT,
            CrossChannelDedup.verdict(charge(Channel.SMS), charge(Channel.OTHER))
        )
    }

    @Test
    fun firstMatchPrefersEnrich() {
        // A bank app alert already saved from SMS is a plain repeat; the same alert
        // saved from a payment app can take this bank's account details.
        val incoming = charge(Channel.BANK_APP)
        val match = CrossChannelDedup.firstMatch(
            incoming,
            listOf(
                1 to charge(Channel.SMS, at = 10_000L),
                2 to charge(Channel.PAYMENT_APP, at = 20_000L, bank = "Paytm")
            )
        )
        assertNotNull(match)
        assertEquals(2, match!!.first)
        assertEquals(CrossChannelDedup.Verdict.ENRICH_EXISTING, match.second)
    }
}
