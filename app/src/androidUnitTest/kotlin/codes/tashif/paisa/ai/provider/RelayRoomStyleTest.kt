package codes.tashif.paisa.ai.provider

import codes.tashif.paisa.ai.LlmAudio
import codes.tashif.paisa.ai.LlmImage
import codes.tashif.paisa.ai.LlmPdf
import codes.tashif.paisa.ai.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelayRoomStyleTest {

    private val relay = BuiltInProviders.byId(BuiltInProviders.RELAYROOM)!!
    private val request = LlmRequest(system = "sys", prompt = "hi", json = true, feature = "statement_import")

    private fun call(model: String, level: ThinkingLevel? = null, mode: RouteMode = RouteMode.AUTO) =
        LlmCall(relay, "http://10.0.2.2:8317/v1/", "gk", model, thinking = level, routeMode = mode)

    @Test
    fun generate_body_for_provider_model() {
        val spec = RelayRoomStyle.build(call("openrouter/google/gemini-3.6-flash", ThinkingLevel.HIGH, RouteMode.API_KEY), request)
        assertEquals("http://10.0.2.2:8317/v1/generate", spec.url)
        assertEquals("Bearer gk", spec.headers["Authorization"])
        val b = spec.body!!
        assertEquals("openrouter", b.getString("provider"))
        assertEquals("google/gemini-3.6-flash", b.getString("model"))
        assertEquals("api", b.getString("mode"))
        assertEquals("sys", b.getString("system"))
        assertEquals("hi", b.getString("prompt"))
        assertEquals("high", b.getJSONObject("config").getString("reasoning"))
        assertEquals("json", b.getJSONObject("output").getString("format"))
        assertFalse(b.getBoolean("stream"))
        val tags = b.getJSONObject("tags")
        assertEquals("paisa", tags.getString("app"))
        assertEquals("statement_import", tags.getString("feature"))
        assertFalse(b.has("attachments"))
    }

    @Test
    fun alias_has_no_provider_and_auto_mode_is_left_out() {
        val b = RelayRoomStyle.build(call("opus"), request).body!!
        assertFalse(b.has("provider"))
        assertEquals("opus", b.getString("model"))
        assertFalse(b.has("mode"))
        assertFalse(b.has("config"))
    }

    @Test
    fun every_level_goes_into_config_reasoning_verbatim() {
        for (level in ThinkingLevel.entries) {
            val b = RelayRoomStyle.build(call("google/gemini-2.5-flash", level), request).body!!
            assertEquals(level.wire, b.getJSONObject("config").getString("reasoning"))
        }
        assertEquals("sub", RelayRoomStyle.build(call("a/b", mode = RouteMode.SUBSCRIPTION), request).body!!.getString("mode"))
    }

    @Test
    fun image_and_pdf_attachments() {
        val b = RelayRoomStyle.build(
            call("google/gemini-2.5-flash"),
            request.copy(image = LlmImage("image/jpeg", "IMG"), pdf = LlmPdf("PDF", "s.pdf"))
        ).body!!
        val attachments = b.getJSONArray("attachments")
        assertEquals("image", attachments.getJSONObject(0).getString("kind"))
        assertEquals("data:image/jpeg;base64,IMG", attachments.getJSONObject(0).getString("data"))
        assertEquals("pdf", attachments.getJSONObject(1).getString("kind"))
        assertEquals("application/pdf", attachments.getJSONObject(1).getString("mime_type"))
        assertEquals("data:application/pdf;base64,PDF", attachments.getJSONObject(1).getString("data"))
    }

    @Test
    fun parses_json_and_text_output() {
        val json = """{"id":"r","provider":"google","model":"m","output":{"type":"json","json":{"transactions":[]}}}"""
        assertEquals(JSONObject("""{"transactions":[]}""").toString(), RelayRoomStyle.parse(json))
        val text = """{"id":"r","provider":"google","model":"m","output":{"type":"text","text":"OK"}}"""
        assertEquals("OK", RelayRoomStyle.parse(text))
        val nullJson = """{"output":{"type":"text","text":"hi","json":null}}"""
        assertEquals("hi", RelayRoomStyle.parse(nullJson))
    }

    @Test
    fun error_shape_becomes_a_readable_message() {
        val body = """{"error":{"type":"mode_not_supported","message":"vercel has no sub route","status":400}}"""
        val message = RelayRoomStyle.errorMessage(400, body)
        assertTrue(message.startsWith("That provider has no route for the chosen mode"))
        assertTrue(message.contains("vercel has no sub route"))
        assertEquals("plain", RelayRoomStyle.errorMessage(500, """{"error":{"type":"provider_error","message":"plain"}}"""))
        assertTrue(RelayRoomStyle.errorMessage(401, "").contains("gateway key"))
        assertEquals("RelayRoom returned 502", RelayRoomStyle.errorMessage(502, "<html>"))
    }

    private fun config(model: String, level: ThinkingLevel, info: codes.tashif.paisa.ai.catalog.ModelInfo) =
        RelayRoomStyle.build(
            LlmCall(relay, "http://10.0.2.2:8317/v1/", "gk", model, thinking = level, modelInfo = info),
            request
        ).body!!.getJSONObject("config")

    @Test
    fun listed_levels_go_in_reasoning_and_none_turns_thinking_off() {
        // anthropic/claude-haiku-5-5: levels low..max, toggle true.
        val haiku = codes.tashif.paisa.ai.catalog.ModelInfo(
            "anthropic/claude-haiku-5-5", reasoning = true,
            reasoningLevels = listOf(ThinkingLevel.NONE, ThinkingLevel.LOW, ThinkingLevel.MEDIUM,
                ThinkingLevel.HIGH, ThinkingLevel.XHIGH, ThinkingLevel.MAX)
        )
        assertEquals("max", config(haiku.id, ThinkingLevel.MAX, haiku).getString("reasoning"))
        val off = config(haiku.id, ThinkingLevel.NONE, haiku)
        assertFalse(off.getBoolean("reasoning_enabled"))
        assertFalse(off.has("reasoning"))
    }

    @Test
    fun budget_routes_get_a_token_budget_inside_their_bounds() {
        // google/gemini-2.5-flash: no levels, budget 0..24576, toggle true.
        val flash = codes.tashif.paisa.ai.catalog.ModelInfo(
            "google/gemini-2.5-flash", reasoning = true, reasoningLevels = emptyList(),
            budgetThinking = true, budgetMin = 0, budgetMax = 24576
        )
        assertEquals(6144, config(flash.id, ThinkingLevel.LOW, flash).getInt("reasoning_budget_tokens"))
        assertEquals(24576, config(flash.id, ThinkingLevel.MAX, flash).getInt("reasoning_budget_tokens"))
        assertEquals(0, config(flash.id, ThinkingLevel.NONE, flash).getInt("reasoning_budget_tokens"))
        assertFalse(config(flash.id, ThinkingLevel.LOW, flash).has("reasoning"))
    }

    @Test
    fun voice_note_is_an_audio_attachment() {
        val b = RelayRoomStyle.build(
            call("google/gemini-3.8-flash"),
            request.copy(audio = LlmAudio("audio/wav", "UklGRg==", "wav"))
        ).body!!
        val audio = b.getJSONArray("attachments").getJSONObject(0)
        assertEquals("audio", audio.getString("kind"))
        assertEquals("audio/wav", audio.getString("mime_type"))
        assertEquals("data:audio/wav;base64,UklGRg==", audio.getString("data"))
    }
}
