package codes.tashif.paisa.ai.store

import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.ai.ladder.Rung
import codes.tashif.paisa.ai.provider.ApiStyle
import codes.tashif.paisa.ai.provider.CustomProvider
import codes.tashif.paisa.ai.provider.RouteMode
import codes.tashif.paisa.ai.provider.ThinkingLevel
import codes.tashif.paisa.ai.json.JSONArray
import codes.tashif.paisa.ai.json.JSONObject

/**
 * Everything about AI except keys: custom providers, base URL overrides,
 * ladders and hand-added models. Kept as one JSON blob on this device only.
 */
data class AiConfig(
    val customProviders: List<CustomProvider> = emptyList(),
    val baseUrls: Map<String, String> = emptyMap(),
    val defaultLadder: List<Rung> = emptyList(),
    /** A service missing here uses the default ladder. */
    val serviceLadders: Map<AiService, List<Rung>> = emptyMap(),
    val manualModels: Map<String, List<String>> = emptyMap(),
    /** The old single-provider settings were moved over. */
    val legacyMigrated: Boolean = false,
)

object AiConfigCodec {
    private const val VERSION = 1
    private const val DEFAULT_KEY = "default"

    fun encode(config: AiConfig): String {
        val ladders = JSONObject().put(DEFAULT_KEY, rungs(config.defaultLadder))
        config.serviceLadders.forEach { (service, rungs) -> ladders.put(service.id, rungs(rungs)) }
        val manual = JSONObject()
        config.manualModels.forEach { (id, models) -> manual.put(id, JSONArray(models)) }
        return JSONObject()
            .put("version", VERSION)
            .put(
                "customProviders",
                JSONArray(
                    config.customProviders.map {
                        JSONObject()
                            .put("id", it.id)
                            .put("name", it.name)
                            .put("style", it.style.id)
                            .put("baseUrl", it.baseUrl)
                    }
                )
            )
            .put("baseUrls", JSONObject(config.baseUrls as Map<*, *>))
            .put("ladders", ladders)
            .put("manualModels", manual)
            .put("legacyMigrated", config.legacyMigrated)
            .toString()
    }

    /** Unknown or broken JSON gives an empty config rather than a crash. */
    fun decode(text: String?): AiConfig {
        if (text.isNullOrBlank()) return AiConfig()
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return AiConfig()
        val custom = objects(root.optJSONArray("customProviders")).mapNotNull { o ->
            val id = o.optString("id").ifBlank { return@mapNotNull null }
            CustomProvider(
                id = id,
                name = o.optString("name").ifBlank { id },
                style = ApiStyle.fromId(o.optString("style")) ?: ApiStyle.OPENAI,
                baseUrl = o.optString("baseUrl"),
            )
        }
        val baseUrls = root.optJSONObject("baseUrls")?.let { o ->
            o.keys().asSequence().associateWith { o.optString(it) }.filterValues { it.isNotBlank() }
        }.orEmpty()
        val ladders = root.optJSONObject("ladders")
        val serviceLadders = AiService.entries.mapNotNull { service ->
            ladders?.optJSONArray(service.id)?.let { service to rungs(it) }
        }.filter { it.second.isNotEmpty() }.toMap()
        val manual = root.optJSONObject("manualModels")?.let { o ->
            o.keys().asSequence().associateWith { key -> strings(o.optJSONArray(key)) }
        }.orEmpty()
        return AiConfig(
            customProviders = custom,
            baseUrls = baseUrls,
            defaultLadder = ladders?.optJSONArray(DEFAULT_KEY)?.let(::rungs).orEmpty(),
            serviceLadders = serviceLadders,
            manualModels = manual,
            legacyMigrated = root.optBoolean("legacyMigrated", false),
        )
    }

    private fun rungs(list: List<Rung>): JSONArray = JSONArray(
        list.map { rung ->
            JSONObject()
                .put("provider", rung.providerId)
                .put("model", rung.modelId)
                .apply { rung.thinking?.let { put("thinking", it.wire) } }
                .put("mode", rung.routeMode.wire)
        }
    )

    private fun rungs(array: JSONArray): List<Rung> = objects(array).mapNotNull { o ->
        val provider = o.optString("provider").ifBlank { return@mapNotNull null }
        val model = o.optString("model").ifBlank { return@mapNotNull null }
        Rung(
            providerId = provider,
            modelId = model,
            thinking = ThinkingLevel.fromWire(o.optString("thinking")),
            routeMode = RouteMode.fromWire(o.optString("mode")),
        )
    }

    private fun objects(array: JSONArray?): List<JSONObject> =
        if (array == null) emptyList() else (0 until array.length()).mapNotNull(array::optJSONObject)

    private fun strings(array: JSONArray?): List<String> =
        if (array == null) emptyList()
        else (0 until array.length()).mapNotNull { array.opt(it) as? String }
}
