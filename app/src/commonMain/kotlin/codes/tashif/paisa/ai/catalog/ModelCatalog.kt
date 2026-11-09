package codes.tashif.paisa.ai.catalog

import codes.tashif.paisa.ai.aiIoDispatcher
import codes.tashif.paisa.platform.nowMillis
import codes.tashif.paisa.ai.provider.AiProviderInfo
import codes.tashif.paisa.ai.provider.BuiltInProviders
import codes.tashif.paisa.ai.provider.ProviderClient
import codes.tashif.paisa.ai.provider.RouteMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Where the models.dev part of the catalogue came from. */
enum class CatalogSource { NETWORK, CACHE, BUILT_IN }

data class ModelsDevState(
    val snapshot: ModelsDevSnapshot = ModelsDevSnapshot.EMPTY,
    val source: CatalogSource = CatalogSource.BUILT_IN,
    /** Epoch millis of the models.dev fetch, or null for built-ins. */
    val fetchedAt: Long? = null,
    /** Why the last refresh failed, if it did. */
    val error: String? = null,
    val refreshing: Boolean = false,
)

/**
 * models.dev plus each provider's live listing, cached in [cache] so it
 * survives restarts. On Android that is noBackupFilesDir, so it never goes
 * into a backup. This class never touches the network on its own; callers
 * decide when to refresh.
 */
class ModelCatalog(
    private val cache: AiCacheStore,
    private val lister: LiveModelLister,
    private val client: ProviderClient,
) {
    private val mutex = Mutex()
    /** Serialises taking a snapshot and writing it, so the newest state is what lands on disk. */
    private val writeMutex = Mutex()
    private var cacheLoaded = false

    private val _modelsDev = MutableStateFlow(ModelsDevState())
    val modelsDev: StateFlow<ModelsDevState> = _modelsDev.asStateFlow()

    private val _live = MutableStateFlow<Map<String, LiveList>>(emptyMap())
    val live: StateFlow<Map<String, LiveList>> = _live.asStateFlow()

    private val _liveErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val liveErrors: StateFlow<Map<String, String>> = _liveErrors.asStateFlow()

    private val _relayOptions = MutableStateFlow<Map<RelayOptionsKey, LiveModelParsers.RelayModelOptions>>(emptyMap())

    /** RelayRoom `/model-options` answers by gateway, model and route mode. */
    val relayOptions: StateFlow<Map<RelayOptionsKey, LiveModelParsers.RelayModelOptions>> = _relayOptions.asStateFlow()

    /** Reads the disk cache once. Cheap after the first call. */
    suspend fun ensureLoaded() {
        if (cacheLoaded) return
        mutex.withLock {
            if (cacheLoaded) return
            withContext(aiIoDispatcher) {
                cache.read(MODELS_DEV_FILE)
                    ?.let(CatalogCodec::decodeModelsDev)
                    ?.let { (snapshot, fetchedAt) ->
                        _modelsDev.value = ModelsDevState(snapshot, CatalogSource.CACHE, fetchedAt)
                    }
                cache.read(LIVE_FILE)
                    ?.let(CatalogCodec::decodeLive)
                    ?.let { _live.value = it }
                cache.read(RELAY_OPTIONS_FILE)
                    ?.let(RelayOptions::decode)
                    ?.let { _relayOptions.value = it }
            }
            cacheLoaded = true
        }
    }

    fun isStale(now: Long = nowMillis()): Boolean {
        val fetchedAt = _modelsDev.value.fetchedAt ?: return true
        return now - fetchedAt > STALE_AFTER_MS
    }

    /** Fetches models.dev. On failure the cache, then built-ins, stay in place. */
    suspend fun refreshModelsDev(): Result<Unit> {
        ensureLoaded()
        _modelsDev.update { it.copy(refreshing = true) }
        return try {
            val text = client.get(ModelsDevParser.URL, mapOf("Accept" to "application/json"), MODELS_DEV_TIMEOUT_S)
            val snapshot = withContext(Dispatchers.Default) { ModelsDevParser.parse(text, KEEP_PROVIDERS) }
            val now = nowMillis()
            _modelsDev.value = ModelsDevState(snapshot, CatalogSource.NETWORK, now)
            persist(MODELS_DEV_FILE) { CatalogCodec.encodeModelsDev(snapshot, now) }
            Result.success(Unit)
        } catch (e: CancellationException) {
            _modelsDev.update { it.copy(refreshing = false) }
            throw e
        } catch (e: Exception) {
            _modelsDev.update { it.copy(refreshing = false, error = e.message ?: "Couldn't load models.dev") }
            Result.failure(e)
        }
    }

    /** Lists one provider's live models and caches them. Returns how many. */
    suspend fun refreshLive(provider: AiProviderInfo, baseUrl: String, apiKey: String): Result<Int> {
        ensureLoaded()
        return try {
            val models = lister.list(provider, baseUrl, apiKey)
            putLive(provider.id, LiveList(models, nowMillis()))
            _liveErrors.update { it - provider.id }
            Result.success(models.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _liveErrors.update { it + (provider.id to (e.message ?: "Couldn't list models")) }
            Result.failure(e)
        }
    }

    /**
     * Asks RelayRoom what one model accepts in one route mode and caches the
     * answer under the gateway, model and mode. The catalogue entry, live or
     * added by hand, takes those levels and input kinds.
     */
    suspend fun refreshRelayOptions(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        mode: RouteMode,
    ): Result<LiveModelParsers.RelayModelOptions> = runCatching {
        ensureLoaded()
        val options = lister.relayModelOptions(baseUrl, apiKey, modelId, mode)
        _relayOptions.update { it + (RelayOptionsKey.of(baseUrl, modelId, mode) to options) }
        persist(RELAY_OPTIONS_FILE) { RelayOptions.encode(_relayOptions.value) }
        options
    }

    suspend fun forgetLive(providerId: String) {
        putLive(providerId, null)
        _liveErrors.update { it - providerId }
    }

    private suspend fun putLive(providerId: String, list: LiveList?) {
        _live.update { if (list == null) it - providerId else it + (providerId to list) }
        persist(LIVE_FILE) { CatalogCodec.encodeLive(_live.value) }
    }

    /** Encodes inside the lock so a slower writer can't land an older snapshot last. */
    private suspend fun persist(name: String, encode: () -> String) {
        writeMutex.withLock {
            withContext(aiIoDispatcher) { cache.write(name, encode()) }
        }
    }

    companion object {
        const val MODELS_DEV_TIMEOUT_S = 12L
        const val STALE_AFTER_MS = 24L * 60 * 60 * 1000

        const val MODELS_DEV_FILE = "models-dev.json"
        const val LIVE_FILE = "live-models.json"
        const val RELAY_OPTIONS_FILE = "relay-options.json"

        /** models.dev provider keys whose full model lists are kept. */
        val KEEP_PROVIDERS: Set<String> =
            BuiltInProviders.all.mapNotNull { it.modelsDevId }.toSet()
    }
}
