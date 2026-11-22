package sh.taf.billi.ui.ai

import sh.taf.billi.ai.CatalogState
import sh.taf.billi.ai.ProviderStatus
import sh.taf.billi.ai.catalog.ModelInfo
import sh.taf.billi.ai.ladder.AiService
import sh.taf.billi.ai.ladder.LadderState
import sh.taf.billi.ai.ladder.Rung
import sh.taf.billi.ai.ladder.ServiceLadder
import sh.taf.billi.ai.provider.BuiltInProviders
import sh.taf.billi.ai.store.AiConfig
import sh.taf.billi.ai.store.ProviderResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiScreensLogicTest {

    private val a = Rung("openai", "a")
    private val b = Rung("openai", "b")
    private val c = Rung("anthropic", "c")

    private fun status(id: String, hasKey: Boolean) = ProviderStatus(
        info = BuiltInProviders.byId(id)!!, hasKey = hasKey, baseUrl = "", modelCount = 0,
        liveFetchedAt = null, liveError = null,
    )

    @Test
    fun moveSwapsNeighboursAndIgnoresEdges() {
        val list = listOf(a, b, c)
        assertEquals(listOf(b, a, c), LadderEdits.move(list, 0, 1))
        assertEquals(listOf(a, c, b), LadderEdits.move(list, 2, -1))
        assertEquals(list, LadderEdits.move(list, 0, -1))
        assertEquals(list, LadderEdits.move(list, 2, 1))
    }

    @Test
    fun removeAndUpdateTouchOnlyTheIndex() {
        assertEquals(listOf(a, c), LadderEdits.remove(listOf(a, b, c), 1))
        assertEquals(listOf(a), LadderEdits.remove(listOf(a), 5))
        val updated = LadderEdits.update(listOf(a, b), 1) { it.copy(modelId = "z") }
        assertEquals(listOf(a, b.copy(modelId = "z")), updated)
    }

    @Test
    fun filterMatchesNameOrIdIgnoringCase() {
        val models = listOf(ModelInfo("gpt-4o", "GPT-4o"), ModelInfo("claude-x", "Claude X"))
        assertEquals(listOf("claude-x"), filterModels(models, " CLAUDE ").map { it.id })
        assertEquals(listOf("gpt-4o"), filterModels(models, "4o").map { it.id })
        assertEquals(2, filterModels(models, "").size)
    }

    @Test
    fun photosPickerHidesOnlyModelsKnownNotToReadImages() {
        val catalog = CatalogState(
            models = mapOf(
                "openai" to listOf(
                    ModelInfo("vision", input = setOf("text", "image")),
                    ModelInfo("text-only", input = setOf("text")),
                    ModelInfo("unknown"),
                ),
                "anthropic" to listOf(ModelInfo("c")),
            )
        )
        val providers = listOf(status("openai", true), status("anthropic", false))
        val photos = pickerGroups(providers, catalog, needsImage = true)
        assertEquals(listOf("openai"), photos.map { it.providerId })
        assertEquals(listOf("vision", "unknown"), photos.single().models.map { it.id })
        assertEquals(3, pickerGroups(providers, catalog, needsImage = false).single().models.size)
    }

    @Test
    fun summaryPicksTheFirstRungWithAKey() {
        val ladders = LadderState(
            default = listOf(c, a),
            services = AiService.entries.map { ServiceLadder(it, null, listOf(c, a)) },
        )
        val providers = listOf(status("openai", true), status("anthropic", false))
        val catalog = CatalogState(models = mapOf("openai" to listOf(ModelInfo("a", "Model A"))))
        val summary = AiSummary.of(ladders, providers, catalog)
        assertTrue(summary.ready)
        assertEquals("Model A", summary.modelName)
        assertEquals("OpenAI", summary.providerName)
        assertFalse(AiSummary.of(ladders, listOf(status("openai", false)), catalog).ready)
    }

    @Test
    fun readyLocalProviderCountsWithoutAKey() {
        val local = ProviderStatus(
            info = BuiltInProviders.byId(BuiltInProviders.LOCAL)!!,
            hasKey = false,
            baseUrl = "",
            modelCount = 1,
            liveFetchedAt = null,
            liveError = null,
            local = sh.taf.billi.ai.local.LocalAiStatus(
                state = sh.taf.billi.ai.local.LocalAiState.AVAILABLE,
                modelName = "nano-v4",
                features = setOf(sh.taf.billi.ai.local.LocalAiFeature.TEXT),
            ),
        )
        val rung = Rung(BuiltInProviders.LOCAL, sh.taf.billi.ai.local.LOCAL_AI_MODEL_ID)
        val ladders = LadderState(
            default = listOf(rung),
            services = AiService.entries.map { ServiceLadder(it, null, listOf(rung)) },
        )
        val catalog = CatalogState(
            models = mapOf(
                BuiltInProviders.LOCAL to listOf(
                    sh.taf.billi.ai.local.LocalAiModels.info(local.local!!)
                )
            )
        )
        val summary = AiSummary.of(ladders, listOf(local), catalog)
        assertTrue(summary.ready)
        assertEquals("On-device", summary.providerName)
        assertEquals(listOf(BuiltInProviders.LOCAL), pickerGroups(listOf(local), catalog, needsImage = true).map { it.providerId })
        assertFalse(pickerGroups(listOf(local.copy(local = null)), catalog, needsImage = false).isNotEmpty())
    }

    @Test
    fun ageLabels() {
        val now = 10L * 24 * 60 * 60_000
        assertEquals("just now", ageLabel(now - 10_000, now))
        assertEquals("5 min ago", ageLabel(now - 5 * 60_000, now))
        assertEquals("3 h ago", ageLabel(now - 3 * 60 * 60_000, now))
        assertEquals("2 days ago", ageLabel(now - 2 * 24 * 60 * 60_000, now))
    }

    @Test
    fun openCodeKeepsItsOwnUrlOverModelsDev() {
        val opencode = BuiltInProviders.byId(BuiltInProviders.OPENCODE)!!
        assertEquals(
            "https://opencode.ai/zen/v1",
            ProviderResolution.baseUrl(opencode, AiConfig(), modelsDevApi = "https://opencode.ai/elsewhere/v1"),
        )
        val groq = BuiltInProviders.byId(BuiltInProviders.GROQ)!!
        assertEquals("https://moved.example/v1", ProviderResolution.baseUrl(groq, AiConfig(), "https://moved.example/v1/"))
    }
}
