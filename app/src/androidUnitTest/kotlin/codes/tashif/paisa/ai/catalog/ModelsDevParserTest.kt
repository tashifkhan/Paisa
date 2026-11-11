package codes.tashif.paisa.ai.catalog

import codes.tashif.paisa.ai.provider.ApiStyle
import codes.tashif.paisa.ai.provider.BuiltInProviders
import codes.tashif.paisa.ai.provider.ThinkingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsDevParserTest {

    private val fixture = """
        {
          "openai": {
            "id": "openai", "name": "OpenAI",
            "models": {
              "gpt-5-mini": {
                "id": "gpt-5-mini", "name": "GPT-5 mini", "reasoning": true,
                "reasoning_options": [{"type": "effort", "values": ["minimal", "low", "medium", "high"]}],
                "modalities": {"input": ["text", "image"], "output": ["text"]},
                "limit": {"context": 400000, "output": 128000}
              },
              "gpt-4o-mini": {
                "id": "gpt-4o-mini", "name": "GPT-4o mini", "reasoning": false,
                "modalities": {"input": ["text", "image", "pdf"], "output": ["text"]},
                "limit": {"context": 128000, "output": 16384}
              },
              "gpt-image-1": {
                "id": "gpt-image-1", "name": "GPT Image",
                "modalities": {"input": ["text"], "output": ["image"]}
              },
              "o9-future": {"id": "o9-future", "reasoning": true}
            }
          },
          "google": {
            "id": "google", "name": "Google",
            "models": {
              "gemini-2.5-flash": {
                "id": "gemini-2.5-flash", "name": "Gemini 2.5 Flash", "reasoning": true,
                "reasoning_options": [{"type": "toggle"}, {"type": "budget_tokens", "min": 0, "max": 24576}],
                "modalities": {"input": ["text", "image", "pdf"], "output": ["text"]}
              }
            }
          },
          "anthropic": {
            "id": "anthropic",
            "models": {
              "claude-haiku-4-5": {
                "id": "claude-haiku-4-5", "reasoning": true,
                "reasoning_options": [{"type": "budget_tokens", "min": 1024}],
                "modalities": {"input": ["text", "image", "pdf"], "output": ["text"]}
              }
            }
          },
          "opencode": {"id": "opencode", "api": "https://opencode.ai/inference/openai/v1", "models": {}},
          "somecloud": {
            "id": "somecloud",
            "models": {
              "qwen/qwen3-vl": {
                "id": "qwen/qwen3-vl", "reasoning": true, "reasoning_options": [],
                "modalities": {"input": ["text", "image"], "output": ["text"]}
              }
            }
          }
        }
    """.trimIndent()

    private val snapshot = ModelsDevParser.parse(fixture, setOf("openai", "google", "anthropic", "opencode"))

    @Test
    fun keeps_known_providers_and_drops_non_text_models() {
        assertEquals(setOf("openai", "google", "anthropic", "opencode"), snapshot.providers.keys)
        val openai = snapshot.providers.getValue("openai").models.map { it.id }
        assertTrue("gpt-5-mini" in openai)
        assertFalse("gpt-image-1" in openai)
        assertEquals("https://opencode.ai/inference/openai/v1", snapshot.providers.getValue("opencode").api)
        assertNull(snapshot.providers.getValue("openai").api)
    }

    @Test
    fun reads_capabilities_and_levels() {
        val mini = snapshot.providers.getValue("openai").models.first { it.id == "gpt-5-mini" }
        assertEquals(setOf("text", "image"), mini.input)
        assertEquals(true, mini.reasoning)
        assertEquals(listOf(ThinkingLevel.MINIMAL, ThinkingLevel.LOW, ThinkingLevel.MEDIUM, ThinkingLevel.HIGH), mini.reasoningLevels)
        assertEquals(400_000, mini.contextWindow)
        assertEquals(128_000, mini.outputLimit)
        assertEquals(false, mini.acceptsPdf)

        val flash = snapshot.providers.getValue("google").models.single()
        assertEquals(listOf(ThinkingLevel.NONE), flash.reasoningLevels)
        assertTrue(flash.budgetThinking)
        assertEquals(0, flash.budgetMin)
        assertEquals(24_576, flash.budgetMax)
    }

    @Test
    fun thinking_choices_follow_the_catalogue() {
        val openai = snapshot.providers.getValue("openai").models.associateBy { it.id }
        assertEquals(4, openai.getValue("gpt-5-mini").thinkingChoices(ApiStyle.OPENAI).size)
        assertTrue(openai.getValue("gpt-4o-mini").thinkingChoices(ApiStyle.OPENAI).isEmpty())
        // Reasons but lists nothing: Low, Medium, High.
        assertEquals(ThinkingLevel.FALLBACK, openai.getValue("o9-future").thinkingChoices(ApiStyle.OPENAI))
        // A budget becomes Low/Medium/High plus None when it can reach 0.
        val flash = snapshot.providers.getValue("google").models.single()
        assertEquals(
            listOf(ThinkingLevel.NONE, ThinkingLevel.LOW, ThinkingLevel.MEDIUM, ThinkingLevel.HIGH),
            flash.thinkingChoices(ApiStyle.GEMINI)
        )
        // Anthropic never gets budget levels, since Paisa sends adaptive thinking only.
        val haiku = snapshot.providers.getValue("anthropic").models.single()
        assertTrue(haiku.thinkingChoices(ApiStyle.ANTHROPIC).isEmpty())
        // An explicit empty list means thinking can't be tuned.
        assertTrue(snapshot.lookup("qwen/qwen3-vl")!!.thinkingChoices(ApiStyle.OPENAI).isEmpty())
    }

    @Test
    fun index_finds_models_from_any_provider() {
        assertNotNull(snapshot.lookup("qwen/qwen3-vl"))
        assertNotNull(snapshot.lookup("qwen3-vl"))
        assertNotNull(snapshot.lookup("openrouter/qwen/qwen3-vl"))
        assertEquals("gpt-5-mini", snapshot.lookup("openai/gpt-5-mini")!!.id)
    }

    @Test
    fun cache_round_trip() {
        val (decoded, fetchedAt) = CatalogCodec.decodeModelsDev(CatalogCodec.encodeModelsDev(snapshot, 1234L))!!
        assertEquals(1234L, fetchedAt)
        assertEquals(snapshot.providers, decoded.providers)
        assertEquals(snapshot.index.keys, decoded.index.keys)
        assertNull(CatalogCodec.decodeModelsDev("not json"))
    }

    @Test
    fun merge_prefers_live_list_and_keeps_capabilities_and_manual_ids() {
        val openai = BuiltInProviders.byId(BuiltInProviders.OPENAI)!!
        val live = LiveList(
            listOf(ModelInfo("gpt-5-mini", source = ModelSource.LIVE), ModelInfo("ft:custom", source = ModelSource.LIVE)),
            fetchedAt = 1L
        )
        val merged = CatalogMerge.modelsFor(openai, snapshot, live, manual = listOf("gpt-4o-mini", "gpt-5-mini"))
        assertEquals(listOf("gpt-5-mini", "ft:custom", "gpt-4o-mini"), merged.map { it.id })
        assertEquals(true, merged[0].reasoning)
        assertEquals("GPT-5 mini", merged[0].name)
        assertEquals(ModelSource.LIVE, merged[0].source)
        assertNull(merged[1].reasoning)
        assertEquals(ModelSource.MANUAL, merged[2].source)
        assertEquals(true, merged[2].acceptsPdf)

        // No live list: models.dev. Nothing at all: built-ins.
        assertEquals(3, CatalogMerge.modelsFor(openai, snapshot, null, emptyList()).size)
        val anthropic = BuiltInProviders.byId(BuiltInProviders.ANTHROPIC)!!
        assertTrue(CatalogMerge.modelsFor(anthropic, ModelsDevSnapshot.EMPTY, null, emptyList()).isNotEmpty())
    }
}
