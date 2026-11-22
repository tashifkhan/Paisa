package sh.taf.billi.ai.local

import sh.taf.billi.ai.ladder.Rung
import sh.taf.billi.ai.provider.BuiltInProviders
import sh.taf.billi.ai.store.AiConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAiLaddersTest {

    private val cloud = Rung(BuiltInProviders.OPENAI, "gpt-4o-mini")
    private val other = Rung(BuiltInProviders.GOOGLE, "gemini-2.5-flash")
    private val local = LocalAiLadders.rung

    @Test
    fun enablingPutsLocalFirstOnce() {
        val before = AiConfig(defaultLadder = listOf(cloud, other))
        val once = LocalAiLadders.enable(before)
        assertEquals(listOf(local, cloud, other), once.defaultLadder)
        assertEquals(once, LocalAiLadders.enable(once))
        assertTrue(LocalAiLadders.uses(once))
    }

    @Test
    fun enablingMovesAnExistingLocalRungToTheTop() {
        val before = AiConfig(defaultLadder = listOf(cloud, local, other))
        assertEquals(listOf(local, cloud, other), LocalAiLadders.enable(before).defaultLadder)
    }

    @Test
    fun disablingRemovesLocalFromDefaultAndServiceLadders() {
        val before = AiConfig(
            defaultLadder = listOf(local, cloud),
            serviceLadders = mapOf(
                AiServiceWithLocal to listOf(local, other),
                AiServiceWithLocalOnly to listOf(local),
            ),
        )
        val after = LocalAiLadders.disable(before)
        assertEquals(listOf(cloud), after.defaultLadder)
        assertEquals(mapOf(AiServiceWithLocal to listOf(other)), after.serviceLadders)
        assertFalse(LocalAiLadders.uses(after))
    }

    @Test
    fun emptyConfigHasNoLocalUse() {
        assertFalse(LocalAiLadders.uses(AiConfig()))
    }

    @Test
    fun modelEntryTakesTextAndImages() {
        val info = LocalAiModels.info(
            LocalAiStatus(
                state = LocalAiState.AVAILABLE,
                modelName = "nano-v4",
                features = setOf(LocalAiFeature.TEXT, LocalAiFeature.IMAGE),
                maxInputTokens = 4_000,
            )
        )
        assertEquals(LOCAL_AI_MODEL_ID, info.id)
        assertEquals("Gemini Nano · nano-v4", info.name)
        assertEquals(true, info.acceptsImage)
        assertEquals(false, info.acceptsPdf)
        assertEquals(false, info.acceptsAudio)
        assertEquals(4_000, info.contextWindow)
    }

    @Test
    fun skipReasonsAreReadable() {
        assertEquals("Gemini Nano isn't downloaded", LocalAiStatus(LocalAiState.DOWNLOADABLE).skipReason)
        assertEquals(null, LocalAiStatus(LocalAiState.AVAILABLE).skipReason)
        assertEquals("no AICore", LocalAiStatus(LocalAiState.UNSUPPORTED, detail = "no AICore").skipReason)
    }

    private companion object {
        val AiServiceWithLocal = sh.taf.billi.ai.ladder.AiService.STATEMENT_IMPORT
        val AiServiceWithLocalOnly = sh.taf.billi.ai.ladder.AiService.QUICK_ADD
    }
}
