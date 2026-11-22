package sh.taf.billi.ai.local

import sh.taf.billi.ai.LlmImage
import sh.taf.billi.ai.LlmPdf
import sh.taf.billi.ai.catalog.ModelInfo
import sh.taf.billi.ai.ladder.LadderRunner
import sh.taf.billi.ai.ladder.ResolvedRung
import sh.taf.billi.ai.ladder.Rung
import sh.taf.billi.ai.provider.BuiltInProviders
import sh.taf.billi.ai.provider.LlmCall
import sh.taf.billi.ai.provider.LlmRequest
import sh.taf.billi.ai.provider.ModelCaller
import sh.taf.billi.ai.provider.ProviderClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The local provider runs without a key or URL and goes through the real client path. */
class LocalLadderTest {

    private val provider = BuiltInProviders.byId(BuiltInProviders.LOCAL)!!
    private val call = LlmCall(provider, "", "", LOCAL_AI_MODEL_ID)

    private class FakeEngine(
        override val status: MutableStateFlow<LocalAiStatus> = MutableStateFlow(
            LocalAiStatus(LocalAiState.AVAILABLE, modelName = "nano-v4", features = setOf(LocalAiFeature.TEXT))
        )
    ) : LocalAiEngine {
        var seen: LlmRequest? = null
        override suspend fun refresh(): LocalAiStatus = status.value
        override suspend fun download(): LocalAiStatus = status.value
        override suspend fun complete(request: LlmRequest): String {
            seen = request
            return """{"transactions":[]}"""
        }
    }

    @Test
    fun localClientNeedsNoKeyOrUrl() = runTest {
        val engine = FakeEngine()
        val client = ProviderClient(engine)
        val request = LlmRequest("system", "lunch 240", json = true)
        assertEquals("""{"transactions":[]}""", client.complete(call, request))
        assertEquals(request, engine.seen)
        assertTrue(call.apiKey.isEmpty())
        assertTrue(call.baseUrl.isEmpty())
    }

    @Test
    fun ladderRunsLocalRungWhenEngineIsReady() = runTest {
        val engine = FakeEngine()
        val info = LocalAiModels.info(engine.status.value)
        val resolve: (Rung) -> ResolvedRung = {
            ResolvedRung.Ready(call.copy(modelInfo = info), "Gemini Nano (On-device)")
        }
        val result = LadderRunner.run(
            "Add with AI",
            listOf(Rung(provider.id, LOCAL_AI_MODEL_ID)),
            LlmRequest("system", "prompt", json = true),
            resolve,
            ModelCaller { _, request -> engine.complete(request) },
        )
        assertEquals("""{"transactions":[]}""", result.text)
    }

    @Test
    fun localStyleCannotCarryPdfOrAudio() {
        val info = ModelInfo(LOCAL_AI_MODEL_ID, input = setOf("text", "image"))
        val pdf = LlmRequest("s", "p", pdf = LlmPdf("x"))
        val audio = LlmRequest("s", "p", audio = sh.taf.billi.ai.LlmAudio("audio/wav", "", "wav"))
        val image = LlmRequest("s", "p", image = LlmImage("image/png", "x"))
        assertEquals("doesn't read PDFs", LadderRunner.inputProblem(call.copy(modelInfo = info), pdf))
        assertEquals("doesn't take audio", LadderRunner.inputProblem(call.copy(modelInfo = info), audio))
        assertNull(LadderRunner.inputProblem(call.copy(modelInfo = info), image))
    }

    @Test
    fun unavailableStatusSkipsWithAReason() {
        val status = LocalAiStatus(LocalAiState.DOWNLOADABLE)
        assertEquals("Gemini Nano isn't downloaded", status.skipReason)
    }
}
