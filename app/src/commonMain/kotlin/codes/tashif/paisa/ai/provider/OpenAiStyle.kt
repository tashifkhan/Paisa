package codes.tashif.paisa.ai.provider

import codes.tashif.paisa.ai.LlmException
import codes.tashif.paisa.ai.LlmFailure
import codes.tashif.paisa.ai.json.JSONArray
import codes.tashif.paisa.ai.json.JSONObject

/** OpenAI Chat Completions, for api.openai.com and every compatible server. */
object OpenAiStyle {

    fun chatUrl(base: String): String =
        if (base.endsWith("/chat/completions")) base else "$base/chat/completions"

    fun modelsUrl(base: String): String = "${base.removeSuffix("/chat/completions")}/models"

    fun headers(apiKey: String): Map<String, String> = mapOf(
        "Authorization" to "Bearer $apiKey",
        "Content-Type" to "application/json",
    )

    /**
     * The `reasoning_effort` value, or null to leave the provider default.
     * On api.openai.com, Max goes out as max only when the catalogue lists it
     * for the model. Otherwise it becomes the highest listed level, or xhigh
     * when the catalogue lists none.
     */
    fun reasoningEffort(call: LlmCall): String? {
        val level = call.thinking ?: return null
        if (!call.provider.nativeOpenAi || level != ThinkingLevel.MAX) return level.wire
        val listed = call.modelInfo?.reasoningLevels.orEmpty()
        if (ThinkingLevel.MAX in listed) return ThinkingLevel.MAX.wire
        val nearest = listed.filter { it != ThinkingLevel.NONE }.maxOrNull() ?: ThinkingLevel.XHIGH
        return nearest.wire
    }

    /** Reasoning models reject temperature, so it goes only to models known not to reason. */
    fun sendsTemperature(call: LlmCall): Boolean {
        if (call.thinking != null) return false
        if (call.modelInfo?.reasoning == true) return false
        if (call.provider.nativeOpenAi && looksLikeOpenAiReasoner(call.model)) return false
        return true
    }

    /** o1, o3, o4-mini, gpt-5 and later, for when the catalogue hasn't loaded. */
    internal fun looksLikeOpenAiReasoner(model: String): Boolean {
        val id = model.lowercase()
        if (Regex("^o\\d").containsMatchIn(id)) return true
        val major = Regex("^gpt-(\\d+)").find(id)?.groupValues?.get(1)?.toIntOrNull()
        return major != null && major >= 5
    }

    fun build(call: LlmCall, request: LlmRequest, jsonMode: Boolean = request.json): HttpSpec {
        val userContent: Any = if (request.image == null && request.audio == null) {
            request.prompt
        } else {
            val parts = JSONArray().put(JSONObject().put("type", "text").put("text", request.prompt))
            request.image?.let { image ->
                parts.put(
                    JSONObject()
                        .put("type", "image_url")
                        .put(
                            "image_url",
                            JSONObject().put("url", "data:${image.mimeType};base64,${image.base64Data}")
                        )
                )
            }
            request.audio?.let { audio ->
                parts.put(
                    JSONObject()
                        .put("type", "input_audio")
                        .put(
                            "input_audio",
                            JSONObject().put("data", audio.base64Data).put("format", audio.format)
                        )
                )
            }
            parts
        }
        val body = JSONObject()
            .put("model", call.model)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", request.system))
                    .put(JSONObject().put("role", "user").put("content", userContent))
            )
        if (sendsTemperature(call)) body.put("temperature", LOW_TEMPERATURE)
        reasoningEffort(call)?.let { body.put("reasoning_effort", it) }
        if (jsonMode) body.put("response_format", JSONObject().put("type", "json_object"))
        return HttpSpec(chatUrl(call.base), headers(call.apiKey), body)
    }

    /** Some compatible servers reject JSON mode; those get one try without it. */
    fun rejectsJsonMode(status: Int, body: String): Boolean =
        status in 400..499 && body.contains("response_format", ignoreCase = true)

    fun parse(text: String): String {
        val json = JSONObject(text)
        val choices = json.optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            throw LlmException("No reply from the model", LlmFailure.EMPTY)
        }
        val choice = choices.getJSONObject(0)
        // A length-truncated reply is partial JSON; fail so the ladder moves on.
        if (choice.optString("finish_reason") == "length") {
            throw LlmException(CUT_OFF_MESSAGE, LlmFailure.CUT_OFF)
        }
        val content = choice.optJSONObject("message")?.opt("content")
        return when (content) {
            is String -> content
            is JSONArray -> buildString {
                for (i in 0 until content.length()) {
                    content.optJSONObject(i)?.optString("text")?.let(::append)
                }
            }
            else -> ""
        }
    }
}
