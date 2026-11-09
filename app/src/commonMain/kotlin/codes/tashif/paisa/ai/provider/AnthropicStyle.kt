package codes.tashif.paisa.ai.provider

import codes.tashif.paisa.ai.LlmException
import codes.tashif.paisa.ai.LlmFailure
import codes.tashif.paisa.ai.json.JSONArray
import codes.tashif.paisa.ai.json.JSONObject

/**
 * Anthropic Messages, for anthropic.com and compatible servers.
 *
 * Thinking follows RelayRoom's thinking-spec.md: None sends
 * `thinking: disabled`, Auto sends nothing, and every other level sends
 * adaptive thinking with `output_config.effort`. `budget_tokens` is never sent.
 */
object AnthropicStyle {
    const val VERSION = "2023-06-01"

    /** Room for a statement chunk's JSON with no thinking. */
    const val MAX_TOKENS = 8_192

    /** Thinking shares max_tokens with the reply, so it gets more. */
    const val THINKING_MAX_TOKENS = 32_000

    fun messagesUrl(base: String): String =
        if (base.endsWith("/v1")) "$base/messages" else "$base/v1/messages"

    fun modelsUrl(base: String): String =
        if (base.endsWith("/v1")) "$base/models" else "$base/v1/models"

    fun headers(apiKey: String): Map<String, String> = mapOf(
        "x-api-key" to apiKey,
        "anthropic-version" to VERSION,
        "Content-Type" to "application/json",
    )

    fun thinkingOn(level: ThinkingLevel?): Boolean = level != null && level != ThinkingLevel.NONE

    /** `output_config.effort` for a level. Minimal has no match, so it becomes low. */
    fun effort(level: ThinkingLevel): String = when (level) {
        ThinkingLevel.MINIMAL -> "low"
        else -> level.wire
    }

    /** More tokens while thinking, never above what the model can write. */
    fun maxTokens(level: ThinkingLevel?, outputLimit: Int?): Int {
        val wanted = if (thinkingOn(level)) THINKING_MAX_TOKENS else MAX_TOKENS
        return if (outputLimit != null && outputLimit > 0) minOf(wanted, outputLimit) else wanted
    }

    fun build(call: LlmCall, request: LlmRequest): HttpSpec {
        val content = JSONArray()
        request.image?.let { image ->
            content.put(
                JSONObject()
                    .put("type", "image")
                    .put("source", base64Source(image.mimeType, image.base64Data))
            )
        }
        request.pdf?.let { pdf ->
            content.put(
                JSONObject()
                    .put("type", "document")
                    .put("source", base64Source("application/pdf", pdf.base64Data))
            )
        }
        content.put(JSONObject().put("type", "text").put("text", request.prompt))

        val level = call.thinking
        val body = JSONObject()
            .put("model", call.model)
            .put("max_tokens", maxTokens(level, call.modelInfo?.outputLimit))
            .put("system", request.system)
            .put(
                "messages",
                JSONArray().put(JSONObject().put("role", "user").put("content", content))
            )
        when {
            level == null -> body.put("temperature", LOW_TEMPERATURE)
            level == ThinkingLevel.NONE -> {
                body.put("thinking", JSONObject().put("type", "disabled"))
                body.put("temperature", LOW_TEMPERATURE)
            }
            else -> {
                // Temperature is rejected while thinking is on.
                body.put("thinking", JSONObject().put("type", "adaptive"))
                mergeOutputConfig(body, JSONObject().put("effort", effort(level)))
            }
        }
        return HttpSpec(messagesUrl(call.base), headers(call.apiKey), body)
    }

    /** Merges into an existing `output_config` instead of replacing it. */
    internal fun mergeOutputConfig(body: JSONObject, extra: JSONObject) {
        val config = body.optJSONObject("output_config") ?: JSONObject()
        for (key in extra.keys()) config.put(key, extra.get(key))
        body.put("output_config", config)
    }

    private fun base64Source(mediaType: String, data: String): JSONObject =
        JSONObject().put("type", "base64").put("media_type", mediaType).put("data", data)

    fun parse(text: String): String {
        val json = JSONObject(text)
        if (json.optString("stop_reason") == "max_tokens") {
            throw LlmException(CUT_OFF_MESSAGE, LlmFailure.CUT_OFF)
        }
        val content = json.optJSONArray("content") ?: return ""
        return buildString {
            for (i in 0 until content.length()) {
                val part = content.optJSONObject(i) ?: continue
                // Thinking blocks are skipped; only the answer counts.
                if (part.optString("type") == "text") append(part.optString("text"))
            }
        }
    }
}
