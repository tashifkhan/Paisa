package codes.tashif.paisa.ai.catalog

import android.content.Context
import codes.tashif.paisa.ai.provider.AiProviderInfo
import codes.tashif.paisa.ai.provider.BuiltInProviders
import codes.tashif.paisa.ai.provider.ProviderClient
import codes.tashif.paisa.ai.provider.RouteMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

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
 * models.dev plus each provider's live listing, cached in noBackupFilesDir so
 * it survives restarts and never goes into a backup. This class never touches
 * the network on its own; callers decide when to refresh.
 */
@Singleton
class ModelCatalog @Inject constructor(
    @ApplicationContext context: Context,
    private val lister: LiveModelLister,
    private val client: ProviderClient,
) {
    private val dir = File(context.noBackupFilesDir, "ai")
    private val modelsDevFile = File(dir, "models-dev.json")
    private val liveFile = File(dir, "live-models.json")
    private val relayOptionsFile = File(dir, "relay-options.json")
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
            withContext(Dispatchers.IO) {
                runCatching { modelsDevFile.takeIf { it.exists() }?.readText() }.getOrNull()
                    ?.let(CatalogCodec::decodeModelsDev)
                    ?.let { (snapshot, fetchedAt) ->
                        _modelsDev.value = ModelsDevState(snapshot, CatalogSource.CACHE, fetchedAt)
                    }
                runCatching { liveFile.takeIf { it.exists() }?.readText() }.getOrNull()
                    ?.let(CatalogCodec::decodeLive)
                    ?.let { _live.value = it }
                runCatching { relayOptionsFile.takeIf { it.exists() }?.readText() }.getOrNull()
                    ?.let(RelayOptions::decode)
                    ?.let { _relayOptions.value = it }
            }
            cacheLoaded = true
        }
    }

    fun isStale(now: Long = System.currentTimeMillis()): Boolean {
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
            val now = System.currentTimeMillis()
            _modelsDev.value = ModelsDevState(snapshot, CatalogSource.NETWORK, now)
            persist(modelsDevFile) { CatalogCodec.encodeModelsDev(snapshot, now) }
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
            putLive(provider.id, LiveList(models, System.currentTimeMillis()))
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
        persist(relayOptionsFile) { RelayOptions.encode(_relayOptions.value) }
        options
    }

    suspend fun forgetLive(providerId: String) {
        putLive(providerId, null)
        _liveErrors.update { it - providerId }
    }

    private suspend fun putLive(providerId: String, list: LiveList?) {
        _live.update { if (list == null) it - providerId else it + (providerId to list) }
        persist(liveFile) { CatalogCodec.encodeLive(_live.value) }
    }

    /** Encodes inside the lock so a slower writer can't land an older snapshot last. */
    private suspend fun persist(file: File, encode: () -> String) {
        writeMutex.withLock {
            withContext(Dispatchers.IO) { CacheFiles.writeAtomically(file, encode()) }
        }
    }

    companion object {
        const val MODELS_DEV_TIMEOUT_S = 12L
        const val STALE_AFTER_MS = 24L * 60 * 60 * 1000

        /** models.dev provider keys whose full model lists are kept. */
        val KEEP_PROVIDERS: Set<String> =
            BuiltInProviders.all.mapNotNull { it.modelsDevId }.toSet()
    }
}
