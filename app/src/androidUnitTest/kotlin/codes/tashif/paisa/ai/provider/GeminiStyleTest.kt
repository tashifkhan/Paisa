package codes.tashif.paisa.ai.provider

import codes.tashif.paisa.ai.catalog.ModelInfo
import codes.tashif.paisa.ai.provider.GeminiStyle.ThinkingMode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiStyleTest {

    private val google = BuiltInProviders.byId(BuiltInProviders.GOOGLE)!!
    private val request = LlmRequest(system = "sys", prompt = "hi", json = true)

    private fun generation(model: String, level: ThinkingLevel?, info: ModelInfo? = null): JSONObject =
        GeminiStyle.build(
            LlmCall(google, google.defaultBaseUrl, "k", model, thinking = level, modelInfo = info),
            request
        ).body!!.getJSONObject("generationConfig")

    @Test
    fun version_in_the_id_picks_budget_or_level() {
        assertEquals(ThinkingMode.BUDGET, GeminiStyle.thinkingMode("gemini-2.5-flash", null))
        assertEquals(ThinkingMode.BUDGET, GeminiStyle.thinkingMode("models/gemini-2.5-pro", null))
        assertEquals(ThinkingMode.LEVEL, GeminiStyle.thinkingMode("gemini-3.6-flash", null))
        assertEquals(ThinkingMode.LEVEL, GeminiStyle.thinkingMode("gemini-4-argon", null))
        assertEquals(ThinkingMode.LEVEL, GeminiStyle.thinkingMode("gemini-flash-latest", null))
    }

    @Test
    fun catalogue_wins_over_the_id() {
        val effort = ModelInfo("gemini-2.9-x", reasoning = true, reasoningLevels = listOf(ThinkingLevel.LOW, ThinkingLevel.HIGH))
        assertEquals(ThinkingMode.LEVEL, GeminiStyle.thinkingMode("gemini-2.9-x", effort))
        val budget = ModelInfo("gemini-flash-latest", reasoning = true, reasoningLevels = listOf(ThinkingLevel.NONE), budgetThinking = true)
        assertEquals(ThinkingMode.BUDGET, GeminiStyle.thinkingMode("gemini-flash-latest", budget))
    }

    @Test
    fun gemini_2_5_takes_a_budget_for_every_level() {
        val expected = mapOf(
            ThinkingLevel.NONE to 0,
            ThinkingLevel.MINIMAL to 512,
            ThinkingLevel.LOW to 1_024,
            ThinkingLevel.MEDIUM to 8_192,
            ThinkingLevel.HIGH to 24_576,
            ThinkingLevel.XHIGH to 24_576,
            ThinkingLevel.MAX to 24_576,
        )
        for ((level, budget) in expected) {
            val config = generation("gemini-2.5-flash", level).getJSONObject("thinkingConfig")
            assertEquals(level.name, budget, config.getInt("thinkingBudget"))
            assertFalse(config.has("thinkingLevel"))
        }
        // Pro can't switch thinking off and goes up to 32k.
        assertEquals(128, GeminiStyle.thinkingBudget("gemini-2.5-pro", ThinkingLevel.NONE, null))
        assertEquals(32_768, GeminiStyle.thinkingBudget("gemini-2.5-pro", ThinkingLevel.MAX, null))
        // Catalogue bounds clamp the table.
        val lite = ModelInfo("gemini-2.5-flash-lite", budgetThinking = true, budgetMin = 512, budgetMax = 24_576,
            reasoningLevels = listOf(ThinkingLevel.NONE))
        assertEquals(0, GeminiStyle.thinkingBudget("gemini-2.5-flash-lite", ThinkingLevel.NONE, lite))
        assertEquals(512, GeminiStyle.thinkingBudget("gemini-2.5-flash-lite", ThinkingLevel.MINIMAL, lite))
    }

    @Test
    fun gemini_3_takes_a_level_for_every_level() {
        val expected = mapOf(
            ThinkingLevel.NONE to "minimal",
            ThinkingLevel.MINIMAL to "minimal",
            ThinkingLevel.LOW to "low",
            ThinkingLevel.MEDIUM to "medium",
            ThinkingLevel.HIGH to "high",
            ThinkingLevel.XHIGH to "high",
            ThinkingLevel.MAX to "high",
        )
        for ((level, value) in expected) {
            val config = generation("gemini-3.6-flash", level).getJSONObject("thinkingConfig")
            assertEquals(value, config.getString("thinkingLevel"))
            assertFalse(config.has("thinkingBudget"))
        }
    }

    @Test
    fun auto_sends_no_thinking_config_and_temperature_only_before_3() {
        val old = generation("gemini-2.5-flash", null)
        assertFalse(old.has("thinkingConfig"))
        assertEquals(0.1, old.getDouble("temperature"), 0.0001)
        assertEquals("application/json", old.getString("responseMimeType"))
        assertFalse(generation("gemini-3.6-flash", null).has("temperature"))
    }

    @Test
    fun url_header_and_parse_skip_thoughts() {
        val spec = GeminiStyle.build(LlmCall(google, google.defaultBaseUrl, "k", "models/gemini-2.5-flash"), request)
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
            spec.url
        )
        assertEquals("k", spec.headers["x-goog-api-key"])
        assertFalse(spec.url.contains("key="))
        val reply = """{"candidates":[{"content":{"parts":[{"text":"thinking","thought":true},{"text":"{}"}]},"finishReason":"STOP"}]}"""
        assertEquals("{}", GeminiStyle.parse(reply))
        assertTrue(GeminiStyle.majorVersion("gemini-2.5-flash") == 2)
    }
}
