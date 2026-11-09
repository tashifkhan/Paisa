package codes.tashif.paisa.ai.provider

import codes.tashif.paisa.ai.LlmException
import codes.tashif.paisa.ai.LlmFailure
import codes.tashif.paisa.ai.catalog.ModelInfo
import codes.tashif.paisa.ai.json.JSONArray
import codes.tashif.paisa.ai.json.JSONObject

/** Gemini generateContent, for Google and compatible servers. */
object GeminiStyle {

    /** Gemini 2.x takes a token budget; 3.x and later take a named level. */
    enum class ThinkingMode { BUDGET, LEVEL }

    fun generateUrl(base: String, model: String): String =
        "$base/models/${model.removePrefix("models/")}:generateContent"

    fun modelsUrl(base: String): String = "$base/models?pageSize=1000"

    /** The key travels in a header, never in the URL where it would reach logs. */
    fun headers(apiKey: String): Map<String, String> = mapOf(
        "x-goog-api-key" to apiKey,
        "Content-Type" to "application/json",
    )

    /** The major version in `gemini-2.5-flash`, or null for other names. */
    internal fun majorVersion(model: String): Int? =
        Regex("gemini-(\\d+)").find(model.lowercase())?.groupValues?.get(1)?.toIntOrNull()

    /**
     * Catalogue first (effort values mean levels, a budget option means
     * budgets), then the version in the id, then levels.
     */
    fun thinkingMode(model: String, info: ModelInfo?): ThinkingMode {
        if (info != null) {
            val listsEfforts = info.reasoningLevels.orEmpty().any { it != ThinkingLevel.NONE }
            if (info.budgetThinking && !listsEfforts) return ThinkingMode.BUDGET
            if (listsEfforts) return ThinkingMode.LEVEL
        }
        val major = majorVersion(model) ?: return ThinkingMode.LEVEL
        return if (major <= 2) ThinkingMode.BUDGET else ThinkingMode.LEVEL
    }

    /** Token budget per level for 2.x models, clamped to the model's bounds. */
    fun thinkingBudget(model: String, level: ThinkingLevel, info: ModelInfo?): Int {
        val pro = model.lowercase().contains("pro")
        val max = info?.budgetMax ?: if (pro) 32_768 else 24_576
        val min = info?.budgetMin ?: if (pro) 128 else 0
        val wanted = when (level) {
            ThinkingLevel.NONE -> 0
            ThinkingLevel.MINIMAL -> 512
            ThinkingLevel.LOW -> 1_024
            ThinkingLevel.MEDIUM -> 8_192
            ThinkingLevel.HIGH -> 24_576
            ThinkingLevel.XHIGH, ThinkingLevel.MAX -> 32_768
        }
        // 0 turns thinking off where the model allows it (a toggle, or a 0 minimum).
        val canTurnOff = min == 0 || info?.reasoningLevels?.contains(ThinkingLevel.NONE) == true
        if (wanted == 0 && canTurnOff) return 0
        return wanted.coerceIn(maxOf(min, 1), max)
    }

    /** `thinkingLevel` for 3.x. These models can't switch thinking fully off. */
    fun thinkingLevelValue(level: ThinkingLevel): String = when (level) {
        ThinkingLevel.NONE, ThinkingLevel.MINIMAL -> "minimal"
        ThinkingLevel.LOW -> "low"
        ThinkingLevel.MEDIUM -> "medium"
        ThinkingLevel.HIGH, ThinkingLevel.XHIGH, ThinkingLevel.MAX -> "high"
    }

    fun thinkingConfig(call: LlmCall): JSONObject? {
        val level = call.thinking ?: return null
        return when (thinkingMode(call.model, call.modelInfo)) {
            ThinkingMode.BUDGET ->
                JSONObject().put("thinkingBudget", thinkingBudget(call.model, level, call.modelInfo))
            ThinkingMode.LEVEL ->
                JSONObject().put("thinkingLevel", thinkingLevelValue(level))
        }
    }

    fun build(call: LlmCall, request: LlmRequest): HttpSpec {
        val parts = JSONArray().put(JSONObject().put("text", request.prompt))
        request.image?.let { image ->
            parts.put(inline(image.mimeType, image.base64Data))
        }
        request.pdf?.let { pdf -> parts.put(inline("application/pdf", pdf.base64Data)) }

        val generation = JSONObject()
        // Gemini 3 is tuned for its default temperature; lower values can loop.
        val major = majorVersion(call.model)
        if (major == null || major <= 2) generation.put("temperature", LOW_TEMPERATURE)
        if (request.json) generation.put("responseMimeType", "application/json")
        thinkingConfig(call)?.let { generation.put("thinkingConfig", it) }

        val body = JSONObject()
            .put(
                "system_instruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", request.system)))
            )
            .put(
                "contents",
                JSONArray().put(JSONObject().put("role", "user").put("parts", parts))
            )
            .put("generationConfig", generation)
        return HttpSpec(generateUrl(call.base, call.model), headers(call.apiKey), body)
    }

    private fun inline(mimeType: String, data: String): JSONObject = JSONObject().put(
        "inline_data",
        JSONObject().put("mime_type", mimeType).put("data", data)
    )

    fun parse(text: String): String {
        val json = JSONObject(text)
        json.optJSONObject("promptFeedback")
            ?.optString("blockReason")
            ?.takeIf { it.isNotBlank() }
            ?.let { reason -> throw LlmException("Gemini blocked the request ($reason).", LlmFailure.EMPTY) }
        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            throw LlmException("No reply from the model", LlmFailure.EMPTY)
        }
        val candidate = candidates.getJSONObject(0)
        if (candidate.optString("finishReason") == "MAX_TOKENS") {
            throw LlmException(CUT_OFF_MESSAGE, LlmFailure.CUT_OFF)
        }
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts") ?: return ""
        return buildString {
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                if (part.optBoolean("thought", false)) continue
                if (part.has("text")) append(part.optString("text"))
            }
        }
    }
}
