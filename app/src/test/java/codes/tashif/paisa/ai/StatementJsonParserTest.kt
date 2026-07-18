package codes.tashif.paisa.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatementJsonParserTest {

    @Test
    fun parses_wrapped_transactions_object() {
        val raw = """
            {
              "transactions": [
                {
                  "date": "2026-03-15",
                  "amount": 349.0,
                  "type": "expense",
                  "merchant": "Zomato",
                  "currency": "INR"
                },
                {
                  "date": "15/03/2026",
                  "amount": "1,200.50",
                  "type": "debit",
                  "merchant": "BigBasket",
                  "reference": "UPI123"
                }
              ]
            }
        """.trimIndent()

        val rows = StatementJsonParser.parse(raw)
        assertEquals(2, rows.size)
        assertEquals("2026-03-15", rows[0].date)
        assertEquals(349.0, rows[0].amount, 0.001)
        assertEquals("expense", rows[0].type)
        assertEquals("Zomato", rows[0].merchant)
        assertEquals("2026-03-15", rows[1].date)
        assertEquals(1200.50, rows[1].amount, 0.001)
        assertEquals("expense", rows[1].type)
        assertEquals("UPI123", rows[1].reference)
    }

    @Test
    fun strips_markdown_fences() {
        val raw = """
            Here you go:
            ```json
            {"transactions":[{"date":"2026-01-01","amount":10,"type":"income","merchant":"Salary"}]}
            ```
        """.trimIndent()
        val rows = StatementJsonParser.parse(raw)
        assertEquals(1, rows.size)
        assertEquals("income", rows[0].type)
        assertEquals("Salary", rows[0].merchant)
    }

    @Test
    fun parses_data_key_and_credit_type() {
        val raw = """
            {"data":[{"date":"2026-02-01","amount":"₹500","type":"credit","description":"Refund"}]}
        """.trimIndent()
        val rows = StatementJsonParser.parse(raw)
        assertEquals(1, rows.size)
        assertEquals("income", rows[0].type)
        assertEquals(500.0, rows[0].amount, 0.001)
        assertEquals("Refund", rows[0].merchant)
    }

    @Test
    fun parses_top_level_array() {
        val raw = """
            [{"date":"04 Jul 2026","amount":99,"type":"dr","merchant":"Uber"}]
        """.trimIndent()
        val rows = StatementJsonParser.parse(raw)
        assertEquals(1, rows.size)
        assertEquals("2026-07-04", rows[0].date)
        assertEquals("expense", rows[0].type)
    }

    @Test
    fun extract_json_handles_braces_inside_strings() {
        val raw = """
            {"transactions":[{"date":"2026-01-01","amount":1,"type":"expense","merchant":"Shop {A}"}]}
        """.trimIndent()
        val extracted = StatementJsonParser.extractJson(raw)
        assertNotNull(extracted)
        val rows = StatementJsonParser.parse(raw)
        assertEquals("Shop {A}", rows[0].merchant)
    }

    @Test
    fun normalize_date_formats() {
        assertEquals("2026-07-04", StatementJsonParser.normalizeDate("2026-07-04T12:00:00"))
        assertEquals("2026-07-04", StatementJsonParser.normalizeDate("04/07/2026"))
        assertEquals("2026-07-04", StatementJsonParser.normalizeDate("04-07-2026"))
        assertEquals("2026-07-04", StatementJsonParser.normalizeDate("4 Jul 2026"))
        assertNull(StatementJsonParser.normalizeDate(""))
        assertNull(StatementJsonParser.normalizeDate("not-a-date"))
        assertNotNull(StatementJsonParser.extractJson("""{"transactions":[]}"""))
    }

    @Test
    fun empty_or_invalid_returns_empty() {
        assertTrue(StatementJsonParser.parse("not json").isEmpty())
        assertTrue(StatementJsonParser.parse("").isEmpty())
        // Truncated JSON (as when model hits max tokens) must not crash
        assertTrue(StatementJsonParser.parse("""{"transactions":[{"date":"2026-""").isEmpty())
    }

    @Test
    fun rejects_non_positive_amounts() {
        val raw = """
            {"transactions":[{"date":"2026-01-01","amount":0,"type":"expense","merchant":"X"}]}
        """.trimIndent()
        assertTrue(StatementJsonParser.parse(raw).isEmpty())
    }
}
