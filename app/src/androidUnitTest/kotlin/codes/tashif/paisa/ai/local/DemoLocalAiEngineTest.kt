package codes.tashif.paisa.ai.local

import codes.tashif.paisa.ai.json.JSONObject
import codes.tashif.paisa.ai.provider.LlmRequest
import codes.tashif.paisa.ai.QuickAddService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DemoLocalAiEngineTest {

    @Test
    fun quickAddReplyIsValidJson() = runTest {
        val prompt = "Today is Saturday, 2026-11-21. The user's currency is INR.\n" +
            "Expense categories: Food & Dining, Transport\n" +
            "Income categories: Salary\n" +
            "Accounts:\n- HDFC Savings (Bank Account)\n\n" +
            "What the user wrote:\nLunch 240 at Chaayos yesterday"
        val reply = DemoLocalAiEngine().complete(
            LlmRequest(system = QuickAddService.SYSTEM_PROMPT, prompt = prompt, json = true)
        )
        val obj = try {
            JSONObject(reply)
        } catch (e: Exception) {
            throw AssertionError("Not JSON: $reply", e)
        }
        val array = obj.getJSONArray("transactions")
        assertEquals(1, array.length())
        val tx = array.getJSONObject(0)
        assertEquals(240.0, tx.getDouble("amount"), 0.001)
        assertEquals("2026-11-21", tx.getString("date"))
        assertEquals("Chaayos", tx.getString("title"))
    }
}
