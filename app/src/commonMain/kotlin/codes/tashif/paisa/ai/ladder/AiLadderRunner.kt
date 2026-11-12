package codes.tashif.paisa.ai.ladder

import codes.tashif.paisa.ai.AiRepository
import codes.tashif.paisa.ai.provider.AiProviderInfo
import codes.tashif.paisa.ai.provider.BaseUrlSafety
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

    suspend fun run(service: AiService, request: LlmRequest): LadderResult {
        repository.awaitReady()
        val rungs = repository.ladders.value.forService(service)
        val tagged = if (request.feature.isBlank()) request.copy(feature = service.id) else request
        return LadderRunner.run(service.label, rungs, tagged, ::resolve, caller)
    }

    /** True when at least one rung of the service's ladder has a provider with a key. */
    fun canRun(service: AiService): Boolean =
        repository.ladders.value.forService(service).any { resolve(it) is ResolvedRung.Ready }

    fun resolve(rung: Rung): ResolvedRung {
        val provider = repository.provider(rung.providerId)
            ?: return ResolvedRung.Skip(rung.modelId, "its provider was removed")
        val info = repository.modelInfo(provider.id, rung.modelId, rung.routeMode)
        val label = "${info?.name ?: rung.modelId} (${provider.name})"
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
     * Checks a provider. With a model it sends one tiny prompt; without one it
     * lists models, which costs no tokens. Returns a short line for the UI.
     */
    suspend fun testProvider(providerId: String, modelId: String? = null): Result<String> {
        repository.awaitReady()
        val provider = repository.provider(providerId)
            ?: return Result.failure(IllegalArgumentException("Unknown provider"))
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
}
