package com.pennywiseai.parser.notification

import com.pennywiseai.parser.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PaymentTextTest {

    private val custom = NotificationApp("com.example.pay", "Example Pay", NotificationAppKind.PAYMENT)

    @Test
    fun offersAndRemindersAreNotPayments() {
        listOf(
            "Get ₹100 cashback on your next bill payment!",
            "Your electricity bill of ₹1,240 is due tomorrow",
            "Ravi requested ₹500 from you",
            "Payment of ₹300 failed",
            "Your OTP for ₹199 is 445566",
        ).forEach { text ->
            assertTrue(text, PaymentText.isPromotion(text))
            assertFalse(text, PaymentText.looksLikePayment(text))
            assertFalse(text, PaymentText.worthAskingModel(text))
        }
    }

    @Test
    fun aBalanceAlertIsNotAPayment() {
        assertTrue(PaymentText.isBalanceOnly("Avl Bal in A/c XX1234 is Rs 5,000.00"))
        assertFalse(PaymentText.looksLikePayment("Avl Bal in A/c XX1234 is Rs 5,000.00"))
        assertFalse(PaymentText.isBalanceOnly("Rs 500 debited, Avl Bal Rs 4,500"))
    }

    @Test
    fun paymentsLookLikePayments() {
        assertTrue(PaymentText.looksLikePayment("Rs 5,000 received from Ravi for rent"))
        assertTrue(PaymentText.looksLikePayment("Acme payroll added 5,000 to your account"))
        // Seen on a Pixel 11 Pro: no ₹ and no "paid", so it needs the word "payment".
        assertTrue(PaymentText.looksLikePayment("Payment of 2,345 to Blinkit was successful"))
        assertFalse(PaymentText.looksLikePayment("Your statement for October is ready with 42 transactions"))
        assertTrue(PaymentText.worthAskingModel("Acme payroll added 5,000 to your account"))
    }

    @Test
    fun aDashBeforeAReferenceIsNotACharge() {
        assertNull(PaymentNotificationParser.parse(custom, "Payment done. Ref - 412345678901", 0L))
    }

    @Test
    fun aSignedAmountStillReads() {
        val p = PaymentNotificationParser.parse(custom, "-1,250 at Store", 0L)!!
        assertEquals(TransactionType.EXPENSE, p.type)
        assertEquals(0, p.amount.compareTo(BigDecimal("1250")))
        assertEquals(TransactionType.INCOME, PaymentNotificationParser.parse(custom, "+450 from Ravi", 0L)!!.type)
    }

    @Test
    fun anIncomePayerNamedWithBy() {
        val p = PaymentNotificationParser.parse(custom, "Salary of Rs 42,000.00 credited to your account by Acme Labs", 0L)!!
        assertEquals(TransactionType.INCOME, p.type)
        assertEquals("Acme Labs", p.merchant)
    }
}
