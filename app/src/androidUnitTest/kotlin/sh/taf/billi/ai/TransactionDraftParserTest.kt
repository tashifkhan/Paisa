package sh.taf.billi.ai

import sh.taf.billi.ui.quickadd.AiAddViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionDraftParserTest {

    private val today = "2026-11-10"

    @Test
    fun reads_a_fenced_reply_with_several_payments() {
        val reply = """
            Here you go:
            ```json
            {"transactions":[
              {"amount":240,"type":"expense","title":"Chaayos","note":"lunch","date":"2026-11-09","time":"13:05","category":"Food","account":"HDFC Savings"},
              {"amount":"₹5,000.50","type":"income","title":"Ravi","date":"10/11/2026"}
            ]}
            ```
        """.trimIndent()
        val drafts = TransactionDraftParser.parse(reply, today)
        assertEquals(2, drafts.size)
        with(drafts[0]) {
            assertEquals(240.0, amount, 0.0)
            assertEquals("expense", type)
            assertEquals("Chaayos", title)
            assertEquals("lunch", note)
            assertEquals("2026-11-09", date)
            assertEquals("13:05", time)
            assertEquals("Food", categoryName)
            assertEquals("HDFC Savings", accountName)
        }
        with(drafts[1]) {
            assertEquals(5000.5, amount, 0.001)
            assertEquals("income", type)
            assertEquals("2026-11-10", date)
            assertNull(time)
            assertNull(categoryName)
        }
    }

    @Test
    fun fills_gaps_and_drops_rows_without_an_amount() {
        val reply = """[
            {"amount":0,"title":"Nothing"},
            {"title":"No amount"},
            {"amount":-180,"merchant":"Uber","time":"25:99"}
        ]"""
        val drafts = TransactionDraftParser.parse(reply, today)
        assertEquals(1, drafts.size)
        with(drafts.single()) {
            assertEquals(180.0, amount, 0.0)
            assertEquals("Uber", title)
            assertEquals("expense", type)
            assertEquals(today, date)
            assertNull(time)
        }
    }

    @Test
    fun empty_or_broken_replies_give_nothing() {
        assertTrue(TransactionDraftParser.parse("""{"transactions":[]}""", today).isEmpty())
        assertTrue(TransactionDraftParser.parse("sorry, I can't read that", today).isEmpty())
        assertTrue(TransactionDraftParser.parse("""{"transactions":[{"amount":""", today).isEmpty())
    }

    @Test
    fun drafts_survive_a_round_trip_for_refining() {
        val drafts = listOf(
            TransactionDraft(240.0, "expense", "Chaayos", "lunch", "2026-11-09", "13:05", "Food", "Cash"),
            TransactionDraft(99.5, "income", "Refund", date = "2026-11-10")
        )
        assertEquals(drafts, TransactionDraftParser.parse(TransactionDraftParser.toJson(drafts), today))
    }

    @Test
    fun amounts_show_without_needless_decimals() {
        assertEquals("240", AiAddViewModel.formatAmount(240.0))
        assertEquals("99.50", AiAddViewModel.formatAmount(99.5))
        assertEquals("1234.57", AiAddViewModel.formatAmount(1234.567))
    }
}

class QuickAddChunkMergeTest {
    private fun d(title: String, amount: Double, date: String = "2026-07-04") =
        TransactionDraft(amount = amount, type = "expense", title = title, date = date)

    @Test
    fun a_row_seen_at_the_seam_of_two_parts_is_kept_once() {
        val first = listOf(d("Swiggy", 240.0), d("Uber", 180.0))
        val second = listOf(d("uber", 180.0), d("Rent", 18000.0))
        val merged = QuickAddService.mergeChunks(listOf(first, second))
        org.junit.Assert.assertEquals(listOf("Swiggy", "Uber", "Rent"), merged.map { it.title })
    }

    @Test
    fun two_real_payments_that_match_inside_one_part_both_stay() {
        val part = listOf(d("Chai", 20.0), d("Chai", 20.0))
        org.junit.Assert.assertEquals(2, QuickAddService.mergeChunks(listOf(part, emptyList())).size)
    }
}
