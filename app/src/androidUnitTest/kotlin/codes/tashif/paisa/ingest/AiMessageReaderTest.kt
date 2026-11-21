package codes.tashif.paisa.ingest

import com.pennywiseai.parser.core.ParsedTransaction
import com.pennywiseai.parser.core.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class AiMessageReaderTest {

    private fun reader(reply: String, enabled: Boolean = true): Pair<AiMessageReader, FakeEngine> {
        val engine = FakeEngine(reply)
        return AiMessageReader(engine, aiConfig(enabled)) to engine
    }

    private suspend fun AiMessageReader.parse(message: String): ParsedTransaction? =
        (read(message, "s", "Bank", 0L) as? AiMessageReader.Result.Parsed)?.transaction

    @Test
    fun readsAPaymentTheRulesMissed() = runBlocking {
        val (reader, engine) = reader("""{"isPayment":true,"amount":5000,"direction":"income","merchant":"Acme payroll"}""")
        val result = reader.read("Acme payroll added 5,000 to your account", "com.example.bank", "Example Bank", 1L)
        val parsed = (result as AiMessageReader.Result.Parsed).transaction
        assertEquals(1, engine.calls)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(0, parsed.amount.compareTo(BigDecimal("5000")))
        assertEquals("Acme payroll", parsed.merchant)
        assertEquals("Example Bank", parsed.bankName)
    }

    @Test
    fun doesNothingWhenTheSwitchIsOff() = runBlocking {
        val (reader, engine) = reader("""{"isPayment":true,"amount":500,"direction":"expense"}""", enabled = false)
        assertEquals(AiMessageReader.Result.Unavailable, reader.read("Rs.500 paid to Swiggy", "s", "Bank", 0L))
        assertEquals(0, engine.calls)
    }

    @Test
    fun rejectsAnAmountThatIsNotInTheMessage() = runBlocking {
        val (reader, _) = reader("""{"isPayment":true,"amount":9000,"direction":"expense","merchant":"Swiggy"}""")
        assertNull(reader.parse("Rs.500 paid to Swiggy"))
    }

    @Test
    fun rejectsAnAmountThatIsOnlyPartOfALargerNumber() = runBlocking {
        val (reader, _) = reader("""{"isPayment":true,"amount":500,"direction":"expense"}""")
        assertNull(reader.parse("Rs.5,500 paid to Swiggy"))
        assertNull(reader.parse("Rs.1500 paid to Swiggy"))
    }

    @Test
    fun acceptsTheAmountWithPaiseInTheText() = runBlocking {
        val (reader, _) = reader("""{"isPayment":true,"amount":500,"direction":"expense"}""")
        assertEquals(0, reader.parse("Rs.500.00 paid to Swiggy")?.amount?.compareTo(BigDecimal("500")))
    }

    @Test
    fun rejectsANonPayment() = runBlocking {
        val (reader, _) = reader("""{"isPayment":false}""")
        assertNull(reader.parse("Rs.100 sent to your wallet"))
    }

    @Test
    fun aGarbledReplyCountsAsReadNotAsUnavailable() = runBlocking {
        val (reader, _) = reader("I think this is a payment")
        assertEquals(AiMessageReader.Result.NotPayment, reader.read("Rs.500 paid to Swiggy", "s", "Bank", 0L))
    }

    @Test
    fun aModelThatThrowsIsUnavailable() = runBlocking {
        val engine = FakeEngine(fails = true)
        val reader = AiMessageReader(engine, aiConfig(true))
        assertEquals(AiMessageReader.Result.Unavailable, reader.read("Rs.500 paid to Swiggy", "s", "Bank", 0L))
    }

    @Test
    fun skipsOffersAndOtpsBeforeCallingTheModel() = runBlocking {
        val (reader, engine) = reader("""{"isPayment":true,"amount":123,"direction":"expense"}""")
        assertNull(reader.parse("Your OTP for Rs.123 is 456789"))
        assertNull(reader.parse("Flat 50% off on Rs.500"))
        assertNull(reader.parse("Get Rs.100 cashback today"))
        assertEquals(0, engine.calls)
    }

    @Test
    fun acceptsCommasInTheMessageAmount() = runBlocking {
        val (reader, _) = reader("""{"isPayment":true,"amount":1200.5,"direction":"expense","merchant":"Store"}""")
        assertEquals(0, reader.parse("Rs.1,200.50 paid to Store")?.amount?.compareTo(BigDecimal("1200.5")))
    }

    @Test
    fun jsonNullsDoNotBecomeTheWordNull() = runBlocking {
        val (reader, _) = reader(
            """{"isPayment":true,"amount":500,"direction":"expense","merchant":null,"accountLast4":null,"reference":null}"""
        )
        val parsed = reader.parse("Rs.500 paid to a shop")
        assertEquals(0, parsed?.amount?.compareTo(BigDecimal("500")))
        assertNull(parsed?.merchant)
        assertNull(parsed?.accountLast4)
        assertNull(parsed?.reference)
    }

    @Test
    fun theUsersOwnAccountIsNotAPayee() = runBlocking {
        val (reader, _) = reader("""{"isPayment":true,"amount":2500,"direction":"income","merchant":"your account"}""")
        val parsed = reader.parse("Acme bonus 2,500 added to your account")
        assertEquals(0, parsed?.amount?.compareTo(BigDecimal("2500")))
        assertNull(parsed?.merchant)
    }
}
