package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class CanaraBankParserTest {
    private val timestamp = 1_752_822_540_000L

    @Test
    fun `parses abbreviated debit messages shown in the inbox`() {
        val cases = listOf(
            ExpectedDebit(
                sender = "VK-CANBNK-S",
                body = "Dear Customer, Acct XXX270 Dr. INR 260.00 on 06/07/26 to WENGERS; UPI: 655371142911; Bal INR 2,18,839.89.Not you? SMS BLOCKUPI to 9901771222-CanaraBank",
                amount = "260.00",
                merchant = "WENGERS",
                reference = "655371142911",
                balance = "218839.89"
            ),
            ExpectedDebit(
                sender = "VK-CANBNK-S",
                body = "Dear Customer, Acct XXX270 Dr. INR 600.00 on 17/07/26 to RAHUL; UPI: 656405512195; Bal INR 1,85,086.15.Not you?SMS BLOCKUPI to 9901771222-CanaraBank",
                amount = "600.00",
                merchant = "RAHUL",
                reference = "656405512195",
                balance = "185086.15"
            ),
            ExpectedDebit(
                sender = "VK-CANBNK-S",
                body = "Dear Customer, Acct XXX270 Dr. INR 41.00 on 17/07/26 to ROPPEN TRANS; UPI: 656426837704; Bal INR 1,84,932.15.Not you? SMS BLOCKUPI to 9901771222-CanaraBank",
                amount = "41.00",
                merchant = "ROPPEN TRANS",
                reference = "656426837704",
                balance = "184932.15"
            ),
            ExpectedDebit(
                sender = "VK-CANBNK-S",
                body = "Dear Customer, Acct XXX270 Dr. INR 354.00 on 17/07/26 to Pabitra  Sin; UPI: 656459677785; Bal INR 1,84,047.15.Not you? SMS BLOCKUPI to 9901771222-CanaraBank",
                amount = "354.00",
                merchant = "Pabitra Sin",
                reference = "656459677785",
                balance = "184047.15"
            ),
            ExpectedDebit(
                sender = "JK-CANBNK-S",
                body = "Dear Customer, Acct XXX270 Dr. INR 165.00 on 18/07/26 to SUNEETA; UPI: 619948868248; Bal INR 1,83,844.15.Not you?SMS BLOCKUPI to 9901771222-CanaraBank",
                amount = "165.00",
                merchant = "SUNEETA",
                reference = "619948868248",
                balance = "183844.15"
            )
        )

        cases.forEach { expected ->
            assertFalse("Sender should be recognized: ${expected.sender}", BankParserFactory.getParsers(expected.sender).isEmpty())
            val parsed = BankParserFactory.parse(expected.body, expected.sender, timestamp)

            assertNotNull("Message should parse: ${expected.body}", parsed)
            requireNotNull(parsed)
            assertEquals(BigDecimal(expected.amount), parsed.amount)
            assertEquals(TransactionType.EXPENSE, parsed.type)
            assertEquals(expected.merchant, parsed.merchant)
            assertEquals(expected.reference, parsed.reference)
            assertEquals("270", parsed.accountLast4)
            assertEquals(BigDecimal(expected.balance), parsed.balance)
            assertEquals("Canara Bank", parsed.bankName)
        }
    }

    @Test
    fun `does not parse Canara ATM usage notification as a transaction`() {
        val body = "Card ending 4395: Dear Customer, you have done 04 out of 06 free transactions at Canara ATMs in this month. Charges applicable beyond free transactions.-Canara Bank"

        assertNull(BankParserFactory.parse(body, "JK-CANBNK-S", timestamp))
    }

    private data class ExpectedDebit(
        val sender: String,
        val body: String,
        val amount: String,
        val merchant: String,
        val reference: String,
        val balance: String
    )
}
