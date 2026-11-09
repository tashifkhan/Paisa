package codes.tashif.paisa.ai.catalog

import codes.tashif.paisa.ai.provider.AiProviderInfo
import codes.tashif.paisa.ai.provider.AnthropicStyle
import codes.tashif.paisa.ai.provider.ApiStyle
import codes.tashif.paisa.ai.provider.BaseUrlSafety
import codes.tashif.paisa.ai.provider.GeminiStyle
import codes.tashif.paisa.ai.provider.OpenAiStyle
import codes.tashif.paisa.ai.provider.ProviderClient
import codes.tashif.paisa.ai.provider.RelayRoomStyle
import codes.tashif.paisa.ai.provider.RouteMode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Asks a provider which models it serves right now. Costs no tokens. */
class LiveModelLister(
    private val client: ProviderClient,
) {
    suspend fun list(provider: AiProviderInfo, baseUrl: String, apiKey: String): List<ModelInfo> {
        val base = BaseUrlSafety.check(baseUrl)
        return when (provider.style) {
            ApiStyle.OPENAI ->
                LiveModelParsers.openAi(get(OpenAiStyle.modelsUrl(base), OpenAiStyle.headers(apiKey)))
            ApiStyle.ANTHROPIC -> paged { cursor ->
                val url = AnthropicStyle.modelsUrl(base) + "?limit=1000" +
                    (cursor?.let { "&after_id=${encode(it)}" } ?: "")
                LiveModelParsers.anthropic(get(url, AnthropicStyle.headers(apiKey)))
            }
            ApiStyle.GEMINI -> paged { cursor ->
                val url = GeminiStyle.modelsUrl(base) + (cursor?.let { "&pageToken=${encode(it)}" } ?: "")
                LiveModelParsers.gemini(get(url, GeminiStyle.headers(apiKey)))
            }
            ApiStyle.RELAYROOM -> relayRoom(base, apiKey)
        }
    }

    /** Aliases first, then each configured provider's live catalogue. */
    private suspend fun relayRoom(base: String, apiKey: String): List<ModelInfo> = coroutineScope {
        val headers = RelayRoomStyle.headers(apiKey)
        val aliases = async {
            runCatching {
                LiveModelParsers.relayAliases(get(RelayRoomStyle.url(base, "models"), headers))
            }.getOrDefault(emptyList())
        }
        val providers = LiveModelParsers.relayProviders(get(RelayRoomStyle.url(base, "providers"), headers))
        val catalogs = providers.map { (id, modes) ->
            async {
                runCatching {
                    val url = RelayRoomStyle.url(base, "catalog/models?provider=${encode(id)}")
                    LiveModelParsers.relayCatalog(id, get(url, headers), modes)
                }.getOrDefault(emptyList())
            }
        }
        aliases.await() + catalogs.awaitAll().flatten()
    }

    /** `GET /model-options` for one RelayRoom model and mode. */
    suspend fun relayModelOptions(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        mode: RouteMode,
    ): LiveModelParsers.RelayModelOptions {
        val base = BaseUrlSafety.check(baseUrl)
        val (provider, model) = RelayRoomStyle.target(modelId)
        val query = buildString {
            provider?.let { append("provider=").append(encode(it)).append('&') }
            append("model=").append(encode(model))
            if (mode != RouteMode.AUTO) append("&mode=").append(mode.wire)
        }
        val text = get(RelayRoomStyle.url(base, "model-options?$query"), RelayRoomStyle.headers(apiKey))
        return LiveModelParsers.relayModelOptions(text)
    }

    private suspend fun paged(page: suspend (String?) -> Pair<List<ModelInfo>, String?>): List<ModelInfo> {
        val all = ArrayList<ModelInfo>()
        var cursor: String? = null
        repeat(MAX_PAGES) {
            val (models, next) = page(cursor)
            all += models
            cursor = next ?: return all
        }
        return all
    }

    private suspend fun get(url: String, headers: Map<String, String>): String =
        client.get(url, headers, TIMEOUT_SECONDS)

    private fun encode(value: String): String = formEncode(value)

    private companion object {
        const val TIMEOUT_SECONDS = 20L
        const val MAX_PAGES = 5
    }
}

/**
 * Encodes a query value like java.net.URLEncoder with UTF-8: letters, digits
 * and `.-*_` stay, a space becomes `+`, every other byte becomes `%XX`.
 */
internal fun formEncode(value: String): String = buildString {
    for (byte in value.encodeToByteArray()) {
        val c = byte.toInt() and 0xff
        when {
            c in 'a'.code..'z'.code || c in 'A'.code..'Z'.code || c in '0'.code..'9'.code ||
                c == '.'.code || c == '-'.code || c == '*'.code || c == '_'.code -> append(c.toChar())
            c == ' '.code -> append('+')
            else -> {
                append('%')
                append(HEX[c shr 4])
                append(HEX[c and 0xf])
            }
        }
    }
}

private const val HEX = "0123456789ABCDEF"
