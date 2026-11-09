package codes.tashif.paisa.ai.catalog

import codes.tashif.paisa.ai.provider.RouteMode
import codes.tashif.paisa.ai.provider.ThinkingLevel
import codes.tashif.paisa.ai.json.JSONArray
import codes.tashif.paisa.ai.json.JSONObject

/** Pure parsers for each provider's model listing. No network here. */
object LiveModelParsers {

    /** OpenAI-style `GET /models`: `{data: [{id}]}`. */
    fun openAi(text: String): List<ModelInfo> =
        objects(JSONObject(text).optJSONArray("data")).mapNotNull { item ->
            val id = item.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ModelInfo(id = id, name = item.optString("name").ifBlank { id }, source = ModelSource.LIVE)
        }

    /** Anthropic `GET /v1/models`: `{data: [{id, display_name}], has_more, last_id}`. */
    fun anthropic(text: String): Pair<List<ModelInfo>, String?> {
        val json = JSONObject(text)
        val models = objects(json.optJSONArray("data")).mapNotNull { item ->
            val id = item.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ModelInfo(id = id, name = item.optString("display_name").ifBlank { id }, source = ModelSource.LIVE)
        }
        val next = if (json.optBoolean("has_more")) json.optString("last_id").ifBlank { null } else null
        return models to next
    }

    /** Gemini `GET /models`: keeps models that can generateContent. */
    fun gemini(text: String): Pair<List<ModelInfo>, String?> {
        val json = JSONObject(text)
        val models = objects(json.optJSONArray("models")).mapNotNull { item ->
            val methods = item.optJSONArray("supportedGenerationMethods")?.let(::strings)
            if (methods != null && "generateContent" !in methods) return@mapNotNull null
            val id = item.optString("name").removePrefix("models/").takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            ModelInfo(
                id = id,
                name = item.optString("displayName").ifBlank { id },
                reasoning = if (item.has("thinking")) item.optBoolean("thinking") else null,
                contextWindow = item.optInt("inputTokenLimit", 0).takeIf { it > 0 },
                outputLimit = item.optInt("outputTokenLimit", 0).takeIf { it > 0 },
                source = ModelSource.LIVE,
            )
        }
        return models to json.optString("nextPageToken").ifBlank { null }
    }

    /** RelayRoom `GET /providers`: ids that are configured and have live routes. */
    fun relayProviders(text: String): List<Pair<String, List<RouteMode>>> =
        objects(JSONObject(text).optJSONArray("data"))
            .filter { it.optBoolean("configured") && it.optInt("live_count") > 0 }
            .map { it.optString("id") to modes(it.optJSONArray("modes")) }
            .filter { it.first.isNotBlank() }

    /** RelayRoom `GET /catalog/models?provider=`: stored as `<provider>/<model>`. */
    fun relayCatalog(
        provider: String,
        text: String,
        providerModes: List<RouteMode> = emptyList(),
    ): List<ModelInfo> = objects(JSONObject(text).optJSONArray("data"))
        .filter { !it.has("available") || it.optBoolean("available") }
        .mapNotNull { record ->
            val id = record.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val caps = record.optJSONObject("capabilities") ?: JSONObject()
            val limit = record.optJSONObject("limit")
            ModelInfo(
                id = "$provider/$id",
                name = record.optString("name").ifBlank { id },
                input = caps.optJSONArray("input")?.let(::strings)?.toSet() ?: setOf("text"),
                reasoning = caps.optBoolean("reasoning", false),
                contextWindow = limit?.optInt("context", 0)?.takeIf { it > 0 },
                outputLimit = limit?.optInt("output", 0)?.takeIf { it > 0 },
                routeModes = modes(record.optJSONArray("modes")).ifEmpty { providerModes },
                upstream = record.optString("provider_name").ifBlank { provider },
                source = ModelSource.LIVE,
            )
        }

    /** Operator aliases from RelayRoom's OpenAI-style `GET /models` (`owned_by: alias`). */
    fun relayAliases(text: String): List<ModelInfo> =
        objects(JSONObject(text).optJSONArray("data"))
            .filter { it.optString("owned_by") == "alias" }
            .mapNotNull { item ->
                val id = item.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                ModelInfo(id = id, name = id, alias = true, source = ModelSource.LIVE)
            }

    /** What RelayRoom's `/model-options` says one route accepts. */
    data class RelayModelOptions(
        val available: Boolean,
        val input: Set<String>,
        val reasoningSupported: Boolean,
        val levels: List<ThinkingLevel>,
        val mode: String?,
        /** The route takes a thinking token budget rather than named levels (Gemini 2.5, older Claude). */
        val budget: Boolean = false,
        /** Thinking can be switched off on this route. */
        val canTurnOff: Boolean = false,
        val budgetMin: Int? = null,
        val budgetMax: Int? = null,
    )

    fun relayModelOptions(text: String): RelayModelOptions {
        val json = JSONObject(text)
        val reasoning = json.optJSONObject("reasoning")
        val input = (json.optJSONArray("input_types") ?: json.optJSONArray("input_modalities"))
            ?.let(::strings)?.toSet() ?: setOf("text")
        val levels = reasoning?.optJSONArray("levels")?.let(::strings)
            ?.mapNotNull(ThinkingLevel::fromWire).orEmpty()
        return RelayModelOptions(
            available = json.optBoolean("available", true),
            input = input,
            reasoningSupported = reasoning?.optBoolean("supported") == true,
            levels = ThinkingLevel.entries.filter { it in levels },
            mode = json.optString("mode").ifBlank { null },
            budget = reasoning?.optJSONObject("budget_tokens") != null ||
                reasoning?.optString("provider_mode") == "budget",
            canTurnOff = reasoning?.optBoolean("toggle") == true ||
                reasoning?.optJSONObject("budget_tokens")?.optInt("min", -1) == 0,
            budgetMin = reasoning?.optJSONObject("budget_tokens")?.takeIf { it.has("min") }?.optInt("min"),
            budgetMax = reasoning?.optJSONObject("budget_tokens")?.takeIf { it.has("max") }?.optInt("max"),
        )
    }

    private fun modes(array: JSONArray?): List<RouteMode> =
        array?.let(::strings)?.mapNotNull { wire -> RouteMode.entries.firstOrNull { it.wire == wire } }
            ?.distinct().orEmpty()

    private fun objects(array: JSONArray?): List<JSONObject> =
        if (array == null) emptyList() else (0 until array.length()).mapNotNull(array::optJSONObject)

    private fun strings(array: JSONArray): List<String> =
        (0 until array.length()).mapNotNull { array.opt(it) as? String }
}
