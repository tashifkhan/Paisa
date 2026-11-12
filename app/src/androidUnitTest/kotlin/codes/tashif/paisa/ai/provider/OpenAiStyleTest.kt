package codes.tashif.paisa.ai.provider

import codes.tashif.paisa.ai.LlmAudio
import codes.tashif.paisa.ai.LlmException
import codes.tashif.paisa.ai.LlmFailure
import codes.tashif.paisa.ai.LlmImage
import codes.tashif.paisa.ai.catalog.ModelInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class OpenAiStyleTest {

    private val openAi = BuiltInProviders.byId(BuiltInProviders.OPENAI)!!
    private val groq = BuiltInProviders.byId(BuiltInProviders.GROQ)!!
    private val request = LlmRequest(system = "sys", prompt = "hi", json = true)

    private fun call(provider: AiProviderInfo, model: String, level: ThinkingLevel?, info: ModelInfo? = null) =
        LlmCall(provider, provider.defaultBaseUrl, "k", model, thinking = level, modelInfo = info)

    @Test
    fun every_level_maps_to_reasoning_effort() {
        for (level in ThinkingLevel.entries) {
            val compatible = OpenAiStyle.build(call(groq, "openai/gpt-oss-20b", level), request).body!!
            assertEquals(level.wire, compatible.getString("reasoning_effort"))
            assertFalse(compatible.has("temperature"))

            val native = OpenAiStyle.build(call(openAi, "gpt-5.6", level), request).body!!
            val expected = if (level == ThinkingLevel.MAX) "xhigh" else level.wire
            assertEquals(expected, native.getString("reasoning_effort"))
            assertFalse(native.has("temperature"))
        }
    }

    @Test
    fun max_follows_the_levels_the_catalogue_lists() {
        fun effort(levels: List<ThinkingLevel>?) = OpenAiStyle.build(
            call(openAi, "gpt-5.6", ThinkingLevel.MAX, ModelInfo("gpt-5.6", reasoning = true, reasoningLevels = levels)),
            request
        ).body!!.getString("reasoning_effort")

        assertEquals("max", effort(listOf(ThinkingLevel.LOW, ThinkingLevel.HIGH, ThinkingLevel.MAX)))
        assertEquals("xhigh", effort(listOf(ThinkingLevel.LOW, ThinkingLevel.XHIGH)))
        assertEquals("high", effort(listOf(ThinkingLevel.NONE, ThinkingLevel.LOW, ThinkingLevel.HIGH)))
        assertEquals("xhigh", effort(null))
        assertEquals("xhigh", effort(emptyList()))
    }

    @Test
    fun auto_sends_no_effort_and_temperature_only_to_non_reasoners() {
        val plain = OpenAiStyle.build(call(openAi, "gpt-4o-mini", null), request).body!!
        assertFalse(plain.has("reasoning_effort"))
        assertEquals(0.1, plain.getDouble("temperature"), 0.0001)

        // The id alone marks gpt-5 and o-series as reasoners on api.openai.com.
        val gpt5 = OpenAiStyle.build(call(openAi, "gpt-5-mini", null), request).body!!
        assertFalse(gpt5.has("temperature"))
        val o3 = OpenAiStyle.build(call(openAi, "o3", null), request).body!!
        assertFalse(o3.has("temperature"))

        // A compatible model the catalogue marks as reasoning gets none either.
        val known = OpenAiStyle.build(
            call(groq, "qwen", null, ModelInfo("qwen", reasoning = true)), request
        ).body!!
        assertFalse(known.has("temperature"))
    }

    @Test
    fun json_mode_and_image_content() {
        val withImage = request.copy(image = LlmImage("image/png", "AAAA"))
        val spec = OpenAiStyle.build(call(openAi, "gpt-4o-mini", null), withImage)
        assertEquals("https://api.openai.com/v1/chat/completions", spec.url)
        assertEquals("Bearer k", spec.headers["Authorization"])
        val body = spec.body!!
        assertEquals("json_object", body.getJSONObject("response_format").getString("type"))
        val content = body.getJSONArray("messages").getJSONObject(1).getJSONArray("content")
        assertEquals("data:image/png;base64,AAAA", content.getJSONObject(1).getJSONObject("image_url").getString("url"))

        val noJson = OpenAiStyle.build(call(openAi, "gpt-4o-mini", null), request, jsonMode = false).body!!
        assertFalse(noJson.has("response_format"))
    }

    @Test
    fun parse_reads_content_and_flags_cut_off() {
        assertEquals("{}", OpenAiStyle.parse("""{"choices":[{"message":{"content":"{}"},"finish_reason":"stop"}]}"""))
        try {
            OpenAiStyle.parse("""{"choices":[{"message":{"content":"{"},"finish_reason":"length"}]}""")
            fail("expected cut off")
        } catch (e: LlmException) {
            assertEquals(LlmFailure.CUT_OFF, e.failure)
        }
        assertTrue(OpenAiStyle.rejectsJsonMode(400, "response_format is not supported"))
    }

    @Test
    fun voice_note_is_an_input_audio_part() {
        val content = OpenAiStyle.build(
            call(openAi, "gpt-4o-audio-preview", null),
            request.copy(audio = LlmAudio("audio/wav", "UklGRg==", "wav"))
        ).body!!.getJSONArray("messages").getJSONObject(1).getJSONArray("content")
        assertEquals(2, content.length())
        assertEquals("hi", content.getJSONObject(0).getString("text"))
        val part = content.getJSONObject(1)
        assertEquals("input_audio", part.getString("type"))
        assertEquals("UklGRg==", part.getJSONObject("input_audio").getString("data"))
        assertEquals("wav", part.getJSONObject("input_audio").getString("format"))
    }
}
