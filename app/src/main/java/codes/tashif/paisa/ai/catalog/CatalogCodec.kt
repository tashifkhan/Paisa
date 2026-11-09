package codes.tashif.paisa.ai.catalog

import codes.tashif.paisa.ai.provider.RouteMode
import codes.tashif.paisa.ai.provider.ThinkingLevel
import org.json.JSONArray
import org.json.JSONObject

/** One provider's live listing and when it was fetched. */
data class LiveList(val models: List<ModelInfo>, val fetchedAt: Long)

/** JSON for the on-disk catalogue cache. Pure, so tests can round-trip it. */
object CatalogCodec {
    private const val VERSION = 1

    fun encodeModel(m: ModelInfo): JSONObject = JSONObject().apply {
        put("id", m.id)
        if (m.name != m.id) put("name", m.name)
        m.input?.let { put("input", JSONArray(it.toList())) }
        m.reasoning?.let { put("reasoning", it) }
        m.reasoningLevels?.let { levels -> put("levels", JSONArray(levels.map { it.wire })) }
        if (m.budgetThinking) put("budget", true)
        m.budgetMin?.let { put("bmin", it) }
        m.budgetMax?.let { put("bmax", it) }
        m.contextWindow?.let { put("ctx", it) }
        m.outputLimit?.let { put("out", it) }
        if (m.routeModes.isNotEmpty()) put("modes", JSONArray(m.routeModes.map { it.wire }))
        if (m.alias) put("alias", true)
        m.upstream?.let { put("upstream", it) }
        put("source", m.source.name)
    }

    fun decodeModel(o: JSONObject): ModelInfo? {
        val id = o.optString("id").takeIf { it.isNotBlank() } ?: return null
        return ModelInfo(
            id = id,
            name = o.optString("name").ifBlank { id },
            input = o.optJSONArray("input")?.let(::strings)?.toSet(),
            reasoning = if (o.has("reasoning")) o.optBoolean("reasoning") else null,
            reasoningLevels = o.optJSONArray("levels")?.let(::strings)?.mapNotNull(ThinkingLevel::fromWire),
            budgetThinking = o.optBoolean("budget", false),
            budgetMin = if (o.has("bmin")) o.optInt("bmin") else null,
            budgetMax = if (o.has("bmax")) o.optInt("bmax") else null,
            contextWindow = if (o.has("ctx")) o.optInt("ctx") else null,
            outputLimit = if (o.has("out")) o.optInt("out") else null,
            routeModes = o.optJSONArray("modes")?.let(::strings)?.map(RouteMode::fromWire).orEmpty(),
            alias = o.optBoolean("alias", false),
            upstream = o.optString("upstream").ifBlank { null },
            source = runCatching { ModelSource.valueOf(o.optString("source")) }
                .getOrDefault(ModelSource.MODELS_DEV),
        )
    }

    fun encodeModelsDev(snapshot: ModelsDevSnapshot, fetchedAt: Long): String {
        val providers = JSONObject()
        snapshot.providers.values.forEach { p ->
            providers.put(
                p.id,
                JSONObject()
                    .put("name", p.name)
                    .apply { p.api?.let { put("api", it) } }
                    .put("models", JSONArray(p.models.map(::encodeModel)))
            )
        }
        val index = JSONObject()
        snapshot.index.forEach { (key, model) -> index.put(key, encodeModel(model)) }
        return JSONObject()
            .put("version", VERSION)
            .put("fetchedAt", fetchedAt)
            .put("providers", providers)
            .put("index", index)
            .toString()
    }

    fun decodeModelsDev(text: String): Pair<ModelsDevSnapshot, Long>? {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return null
        if (root.optInt("version") != VERSION) return null
        val providers = LinkedHashMap<String, ModelsDevProvider>()
        root.optJSONObject("providers")?.let { all ->
            for (id in all.keys()) {
                val p = all.optJSONObject(id) ?: continue
                providers[id] = ModelsDevProvider(
                    id = id,
                    name = p.optString("name", id),
                    api = p.optString("api").ifBlank { null },
                    models = models(p.optJSONArray("models")),
                )
            }
        }
        val index = HashMap<String, ModelInfo>()
        root.optJSONObject("index")?.let { all ->
            for (key in all.keys()) all.optJSONObject(key)?.let(::decodeModel)?.let { index[key] = it }
        }
        return ModelsDevSnapshot(providers, index) to root.optLong("fetchedAt")
    }

    fun encodeLive(live: Map<String, LiveList>): String {
        val providers = JSONObject()
        live.forEach { (id, list) ->
            providers.put(
                id,
                JSONObject()
                    .put("fetchedAt", list.fetchedAt)
                    .put("models", JSONArray(list.models.map(::encodeModel)))
            )
        }
        return JSONObject().put("version", VERSION).put("providers", providers).toString()
    }

    fun decodeLive(text: String): Map<String, LiveList> {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return emptyMap()
        if (root.optInt("version") != VERSION) return emptyMap()
        val all = root.optJSONObject("providers") ?: return emptyMap()
        return all.keys().asSequence().mapNotNull { id ->
            val p = all.optJSONObject(id) ?: return@mapNotNull null
            id to LiveList(models(p.optJSONArray("models")), p.optLong("fetchedAt"))
        }.toMap()
    }

    private fun models(array: JSONArray?): List<ModelInfo> =
        if (array == null) emptyList()
        else (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(::decodeModel) }

    private fun strings(array: JSONArray): List<String> =
        (0 until array.length()).mapNotNull { array.opt(it) as? String }
}
