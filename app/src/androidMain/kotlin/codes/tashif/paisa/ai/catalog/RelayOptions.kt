package codes.tashif.paisa.ai.catalog

import codes.tashif.paisa.ai.catalog.LiveModelParsers.RelayModelOptions
import codes.tashif.paisa.ai.provider.RouteMode
import org.json.JSONArray
import org.json.JSONObject
import codes.tashif.paisa.ai.provider.ThinkingLevel

/** One RelayRoom `/model-options` answer is for one gateway, one model and one route mode. */
data class RelayOptionsKey(val endpoint: String, val modelId: String, val mode: RouteMode) {
    companion object {
        fun of(baseUrl: String, modelId: String, mode: RouteMode) =
            RelayOptionsKey(baseUrl.trim().trimEnd('/'), modelId, mode)
    }
}

/** Applies cached RelayRoom options to catalogue entries. Pure. */
object RelayOptions {

    /** [model] with the levels and inputs RelayRoom gave. */
    fun apply(model: ModelInfo, options: RelayModelOptions): ModelInfo = model.copy(
        input = options.input,
        reasoning = options.reasoningSupported,
        // None is offered when the route can switch thinking off.
        reasoningLevels = if (options.canTurnOff && options.levels.isNotEmpty()) {
            ThinkingLevel.entries.filter { it == ThinkingLevel.NONE || it in options.levels }
        } else {
            options.levels
        },
        // A budget route names no levels; Paisa sends a token budget inside its bounds.
        budgetThinking = options.budget,
        budgetMin = options.budgetMin ?: if (options.budget && options.canTurnOff) 0 else model.budgetMin,
        budgetMax = options.budgetMax ?: model.budgetMax,
    )

    /**
     * The catalogue list for one gateway, each entry updated from its cached
     * options: Auto's when there are some, otherwise the first mode fetched.
     * Hand-added models are in [models] too, so they get their abilities.
     */
    fun overlay(
        models: List<ModelInfo>,
        baseUrl: String,
        cache: Map<RelayOptionsKey, RelayModelOptions>,
    ): List<ModelInfo> {
        if (cache.isEmpty()) return models
        val endpoint = RelayOptionsKey.of(baseUrl, "", RouteMode.AUTO).endpoint
        return models.map { model ->
            val options = cache[RelayOptionsKey(endpoint, model.id, RouteMode.AUTO)]
                ?: RouteMode.entries.firstNotNullOfOrNull { cache[RelayOptionsKey(endpoint, model.id, it)] }
            if (options == null) model else apply(model, options)
        }
    }

    /** The entry as RelayRoom describes it for [mode], or [model] when that mode wasn't fetched. */
    fun forMode(
        model: ModelInfo?,
        modelId: String,
        baseUrl: String,
        mode: RouteMode,
        cache: Map<RelayOptionsKey, RelayModelOptions>,
    ): ModelInfo? {
        val options = cache[RelayOptionsKey.of(baseUrl, modelId, mode)] ?: return model
        return apply(model ?: ModelInfo(modelId, source = ModelSource.LIVE), options)
    }

    fun encode(cache: Map<RelayOptionsKey, RelayModelOptions>): String {
        val items = JSONArray()
        cache.forEach { (key, o) ->
            items.put(
                JSONObject()
                    .put("endpoint", key.endpoint)
                    .put("model", key.modelId)
                    .put("mode", key.mode.wire)
                    .put("available", o.available)
                    .put("input", JSONArray(o.input.toList()))
                    .put("reasoning", o.reasoningSupported)
                    .put("levels", JSONArray(o.levels.map { it.wire }))
                    .put("budget", o.budget)
                    .put("canTurnOff", o.canTurnOff)
                    .apply { o.budgetMin?.let { put("budgetMin", it) } }
                    .apply { o.budgetMax?.let { put("budgetMax", it) } }
                    .apply { o.mode?.let { put("served", it) } }
            )
        }
        return JSONObject().put("version", VERSION).put("items", items).toString()
    }

    fun decode(text: String): Map<RelayOptionsKey, RelayModelOptions> {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return emptyMap()
        if (root.optInt("version") != VERSION) return emptyMap()
        val items = root.optJSONArray("items") ?: return emptyMap()
        val out = LinkedHashMap<RelayOptionsKey, RelayModelOptions>()
        for (i in 0 until items.length()) {
            val o = items.optJSONObject(i) ?: continue
            val model = o.optString("model").ifBlank { null } ?: continue
            val key = RelayOptionsKey(o.optString("endpoint"), model, RouteMode.fromWire(o.optString("mode")))
            out[key] = RelayModelOptions(
                available = o.optBoolean("available", true),
                input = strings(o.optJSONArray("input")).toSet(),
                reasoningSupported = o.optBoolean("reasoning", false),
                levels = strings(o.optJSONArray("levels")).mapNotNull(ThinkingLevel::fromWire),
                mode = o.optString("served").ifBlank { null },
                budget = o.optBoolean("budget", false),
                canTurnOff = o.optBoolean("canTurnOff", false),
                budgetMin = if (o.has("budgetMin")) o.optInt("budgetMin") else null,
                budgetMax = if (o.has("budgetMax")) o.optInt("budgetMax") else null,
            )
        }
        return out
    }

    private fun strings(array: JSONArray?): List<String> =
        if (array == null) emptyList() else (0 until array.length()).mapNotNull { array.opt(it) as? String }

    private const val VERSION = 3
}
