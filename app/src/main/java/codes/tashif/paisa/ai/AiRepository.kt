package codes.tashif.paisa.ai

import codes.tashif.paisa.ai.catalog.CatalogMerge
import codes.tashif.paisa.ai.catalog.CatalogSource
import codes.tashif.paisa.ai.catalog.LiveModelParsers
import codes.tashif.paisa.ai.catalog.ModelCatalog
import codes.tashif.paisa.ai.catalog.ModelInfo
import codes.tashif.paisa.ai.catalog.RelayOptions
import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.ai.ladder.LadderConfig
import codes.tashif.paisa.ai.ladder.LadderState
import codes.tashif.paisa.ai.ladder.Rung
import codes.tashif.paisa.ai.ladder.ServiceLadder
import codes.tashif.paisa.ai.provider.AiProviderInfo
import codes.tashif.paisa.ai.provider.ApiStyle
import codes.tashif.paisa.ai.provider.BaseUrlSafety
import codes.tashif.paisa.ai.provider.BuiltInProviders
import codes.tashif.paisa.ai.provider.CustomProvider
import codes.tashif.paisa.ai.provider.RouteMode
import codes.tashif.paisa.ai.provider.ThinkingLevel
import codes.tashif.paisa.ai.store.AiConfig
import codes.tashif.paisa.ai.store.AiConfigStore
import codes.tashif.paisa.ai.store.AiKeyStore
import codes.tashif.paisa.ai.store.LegacyMigrator
import codes.tashif.paisa.ai.store.ProviderResolution
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A provider with what the UI shows next to it. */
data class ProviderStatus(
    val info: AiProviderInfo,
    val hasKey: Boolean,
    val baseUrl: String,
    val modelCount: Int,
    /** When this provider's live list was fetched, if ever. */
    val liveFetchedAt: Long?,
    val liveError: String?,
)

/** The merged catalogue, keyed by provider id. */
data class CatalogState(
    val models: Map<String, List<ModelInfo>> = emptyMap(),
    val source: CatalogSource = CatalogSource.BUILT_IN,
    /** Epoch millis of the models.dev fetch; null when only built-ins are loaded. */
    val fetchedAt: Long? = null,
    val error: String? = null,
    val refreshing: Boolean = false,
) {
    val modelCount: Int get() = models.values.sumOf { it.size }

    fun model(providerId: String, modelId: String): ModelInfo? =
        models[providerId]?.firstOrNull { it.id == modelId }
}

/**
 * The AI settings: providers and keys, the model catalogue and the ladders.
 * The UI reads the flows and calls the suspend functions; [AiLadderRunner]
 * runs requests.
 */
