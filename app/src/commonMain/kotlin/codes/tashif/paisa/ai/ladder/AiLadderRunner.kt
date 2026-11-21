package codes.tashif.paisa.ai.ladder

import codes.tashif.paisa.ai.AiRepository
import codes.tashif.paisa.ai.local.LOCAL_AI_MODEL_ID
import codes.tashif.paisa.ai.provider.AiProviderInfo
import codes.tashif.paisa.ai.provider.BaseUrlSafety
import codes.tashif.paisa.ai.provider.BuiltInProviders
import codes.tashif.paisa.ai.provider.LlmCall
import codes.tashif.paisa.ai.provider.LlmRequest
import codes.tashif.paisa.ai.provider.ModelCaller
import codes.tashif.paisa.ai.provider.ProviderClient
import kotlinx.coroutines.CancellationException

/** Runs a service's ladder against the saved keys, config and catalogue. */
class AiLadderRunner(
    private val repository: AiRepository,
    client: ProviderClient,
) {
    /** Swappable so tests can run ladders with no network. */
    internal var caller: ModelCaller = client

    /** With [cloudOnly], on-device rungs are left out: the caller chose a cloud model. */
    suspend fun run(service: AiService, request: LlmRequest, cloudOnly: Boolean = false): LadderResult {
        repository.awaitReady()
        val rungs = repository.ladders.value.forService(service)
            .filterNot { cloudOnly && it.providerId == BuiltInProviders.LOCAL }
        val tagged = if (request.feature.isBlank()) request.copy(feature = service.id) else request
        return LadderRunner.run(service.label, rungs, tagged, ::resolve, caller)
    }

    /** True when at least one rung of the service's ladder has a provider with a key. */
    fun canRun(service: AiService, cloudOnly: Boolean = false): Boolean =
        repository.ladders.value.forService(service)
            .filterNot { cloudOnly && it.providerId == BuiltInProviders.LOCAL }
            .any { resolve(it) is ResolvedRung.Ready }

    fun resolve(rung: Rung): ResolvedRung {
        val provider = repository.provider(rung.providerId)
            ?: return ResolvedRung.Skip(rung.modelId, "its provider was removed")
        val info = repository.modelInfo(provider.id, rung.modelId, rung.routeMode)
        val label = "${info?.name ?: rung.modelId} (${provider.name})"
        if (provider.local) {
            val status = repository.localStatus.value
            if (!status.ready) {
                return ResolvedRung.Skip(label, status.skipReason ?: "the on-device model isn't ready")
            }
            return ResolvedRung.Ready(
                LlmCall(
                    provider = provider,
                    baseUrl = "",
                    apiKey = "",
                    model = rung.modelId,
                    thinking = rung.thinking,
                    routeMode = rung.routeMode,
                    modelInfo = info,
                ),
                label,
            )
        }
        val key = repository.key(provider.id)
            ?: return ResolvedRung.Skip(label, "no key saved for ${provider.name}")
        val base = repository.baseUrlFor(provider)
        if (!BaseUrlSafety.isSafe(base)) return ResolvedRung.Skip(label, "base URL isn't allowed")
        return ResolvedRung.Ready(
            LlmCall(
                provider = provider,
                baseUrl = base,
                apiKey = key,
                model = rung.modelId,
                thinking = rung.thinking,
                routeMode = rung.routeMode,
                modelInfo = info,
            ),
            label,
        )
    }

    /**
     * True when some rung of the service's ladder could take [request]'s
     * inputs, for example a voice note, without spending a call to find out.
     */
    fun canTake(service: AiService, request: LlmRequest): Boolean =
        repository.ladders.value.forService(service).any { rung ->
            val ready = resolve(rung) as? ResolvedRung.Ready ?: return@any false
            LadderRunner.inputProblem(ready.call, request) == null
        }

    /**
     * Characters to put in one text call. Gemini Nano has about 4,000 input
     * tokens, so a local rung gets smaller chunks; cloud models keep the
     * caller's larger default.
     */
    fun textBudget(service: AiService, defaultChars: Int): Int {
        val usesLocal = repository.ladders.value.forService(service)
            .any { it.providerId == BuiltInProviders.LOCAL && repository.localStatus.value.ready }
        return if (usesLocal) LOCAL_TEXT_CHARS else defaultChars
    }

    /**
     * Checks a provider. With a model it sends one tiny prompt; without one it
     * lists models, which costs no tokens. Returns a short line for the UI.
     */
    suspend fun testProvider(providerId: String, modelId: String? = null): Result<String> {
        repository.awaitReady()
        val provider = repository.provider(providerId)
            ?: return Result.failure(IllegalArgumentException("Unknown provider"))
        if (provider.local) {
            return ping(provider, "", "", modelId ?: LOCAL_AI_MODEL_ID)
        }
        val key = repository.key(providerId)
            ?: return Result.failure(IllegalStateException("Save a key first"))
        if (modelId == null) {
            return repository.refreshProviderModels(providerId).map { count -> "Connected · $count models" }
        }
        return ping(provider, repository.baseUrlFor(provider), key, modelId)
    }

    /** One short completion against settings that may not be saved yet. */
    suspend fun ping(provider: AiProviderInfo, baseUrl: String, apiKey: String, modelId: String): Result<String> =
        try {
            val info = repository.catalog.value.model(provider.id, modelId)
            val reply = caller.complete(
                LlmCall(provider, baseUrl, apiKey, modelId, modelInfo = info),
                LlmRequest(system = "Reply with exactly: OK", prompt = "ping", feature = "connection_test"),
            )
            Result.success("Connected · reply: ${reply.trim().take(80)}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private companion object {
        /** About 1,300 words, leaving room for the system prompt and JSON instructions. */
        const val LOCAL_TEXT_CHARS = 8_000
    }
}
