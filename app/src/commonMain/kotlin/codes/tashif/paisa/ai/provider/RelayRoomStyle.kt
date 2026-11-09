package codes.tashif.paisa.ai.provider

import codes.tashif.paisa.ai.LlmException
import codes.tashif.paisa.ai.LlmFailure
import codes.tashif.paisa.ai.catalog.ModelInfo
import codes.tashif.paisa.ai.json.JSONArray
import codes.tashif.paisa.ai.json.JSONObject

/**
 * RelayRoom's unified API (`POST {base}/generate`). Models are stored as
 * `<provider>/<model>` or as an alias with no slash.
 */
object RelayRoomStyle {

    fun url(base: String, path: String): String = "$base/${path.trimStart('/')}"

    fun headers(apiKey: String): Map<String, String> = mapOf(
        "Authorization" to "Bearer $apiKey",
        "Accept" to "application/json",
        "Content-Type" to "application/json",
        "X-Client" to "paisa",
    )

    /** Splits a stored id into the request's provider and model. Aliases have no provider. */
    fun target(id: String): Pair<String?, String> {
        val slash = id.indexOf('/')
        if (slash <= 0) return null to id
        return id.substring(0, slash) to id.substring(slash + 1)
    }

    fun build(call: LlmCall, request: LlmRequest): HttpSpec {
        val (provider, model) = target(call.model)
        val body = JSONObject()
        if (provider != null) body.put("provider", provider)
        body.put("model", model)
        // Auto is the gateway default, so it stays out of the body.
        if (call.routeMode != RouteMode.AUTO) body.put("mode", call.routeMode.wire)
        body.put("system", request.system)
        body.put("prompt", request.prompt)

        val attachments = JSONArray()
        request.image?.let { image ->
            attachments.put(
                JSONObject()
                    .put("kind", "image")
                    .put("mime_type", image.mimeType)
                    .put("data", "data:${image.mimeType};base64,${image.base64Data}")
            )
        }
        request.pdf?.let { pdf ->
            attachments.put(
                JSONObject()
                    .put("kind", "pdf")
                    .put("mime_type", "application/pdf")
                    .apply { pdf.name?.let { put("name", it) } }
                    .put("data", "data:application/pdf;base64,${pdf.base64Data}")
            )
        }
        if (attachments.length() > 0) body.put("attachments", attachments)

        call.thinking?.let { body.put("config", reasoningConfig(it, call.modelInfo)) }
        body.put("output", JSONObject().put("format", if (request.json) "json" else "text"))
        body.put("stream", false)
        val tags = JSONObject().put("app", "paisa")
        if (request.feature.isNotBlank()) tags.put("feature", request.feature)
        body.put("tags", tags)
        return HttpSpec(url(call.base, "generate"), headers(call.apiKey), body)
    }

    /**
     * The one thinking control RelayRoom accepts for this route, from its
     * `/model-options` answer (docs/UNIFIED-API.md):
     * - a listed level goes in `reasoning` (the gateway rejects others with a 400);
     * - None is `reasoning_enabled: false` when thinking can be switched off,
     *   or a zero budget on a budget route that allows it;
     * - a budget route without named levels takes `reasoning_budget_tokens`,
     *   scaled within its published range;
     * - with nothing known about the route, the level goes through as is.
     */
    fun reasoningConfig(level: ThinkingLevel, info: ModelInfo?): JSONObject {
        val listed = info?.reasoningLevels.orEmpty()
        val config = JSONObject()
        when {
            level == ThinkingLevel.NONE && ThinkingLevel.NONE in listed -> config.put("reasoning_enabled", false)
            level == ThinkingLevel.NONE && info?.budgetThinking == true && info.budgetMin == 0 ->
                config.put("reasoning_budget_tokens", 0)
            level in listed -> config.put("reasoning", level.wire)
            info?.budgetThinking == true -> config.put("reasoning_budget_tokens", budgetFor(level, info))
            else -> config.put("reasoning", level.wire)
        }
        return config
    }

    /** Tokens for [level] on a budget route: a share of its maximum, kept inside its bounds. */
    internal fun budgetFor(level: ThinkingLevel, info: ModelInfo): Int {
        val max = info.budgetMax
        val tokens = if (max != null && max > 0) {
            val share = when (level) {
                ThinkingLevel.NONE -> 0.0
                ThinkingLevel.MINIMAL -> 0.1
                ThinkingLevel.LOW -> 0.25
                ThinkingLevel.MEDIUM -> 0.5
                ThinkingLevel.HIGH -> 0.75
                ThinkingLevel.XHIGH -> 0.9
                ThinkingLevel.MAX -> 1.0
            }
            (max * share).toInt().coerceAtLeast(minOf(1024, max))
        } else {
            when (level) {
                ThinkingLevel.NONE -> 0
                ThinkingLevel.MINIMAL -> 1024
                ThinkingLevel.LOW -> 4096
                ThinkingLevel.MEDIUM -> 8192
                ThinkingLevel.HIGH -> 16384
                ThinkingLevel.XHIGH -> 24576
                ThinkingLevel.MAX -> 32768
            }
        }
        val min = info.budgetMin ?: 0
        return if (max != null && max > 0) tokens.coerceIn(min, max) else tokens.coerceAtLeast(min)
    }

    /** `{output: {json}}` comes back as JSON text; otherwise `output.text`. */
    fun parse(text: String): String {
        val output = JSONObject(text).optJSONObject("output")
            ?: throw LlmException("RelayRoom sent no output", LlmFailure.EMPTY)
        val json = output.opt("json")
        if (json != null && json != JSONObject.NULL) return json.toString()
        return output.optString("text", "")
    }

    private val ERROR_HINTS = mapOf(
        "mode_not_supported" to "That provider has no route for the chosen mode. Switch the model to Auto.",
        "mode_not_permitted" to "Your gateway key isn't allowed that mode. Switch the model to Auto.",
        "provider_blocked" to "Your gateway key is blocked from that provider.",
        "model_not_found" to "RelayRoom has no live route for that model. Pick another one.",
        "model_not_routed" to "RelayRoom has no live route for that model. Pick another one.",
        "provider_not_found" to "RelayRoom doesn't know that provider.",
        "alias_unavailable" to "None of the alias's steps can serve your gateway key right now.",
    )

    /** The relay's `{error: {type, message}}` shape, in words a person can act on. */
    fun errorMessage(status: Int, body: String): String {
        if (status == 401) {
            return "RelayRoom rejected the gateway key. Paste a key from the console under API keys."
        }
        val error = runCatching { JSONObject(body).optJSONObject("error") }.getOrNull()
        val type = error?.optString("type")?.takeIf { it.isNotBlank() }
        val message = error?.optString("message")?.takeIf { it.isNotBlank() }
            ?: "RelayRoom returned $status"
        val hint = type?.let(ERROR_HINTS::get)
        return if (hint != null) "$hint ($message)" else message
    }
}
