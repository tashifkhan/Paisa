package codes.tashif.paisa.ai.ladder

import codes.tashif.paisa.ai.LlmException
import codes.tashif.paisa.ai.LlmFailure
import codes.tashif.paisa.ai.LlmImage
import codes.tashif.paisa.ai.LlmPdf
import codes.tashif.paisa.ai.catalog.ModelInfo
import codes.tashif.paisa.ai.provider.AiProviderInfo
import codes.tashif.paisa.ai.provider.BuiltInProviders
import codes.tashif.paisa.ai.provider.LlmCall
import codes.tashif.paisa.ai.provider.LlmRequest
import codes.tashif.paisa.ai.provider.ModelCaller
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LadderRunnerTest {

    private val openai = BuiltInProviders.byId(BuiltInProviders.OPENAI)!!
    private val google = BuiltInProviders.byId(BuiltInProviders.GOOGLE)!!
    private val relay = BuiltInProviders.byId(BuiltInProviders.RELAYROOM)!!
    private val providers = listOf(openai, google, relay).associateBy { it.id }

    private val jsonRequest = LlmRequest("sys", "prompt", json = true)

    /** Keys exist for every provider except those in [noKey]; [info] gives catalogue entries. */
    private fun resolver(noKey: Set<String> = emptySet(), info: Map<String, ModelInfo> = emptyMap()): (Rung) -> ResolvedRung = { rung ->
        val provider: AiProviderInfo = providers.getValue(rung.providerId)
        if (rung.providerId in noKey) {
            ResolvedRung.Skip(rung.modelId, "no key saved for ${provider.name}")
        } else {
            ResolvedRung.Ready(
                LlmCall(provider, provider.defaultBaseUrl, "k", rung.modelId, rung.thinking, rung.routeMode, info[rung.modelId]),
                rung.modelId
            )
        }
    }

    /** Answers by model id and records the order of calls. */
    private class Script(private val replies: Map<String, () -> String>) : ModelCaller {
        val calls = mutableListOf<String>()
        override suspend fun complete(call: LlmCall, request: LlmRequest): String {
            calls += call.model
            return replies.getValue(call.model).invoke()
        }
    }

    @Test
    fun first_good_rung_wins_and_later_rungs_never_run() = runTest {
        val script = Script(mapOf("a" to { """{"transactions":[]}""" }, "b" to { fail(); "" }))
        val result = LadderRunner.run(
            "Statement import",
            listOf(Rung(openai.id, "a"), Rung(google.id, "b")),
            jsonRequest, resolver(), script
        )
        assertEquals("a", result.label)
        assertEquals(listOf("a"), script.calls)
    }

    @Test
    fun moves_on_after_http_error_empty_reply_and_bad_json() = runTest {
        val script = Script(
            mapOf(
                "http" to { throw LlmException("HTTP 500: down", LlmFailure.HTTP, 500) },
                "timeout" to { throw LlmException("timed out", LlmFailure.TIMEOUT) },
                "empty" to { "  " },
                "prose" to { "Sure! Here are your transactions." },
                "good" to { "```json\n{\"transactions\":[]}\n```" },
            )
        )
        val rungs = listOf("http", "timeout", "empty", "prose", "good").map { Rung(openai.id, it) }
        val result = LadderRunner.run("Statement import", rungs, jsonRequest, resolver(), script)
        assertEquals("good", result.label)
        assertEquals(listOf("http", "timeout", "empty", "prose", "good"), script.calls)
        assertEquals(
            listOf("HTTP 500: down", "timed out", "empty reply", "reply wasn't valid JSON"),
            result.failures.map { it.reason }
        )
    }

    @Test
    fun prose_is_fine_when_json_was_not_asked_for() = runTest {
        val script = Script(mapOf("a" to { "OK" }))
        val result = LadderRunner.run("Test", listOf(Rung(openai.id, "a")), jsonRequest.copy(json = false), resolver(), script)
        assertEquals("OK", result.text)
    }

    @Test
    fun skips_missing_key_and_models_without_image_input() = runTest {
        val script = Script(mapOf("vision" to { "{}" }, "blind" to { fail(); "" }, "nokey" to { fail(); "" }))
        val info = mapOf(
            "blind" to ModelInfo("blind", input = setOf("text")),
            "vision" to ModelInfo("vision", input = setOf("text", "image")),
        )
        val rungs = listOf(Rung(google.id, "nokey"), Rung(openai.id, "blind"), Rung(openai.id, "vision"))
        val result = LadderRunner.run(
            "Statement photos", rungs,
            jsonRequest.copy(image = LlmImage("image/png", "x")),
            resolver(noKey = setOf(google.id), info = info), script
        )
        assertEquals("vision", result.label)
        assertEquals(listOf("vision"), script.calls)
        assertEquals(listOf(true, true), result.failures.map { it.skipped })
        assertEquals("doesn't read images", result.failures[1].reason)
    }

    @Test
    fun unknown_image_support_gets_a_try_but_pdf_needs_a_known_reader() = runTest {
        val script = Script(mapOf("unknown" to { "{}" }, "relaypdf" to { "{}" }))
        val image = LadderRunner.run(
            "Statement photos", listOf(Rung(openai.id, "unknown")),
            jsonRequest.copy(image = LlmImage("image/png", "x")), resolver(), script
        )
        assertEquals("unknown", image.label)

        // OpenAI-style can't carry PDFs at all; RelayRoom can when the model reads them.
        val pdfInfo = mapOf("relaypdf" to ModelInfo("relaypdf", input = setOf("text", "pdf")))
        val pdf = LadderRunner.run(
            "Statement import", listOf(Rung(openai.id, "unknown"), Rung(relay.id, "relaypdf")),
            jsonRequest.copy(pdf = LlmPdf("x")), resolver(info = pdfInfo), script
        )
        assertEquals("relaypdf", pdf.label)
        assertEquals("doesn't read PDFs", pdf.failures.single().reason)
    }

    @Test
    fun all_failing_lists_every_rung() = runTest {
        val script = Script(mapOf("a" to { throw LlmException("HTTP 429: slow down", LlmFailure.HTTP, 429) }, "b" to { "nope" }))
        try {
            LadderRunner.run(
                "Statement import",
                listOf(Rung(openai.id, "a"), Rung(openai.id, "b"), Rung(google.id, "c")),
                jsonRequest, resolver(noKey = setOf(google.id)), script
            )
            fail("expected failure")
        } catch (e: AiLadderException) {
            assertEquals(3, e.failures.size)
            val message = e.message!!
            assertTrue(message, message.startsWith("Every model in the Statement import ladder failed."))
            assertTrue(message, message.contains("a: HTTP 429: slow down"))
            assertTrue(message, message.contains("b: reply wasn't valid JSON"))
            assertTrue(message, message.contains("c: no key saved for Google Gemini"))
        }
    }

    @Test
    fun empty_ladder_says_to_add_a_model() = runTest {
        try {
            LadderRunner.run("Statement import", emptyList(), jsonRequest, resolver(), Script(emptyMap()))
            fail("expected failure")
        } catch (e: AiLadderException) {
            assertEquals("No models set up for Statement import. Add one under AI.", e.message)
        }
    }

    @Test
    fun json_reply_detection() {
        assertTrue(JsonReply.parses("""{"a":1}"""))
        assertTrue(JsonReply.parses("[1,2]"))
        assertTrue(JsonReply.parses("Here you go:\n{\"a\":1}\nThanks"))
        assertTrue(JsonReply.parses("```json\n{\"a\":1}\n```"))
        assertTrue(!JsonReply.parses("{\"a\":"))
        assertTrue(!JsonReply.parses("no json here"))
    }
}
