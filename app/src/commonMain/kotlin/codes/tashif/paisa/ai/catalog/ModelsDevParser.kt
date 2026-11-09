package codes.tashif.paisa.ai.catalog

import codes.tashif.paisa.ai.provider.ThinkingLevel
import codes.tashif.paisa.ai.json.JSONArray
import codes.tashif.paisa.ai.json.JSONObject

/** One provider's entry in models.dev. */
data class ModelsDevProvider(
    val id: String,
    val name: String,
    /** The provider's OpenAI-compatible base URL, when models.dev lists one. */
    val api: String?,
    val models: List<ModelInfo>,
)

/**
 * The parts of models.dev Paisa keeps: full model lists for the providers it
 * knows, and a capability index by model id for everyone else's models.
 */
data class ModelsDevSnapshot(
    val providers: Map<String, ModelsDevProvider>,
    val index: Map<String, ModelInfo>,
) {
    /** Capabilities for a model id seen on any provider, tried with and without its prefix. */
    fun lookup(modelId: String): ModelInfo? {
        val id = modelId.lowercase()
        return index[id] ?: index[id.substringAfterLast('/')]
    }

    val modelCount: Int get() = providers.values.sumOf { it.models.size }

    companion object {
        val EMPTY = ModelsDevSnapshot(emptyMap(), emptyMap())
    }
}

object ModelsDevParser {
    const val URL = "https://models.dev/api.json"

    fun parse(text: String, keepProviders: Set<String>): ModelsDevSnapshot {
        val root = JSONObject(text)
        val providers = LinkedHashMap<String, ModelsDevProvider>()
        val index = HashMap<String, ModelInfo>()
        for (providerId in root.keys()) {
            val raw = root.optJSONObject(providerId) ?: continue
            val modelsObj = raw.optJSONObject("models") ?: continue
            val models = ArrayList<ModelInfo>(modelsObj.length())
            for (key in modelsObj.keys()) {
                val model = parseModel(modelsObj.optJSONObject(key) ?: continue) ?: continue
                if (!producesText(modelsObj.optJSONObject(key))) continue
                models += model
                val bare = model.id.lowercase()
                index.putIfAbsent(bare, model)
                index.putIfAbsent(bare.substringAfterLast('/'), model)
            }
            if (providerId in keepProviders) {
                providers[providerId] = ModelsDevProvider(
                    id = providerId,
                    name = raw.optString("name", providerId),
                    api = raw.optString("api").takeIf { it.startsWith("http") },
                    models = models.sortedBy { it.name.lowercase() },
                )
            }
        }
        return ModelsDevSnapshot(providers, index)
    }

    /** Skips image, speech and embedding models, which can't return statement JSON. */
    private fun producesText(raw: JSONObject?): Boolean {
        val output = raw?.optJSONObject("modalities")?.optJSONArray("output") ?: return true
        return strings(output).contains("text") && raw.optJSONObject("limit")?.optInt("output", 1) != 0
    }

    fun parseModel(raw: JSONObject): ModelInfo? {
        val id = raw.optString("id").takeIf { it.isNotBlank() } ?: return null
        val input = raw.optJSONObject("modalities")?.optJSONArray("input")?.let(::strings)?.toSet()
        val limit = raw.optJSONObject("limit")
        val options = raw.optJSONArray("reasoning_options")
        val parsed = options?.let(::parseReasoningOptions)
        return ModelInfo(
            id = id,
            name = raw.optString("name").takeIf { it.isNotBlank() } ?: id,
            input = input,
            reasoning = if (raw.has("reasoning")) raw.optBoolean("reasoning") else null,
            reasoningLevels = parsed?.levels,
            budgetThinking = parsed?.budget == true,
            budgetMin = parsed?.budgetMin,
            budgetMax = parsed?.budgetMax,
            contextWindow = limit?.optInt("context", 0)?.takeIf { it > 0 },
            outputLimit = limit?.optInt("output", 0)?.takeIf { it > 0 },
            source = ModelSource.MODELS_DEV,
        )
    }

    internal data class ReasoningOptions(
        val levels: List<ThinkingLevel>,
        val budget: Boolean,
        val budgetMin: Int?,
        val budgetMax: Int?,
    )

    /**
     * `effort` lists exact values; `toggle` means thinking can be switched
     * off (None); `budget_tokens` carries bounds. Unknown values are dropped.
     */
    internal fun parseReasoningOptions(options: JSONArray): ReasoningOptions {
        val levels = LinkedHashSet<ThinkingLevel>()
        var budget = false
        var min: Int? = null
        var max: Int? = null
        for (i in 0 until options.length()) {
            val entry = options.optJSONObject(i) ?: continue
            when (entry.optString("type")) {
                "effort" -> entry.optJSONArray("values")?.let(::strings)
                    ?.mapNotNull(ThinkingLevel::fromWire)?.let(levels::addAll)
                "toggle" -> levels += ThinkingLevel.NONE
                "budget_tokens" -> {
                    budget = true
                    if (entry.has("min")) min = entry.optInt("min")
                    if (entry.has("max")) max = entry.optInt("max")
                }
            }
        }
        return ReasoningOptions(ThinkingLevel.entries.filter { it in levels }, budget, min, max)
    }

    private fun strings(array: JSONArray): List<String> =
        (0 until array.length()).mapNotNull { array.opt(it) as? String }
}
