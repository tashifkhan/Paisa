package codes.tashif.paisa.ai.provider

import codes.tashif.paisa.ai.LlmImage
import codes.tashif.paisa.ai.LlmPdf
import codes.tashif.paisa.ai.catalog.ModelInfo
import codes.tashif.paisa.ai.json.JSONObject

/** The prompt side of one call, the same whichever model runs it. */
data class LlmRequest(
    val system: String,
    val prompt: String,
    /** Ask for JSON. The ladder also rejects replies that don't parse. */
    val json: Boolean = false,
    val image: LlmImage? = null,
    val pdf: LlmPdf? = null,
    /** RelayRoom usage tag, for example `statement_import`. */
    val feature: String = "",
)

/** The model side of one call: where it goes and how it thinks. */
data class LlmCall(
    val provider: AiProviderInfo,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val thinking: ThinkingLevel? = null,
    val routeMode: RouteMode = RouteMode.AUTO,
    /** What the catalogue knows about [model], if anything. */
    val modelInfo: ModelInfo? = null,
) {
    val style: ApiStyle get() = provider.style
    val base: String get() = baseUrl.trim().trimEnd('/')
}

/** A built HTTP request, kept as data so tests can read it without a network. */
data class HttpSpec(
    val url: String,
    val headers: Map<String, String>,
    val body: JSONObject? = null,
)

internal const val LOW_TEMPERATURE = 0.1

internal const val CUT_OFF_MESSAGE =
    "Model reply was cut off (max tokens). Try a smaller file or a model " +
        "with a larger output limit."