class AiRepository(
    private val keys: AiKeyStore,
    private val configStore: AiConfigStore,
    private val catalogSource: ModelCatalog,
    private val legacyMigrator: LegacyMigrator,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val ready = CompletableDeferred<Unit>()

    val config: StateFlow<AiConfig> = configStore.config

    /** Provider ids with a key saved. */
    val keyIds: StateFlow<Set<String>> = keys.withKeys

    val catalog: StateFlow<CatalogState> = combine(
        configStore.config, catalogSource.modelsDev, catalogSource.live, catalogSource.relayOptions
    ) { config, dev, live, relayOptions ->
        val models = ProviderResolution.all(config).associate { provider ->
            val merged = CatalogMerge.modelsFor(
                provider, dev.snapshot, live[provider.id], config.manualModels[provider.id].orEmpty()
            )
            provider.id to if (provider.id == BuiltInProviders.RELAYROOM) {
                RelayOptions.overlay(merged, baseUrlFor(provider, config), relayOptions)
            } else {
                merged
            }
        }
        CatalogState(models, dev.source, dev.fetchedAt, dev.error, dev.refreshing)
    }.stateIn(scope, SharingStarted.Eagerly, CatalogState())

    val providers: StateFlow<List<ProviderStatus>> = combine(
        configStore.config, keys.withKeys, catalog, catalogSource.live, catalogSource.liveErrors
    ) { config, withKeys, catalog, live, errors ->
        ProviderResolution.all(config).map { info ->
            ProviderStatus(
                info = info,
                hasKey = info.id in withKeys,
                baseUrl = baseUrlFor(info, config),
                modelCount = catalog.models[info.id]?.size ?: 0,
                liveFetchedAt = live[info.id]?.fetchedAt,
                liveError = errors[info.id],
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val ladders: StateFlow<LadderState> = configStore.config.map(::ladderState)
        .stateIn(scope, SharingStarted.Eagerly, ladderState(configStore.config.value))

    init {
        scope.launch {
            try {
                legacyMigrator.run()
                catalogSource.ensureLoaded()
            } finally {
                ready.complete(Unit)
            }
        }
    }

    /** Waits for migration and the disk cache. Cheap once done. */
    suspend fun awaitReady() = ready.await()

    // Keys and providers

    fun key(providerId: String): String? = keys.key(providerId)

    fun provider(id: String): AiProviderInfo? = ProviderResolution.find(config.value, id)

    fun baseUrlFor(provider: AiProviderInfo, config: AiConfig = this.config.value): String {
        val api = provider.modelsDevId?.let { catalogSource.modelsDev.value.snapshot.providers[it]?.api }
        return ProviderResolution.baseUrl(provider, config, api)
    }

    suspend fun saveKey(providerId: String, key: String) = keys.save(providerId, key)

    suspend fun removeKey(providerId: String) {
        keys.remove(providerId)
        catalogSource.forgetLive(providerId)
    }

    /** Sets or, with null or blank, clears a base URL override. Throws when the URL is unsafe. */
    suspend fun setBaseUrl(providerId: String, url: String?) {
        val clean = url?.takeIf { it.isNotBlank() }?.let { BaseUrlSafety.check(ProviderResolution.normalize(it)) }
        configStore.update { c ->
            val custom = c.customProviders.map { if (it.id == providerId && clean != null) it.copy(baseUrl = clean) else it }
            val isCustom = c.customProviders.any { it.id == providerId }
            c.copy(
                customProviders = custom,
                baseUrls = if (clean == null || isCustom) c.baseUrls - providerId else c.baseUrls + (providerId to clean),
            )
        }
    }

    /** Adds a provider of any of the three API styles. Throws when the URL is unsafe. */
    suspend fun addCustomProvider(name: String, style: ApiStyle, baseUrl: String): AiProviderInfo {
        require(style in ApiStyle.customStyles) { "Pick OpenAI, Anthropic or Gemini style" }
        val clean = BaseUrlSafety.check(ProviderResolution.normalize(baseUrl))
        val label = name.trim().ifEmpty { "Custom" }
        var added: CustomProvider? = null
        configStore.update { c ->
            val id = ProviderResolution.newCustomId(label, c.customProviders.mapTo(HashSet()) { it.id })
            val custom = CustomProvider(id, label, style, clean)
            added = custom
            c.copy(customProviders = c.customProviders + custom)
        }
        return added!!.toInfo()
    }

    /** Removes a custom provider, its key, its models and every rung that used it. */
    suspend fun deleteCustomProvider(id: String) {
        configStore.update { c ->
            if (c.customProviders.none { it.id == id }) return@update c
            c.copy(
                customProviders = c.customProviders.filterNot { it.id == id },
                baseUrls = c.baseUrls - id,
                manualModels = c.manualModels - id,
                defaultLadder = c.defaultLadder.filterNot { it.providerId == id },
                serviceLadders = c.serviceLadders
                    .mapValues { (_, rungs) -> rungs.filterNot { it.providerId == id } }
                    .filterValues { it.isNotEmpty() },
            )
        }
        removeKey(id)
    }

    suspend fun addModel(providerId: String, modelId: String) {
        val id = modelId.trim()
        if (id.isEmpty()) return
        configStore.update { c ->
            val existing = c.manualModels[providerId].orEmpty()
            if (id in existing) c else c.copy(manualModels = c.manualModels + (providerId to existing + id))
        }
    }

    suspend fun removeModel(providerId: String, modelId: String) {
        configStore.update { c ->
            val left = c.manualModels[providerId].orEmpty() - modelId
            c.copy(manualModels = if (left.isEmpty()) c.manualModels - providerId else c.manualModels + (providerId to left))
        }
    }

    // Ladders

    /**
     * Changes one ladder. [change] gets that ladder as it is in the latest
     * config, inside the config store's lock, so two quick edits can't
     * overwrite each other. [service] null is the Default ladder. A service on
     * the default sees a copy of it, and an empty result makes it use the
     * default again. Returns the ladder after the edit.
     */
    suspend fun editLadder(service: AiService?, change: (List<Rung>) -> List<Rung>): List<Rung> {
        var result: List<Rung> = emptyList()
        configStore.update { c ->
            val next = LadderConfig.edit(c, service, change)
            result = LadderConfig.rungs(next, service)
            next
        }
        return result
    }

    /** Replaces a ladder. [service] null is the Default ladder; an empty list makes a service use it. */
    suspend fun setLadder(service: AiService?, rungs: List<Rung>) {
        editLadder(service) { rungs }
    }

    /** Appends a rung. A service on the default ladder starts its own from a copy of it. */
    suspend fun addRung(service: AiService?, rung: Rung) {
        editLadder(service) { it + rung }
    }

    suspend fun useDefaultLadder(service: AiService) {
        editLadder(service) { emptyList() }
    }

    /**
     * The catalogue entry a rung runs with. For RelayRoom this is what
     * `/model-options` said for that rung's own route mode, when known.
     */
    fun modelInfo(providerId: String, modelId: String, mode: RouteMode = RouteMode.AUTO): ModelInfo? {
        val info = catalog.value.model(providerId, modelId)
        if (providerId != BuiltInProviders.RELAYROOM) return info
        val provider = provider(providerId) ?: return info
        return RelayOptions.forMode(info, modelId, baseUrlFor(provider), mode, catalogSource.relayOptions.value)
    }

    /** The thinking levels to offer for one model, not counting Auto. */
    fun thinkingChoices(providerId: String, modelId: String, mode: RouteMode = RouteMode.AUTO): List<ThinkingLevel> {
        val style = provider(providerId)?.style ?: return emptyList()
        return modelInfo(providerId, modelId, mode)?.thinkingChoices(style).orEmpty()
    }

    // Catalogue

    /** models.dev, then the live list of every provider with a key. */
    suspend fun refreshCatalog(): Result<Unit> = coroutineScope {
        awaitReady()
        val dev = async { catalogSource.refreshModelsDev() }
        ProviderResolution.all(config.value).filter { keys.hasKey(it.id) }
            .map { async { refreshProviderModels(it.id) } }
            .awaitAll()
        dev.await()
    }

    /** Refreshes only when the cache is a day old and some provider has a key. */
    suspend fun refreshIfStale() {
        awaitReady()
        if (keys.withKeys.value.isNotEmpty() && catalogSource.isStale()) refreshCatalog()
    }

    suspend fun refreshProviderModels(providerId: String): Result<Int> {
        val info = provider(providerId) ?: return Result.failure(IllegalArgumentException("Unknown provider"))
        val key = keys.key(providerId) ?: return Result.failure(IllegalStateException("No key saved"))
        return catalogSource.refreshLive(info, baseUrlFor(info), key)
    }

    /** RelayRoom's exact levels and inputs for one model, saved onto its catalogue entry. */
    suspend fun refreshRelayModelOptions(modelId: String, mode: RouteMode): Result<LiveModelParsers.RelayModelOptions> {
        val info = provider(BuiltInProviders.RELAYROOM)!!
        val key = keys.key(info.id) ?: return Result.failure(IllegalStateException("No key saved"))
        return catalogSource.refreshRelayOptions(baseUrlFor(info), key, modelId, mode)
    }

    private fun ladderState(config: AiConfig): LadderState = LadderState(
        default = config.defaultLadder,
        services = AiService.entries.map { service ->
            val own = config.serviceLadders[service]
            ServiceLadder(service, own, own ?: config.defaultLadder)
        },
    )
}
