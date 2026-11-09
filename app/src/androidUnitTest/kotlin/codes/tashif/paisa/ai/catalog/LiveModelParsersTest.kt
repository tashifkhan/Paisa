package codes.tashif.paisa.ai.catalog

import codes.tashif.paisa.ai.provider.RouteMode
import codes.tashif.paisa.ai.provider.ThinkingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveModelParsersTest {

    @Test
    fun openai_anthropic_and_gemini_listings() {
        assertEquals(listOf("gpt-4o", "gpt-5"), LiveModelParsers.openAi("""{"data":[{"id":"gpt-4o"},{"id":"gpt-5"}]}""").map { it.id })

        val (claude, next) = LiveModelParsers.anthropic(
            """{"data":[{"id":"claude-opus-5-5","display_name":"Claude Opus 5.5"}],"has_more":true,"last_id":"claude-opus-5-5"}"""
        )
        assertEquals("Claude Opus 5.5", claude.single().name)
        assertEquals("claude-opus-5-5", next)

        val (gemini, token) = LiveModelParsers.gemini(
            """
            {"models":[
              {"name":"models/gemini-2.5-flash","displayName":"Gemini 2.5 Flash","supportedGenerationMethods":["generateContent"],"inputTokenLimit":1048576,"outputTokenLimit":65536,"thinking":true},
              {"name":"models/text-embedding-004","supportedGenerationMethods":["embedContent"]}
            ]}
            """.trimIndent()
        )
        assertEquals("gemini-2.5-flash", gemini.single().id)
        assertEquals(true, gemini.single().reasoning)
        assertNull(token)
    }

    @Test
    fun relayroom_providers_catalog_and_aliases() {
        val providers = LiveModelParsers.relayProviders(
            """{"object":"list","data":[
              {"id":"google","configured":true,"live_count":4,"modes":["auto","api"]},
              {"id":"xai","configured":true,"live_count":0},
              {"id":"meta","configured":false,"live_count":3}
            ]}"""
        )
        assertEquals(listOf("google"), providers.map { it.first })
        assertEquals(listOf(RouteMode.AUTO, RouteMode.API_KEY), providers.single().second)

        val models = LiveModelParsers.relayCatalog(
            "google",
            """{"object":"list","provider":"google","data":[
              {"id":"gemini-2.5-flash","name":"Gemini 2.5 Flash","provider_name":"Google","available":true,
               "capabilities":{"input":["text","image","pdf"],"reasoning":true},"limit":{"context":1048576},"modes":["auto","sub"]},
              {"id":"gemini-old","available":false}
            ]}""",
            providerModes = listOf(RouteMode.AUTO)
        )
        val flash = models.single()
        assertEquals("google/gemini-2.5-flash", flash.id)
        assertEquals(true, flash.acceptsPdf)
        assertEquals(listOf(RouteMode.AUTO, RouteMode.SUBSCRIPTION), flash.routeModes)
        assertEquals("Google", flash.upstream)

        val aliases = LiveModelParsers.relayAliases(
            """{"data":[{"id":"opus","owned_by":"alias"},{"id":"google/gemini-2.5-flash","owned_by":"google"}]}"""
        )
        assertEquals("opus", aliases.single().id)
        assertTrue(aliases.single().alias)
    }

    @Test
    fun relayroom_model_options() {
        val options = LiveModelParsers.relayModelOptions(
            """{"object":"model.options","provider":"google","model":"gemini-3.6-flash","available":true,
               "input_types":["text","image","pdf"],
               "reasoning":{"supported":true,"levels":["minimal","low","medium","high","weird"],"default":"medium"},
               "temperature":{"supported":true},"top_p":{"supported":true}}"""
        )
        assertTrue(options.available)
        assertTrue(options.reasoningSupported)
        assertEquals(listOf(ThinkingLevel.MINIMAL, ThinkingLevel.LOW, ThinkingLevel.MEDIUM, ThinkingLevel.HIGH), options.levels)
        assertTrue("pdf" in options.input)

        val none = LiveModelParsers.relayModelOptions("""{"available":false,"reasoning":{"supported":false}}""")
        assertFalse(none.available)
        assertTrue(none.levels.isEmpty())
    }

    @Test
    fun relayBudgetRouteOffersNoneToHigh() {
        // What RelayRoom returns for google/gemini-2.5-flash: no named levels, a 0..24576 budget.
        val text = """
            {"available":true,"mode":"api","input_types":["text","image","pdf"],
             "reasoning":{"supported":true,"toggle":true,"levels":[],"provider_mode":"budget",
                          "budget_tokens":{"min":0,"max":24576}}}
        """.trimIndent()
        val options = LiveModelParsers.relayModelOptions(text)
        assertTrue(options.budget)
        assertTrue(options.canTurnOff)
        val model = RelayOptions.apply(ModelInfo("google/gemini-2.5-flash"), options)
        assertEquals(
            listOf(ThinkingLevel.NONE, ThinkingLevel.LOW, ThinkingLevel.MEDIUM, ThinkingLevel.HIGH),
            model.thinkingChoices(codes.tashif.paisa.ai.provider.ApiStyle.RELAYROOM)
        )
        // The cache keeps the budget details across restarts.
        val key = RelayOptionsKey.of("http://10.0.2.2:8317/v1", "google/gemini-2.5-flash", RouteMode.API_KEY)
        assertEquals(options, RelayOptions.decode(RelayOptions.encode(mapOf(key to options)))[key])
    }
}
