package codes.tashif.paisa.ai.provider

import codes.tashif.paisa.ai.LlmPdf
import codes.tashif.paisa.ai.catalog.ModelInfo
import codes.tashif.paisa.ai.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnthropicStyleTest {

    private val anthropic = BuiltInProviders.byId(BuiltInProviders.ANTHROPIC)!!
    private val compatible = CustomProvider("custom-kimi", "Kimi", ApiStyle.ANTHROPIC, "https://api.kimi.example/anthropic/v1").toInfo()
    private val request = LlmRequest(system = "sys", prompt = "hi", json = true)

    private fun body(level: ThinkingLevel?, provider: AiProviderInfo = anthropic, info: ModelInfo? = null): JSONObject =
        AnthropicStyle.build(
            LlmCall(provider, provider.defaultBaseUrl, "k", "claude-opus-5-5", thinking = level, modelInfo = info),
            request
        ).body!!

    @Test
    fun auto_sends_no_thinking_and_keeps_temperature() {
        val b = body(null)
        assertFalse(b.has("thinking"))
        assertFalse(b.has("output_config"))
        assertEquals(0.1, b.getDouble("temperature"), 0.0001)
        assertEquals(AnthropicStyle.MAX_TOKENS, b.getInt("max_tokens"))
    }

    @Test
    fun none_disables_thinking() {
        val b = body(ThinkingLevel.NONE)
        assertEquals("disabled", b.getJSONObject("thinking").getString("type"))
        assertFalse(b.has("output_config"))
        assertTrue(b.has("temperature"))
    }

    @Test
    fun every_other_level_is_adaptive_with_effort_and_no_budget() {
        val expected = mapOf(
            ThinkingLevel.MINIMAL to "low",
            ThinkingLevel.LOW to "low",
            ThinkingLevel.MEDIUM to "medium",
            ThinkingLevel.HIGH to "high",
            ThinkingLevel.XHIGH to "xhigh",
            ThinkingLevel.MAX to "max",
        )
        for ((level, effort) in expected) {
            for (provider in listOf(anthropic, compatible)) {
                val b = body(level, provider)
                val thinking = b.getJSONObject("thinking")
                assertEquals("adaptive", thinking.getString("type"))
                assertFalse("budget_tokens is never sent", thinking.has("budget_tokens"))
                assertEquals(effort, b.getJSONObject("output_config").getString("effort"))
                assertFalse("temperature is dropped while thinking", b.has("temperature"))
            }
        }
    }

    @Test
    fun max_tokens_grows_with_thinking_but_stays_within_the_model_limit() {
        assertEquals(AnthropicStyle.MAX_TOKENS, AnthropicStyle.maxTokens(null, null))
        assertEquals(AnthropicStyle.MAX_TOKENS, AnthropicStyle.maxTokens(ThinkingLevel.NONE, null))
        assertEquals(AnthropicStyle.THINKING_MAX_TOKENS, AnthropicStyle.maxTokens(ThinkingLevel.HIGH, null))
        assertTrue(AnthropicStyle.THINKING_MAX_TOKENS > AnthropicStyle.MAX_TOKENS)
        assertEquals(16_000, AnthropicStyle.maxTokens(ThinkingLevel.HIGH, 16_000))
        assertEquals(4_096, AnthropicStyle.maxTokens(null, 4_096))
        val b = body(ThinkingLevel.MAX, info = ModelInfo("claude-opus-5-5", outputLimit = 20_000))
        assertEquals(20_000, b.getInt("max_tokens"))
    }

    @Test
    fun effort_merges_into_an_existing_output_config() {
        val b = JSONObject().put("output_config", JSONObject().put("format", JSONObject().put("type", "json_schema")))
        AnthropicStyle.mergeOutputConfig(b, JSONObject().put("effort", "high"))
        val config = b.getJSONObject("output_config")
        assertEquals("high", config.getString("effort"))
        assertEquals("json_schema", config.getJSONObject("format").getString("type"))
    }

    @Test
    fun urls_pdf_and_parse() {
        val spec = AnthropicStyle.build(
            LlmCall(compatible, compatible.defaultBaseUrl, "k", "m"),
            request.copy(pdf = LlmPdf("JVBER"))
        )
        assertEquals("https://api.kimi.example/anthropic/v1/messages", spec.url)
        assertEquals("k", spec.headers["x-api-key"])
        val content = spec.body!!.getJSONArray("messages").getJSONObject(0).getJSONArray("content")
        assertEquals("document", content.getJSONObject(0).getString("type"))
        assertEquals("https://api.anthropic.com/v1/messages", AnthropicStyle.messagesUrl("https://api.anthropic.com"))

        val reply = """{"content":[{"type":"thinking","thinking":"hmm"},{"type":"text","text":"{\"a\":1}"}],"stop_reason":"end_turn"}"""
        assertEquals("{\"a\":1}", AnthropicStyle.parse(reply))
    }
}
