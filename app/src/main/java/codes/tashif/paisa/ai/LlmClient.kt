package codes.tashif.paisa.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Minimal multi-provider chat client for structured statement extraction.
 */
class LlmClient(
    private val http: OkHttpClient = defaultClient()
) {
    suspend fun complete(
        credentials: AiCredentials,
        systemPrompt: String,
        userPrompt: String,
        jsonOutput: Boolean = false,
        image: LlmImage? = null
    ): String = withContext(Dispatchers.IO) {
        require(credentials.isConfigured) { "AI credentials are not configured" }
        when (credentials.provider) {
            AiProvider.OPENAI_COMPATIBLE ->
                openAiCompatible(
                    credentials,
                    systemPrompt,
                    userPrompt,
                    jsonMode = jsonOutput,
                    image = image
                )
            AiProvider.GEMINI ->
                gemini(credentials, systemPrompt, userPrompt, jsonOutput, image)
            AiProvider.ANTHROPIC ->
                anthropic(credentials, systemPrompt, userPrompt, image)
        }
    }

    /** Lightweight connectivity check — one short completion. */
    suspend fun testConnection(credentials: AiCredentials): Result<String> = runCatching {
        complete(
            credentials = credentials,
            systemPrompt = "Reply with exactly: OK",
            userPrompt = "ping"
        ).trim().take(200)
    }

    private fun openAiCompatible(
        credentials: AiCredentials,
        systemPrompt: String,
        userPrompt: String,
        jsonMode: Boolean,
        image: LlmImage?
    ): String {
        val base = credentials.baseUrl.trimEnd('/')
        val url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        val userContent: Any = if (image == null) {
            userPrompt
        } else {
            JSONArray()
                .put(JSONObject().put("type", "text").put("text", userPrompt))
                .put(
                    JSONObject()
                        .put("type", "image_url")
                        .put(
                            "image_url",
                            JSONObject().put(
                                "url",
                                "data:${image.mimeType};base64,${image.base64Data}"
                            )
                        )
                )
        }
        val body = JSONObject()
            .put("model", credentials.model)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemPrompt))
                    .put(JSONObject().put("role", "user").put("content", userContent))
            )
            .put("temperature", 0.1)
            .apply {
                if (jsonMode) {
                    put("response_format", JSONObject().put("type", "json_object"))
                }
            }
            .toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${credentials.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(JSON_MEDIA))
            .build()

        return http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                // Some OpenAI-compatible endpoints don't support JSON mode — retry once without.
                if (jsonMode && response.code in 400..499 &&
                    text.contains("response_format", ignoreCase = true)
                ) {
                    return openAiCompatible(
                        credentials,
                        systemPrompt,
                        userPrompt,
                        jsonMode = false,
                        image = image
                    )
                }
                throw LlmException("OpenAI-compatible error ${response.code}: ${text.take(400)}")
            }
            val json = JSONObject(text)
            val choice = json.getJSONArray("choices").getJSONObject(0)
            // A length-truncated reply yields unparseable partial JSON — fail loudly
            // instead of silently dropping that chunk's transactions.
            if (choice.optString("finish_reason") == "length") {
                throw LlmException(
                    "Model reply was cut off (max tokens). Try a smaller file or a model " +
                        "with a larger output limit."
                )
            }
            choice.getJSONObject("message").getString("content")
        }
    }

    private fun gemini(
        credentials: AiCredentials,
        systemPrompt: String,
        userPrompt: String,
        jsonMode: Boolean,
        image: LlmImage?
    ): String {
        val base = credentials.baseUrl.ifBlank { AiProvider.GEMINI.defaultBaseUrl }.trimEnd('/')
        val model = credentials.model
        // API key travels in the header — never in the URL, where it would leak into logs.
        val url = "$base/models/$model:generateContent"
        val parts = JSONArray().put(JSONObject().put("text", userPrompt))
        if (image != null) {
            parts.put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", image.mimeType)
                        .put("data", image.base64Data)
                )
            )
        }
        val body = JSONObject()
            .put(
                "system_instruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", systemPrompt))
                )
            )
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put(
                            "parts",
                            parts
                        )
                )
            )
            .put(
                "generationConfig",
                JSONObject().put("temperature", 0.1).apply {
                    if (jsonMode) put("responseMimeType", "application/json")
                }
            )
            .toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", credentials.apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(JSON_MEDIA))
            .build()

        return http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw LlmException("Gemini error ${response.code}: ${text.take(400)}")
            }
            val json = JSONObject(text)
            json.optJSONObject("promptFeedback")
                ?.optString("blockReason")
                ?.takeIf { it.isNotBlank() }
                ?.let { reason ->
                    throw LlmException("Gemini blocked the request ($reason).")
                }
            val candidate = json.getJSONArray("candidates").getJSONObject(0)
            if (candidate.optString("finishReason") == "MAX_TOKENS") {
                throw LlmException(
                    "Model reply was cut off (max tokens). Try a smaller file or a model " +
                        "with a larger output limit."
                )
            }
            candidate.getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }

    private fun anthropic(
        credentials: AiCredentials,
        systemPrompt: String,
        userPrompt: String,
        image: LlmImage?
    ): String {
        val base = credentials.baseUrl.ifBlank { AiProvider.ANTHROPIC.defaultBaseUrl }.trimEnd('/')
        // Tolerate users pasting a base URL that already ends in /v1.
        val url = if (base.endsWith("/v1")) "$base/messages" else "$base/v1/messages"
        val userContent: Any = if (image == null) {
            userPrompt
        } else {
            JSONArray()
                .put(
                    JSONObject()
                        .put("type", "image")
                        .put(
                            "source",
                            JSONObject()
                                .put("type", "base64")
                                .put("media_type", image.mimeType)
                                .put("data", image.base64Data)
                        )
                )
                .put(JSONObject().put("type", "text").put("text", userPrompt))
        }
        val body = JSONObject()
            .put("model", credentials.model)
            .put("max_tokens", 8192)
            .put("system", systemPrompt)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject().put("role", "user").put("content", userContent)
                )
            )
            .put("temperature", 0.1)
            .toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", credentials.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(JSON_MEDIA))
            .build()

        return http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw LlmException("Anthropic error ${response.code}: ${text.take(400)}")
            }
            val json = JSONObject(text)
            if (json.optString("stop_reason") == "max_tokens") {
                throw LlmException(
                    "Model reply was cut off (max tokens). Try a smaller file or a model " +
                        "with a larger output limit."
                )
            }
            val content = json.getJSONArray("content")
            val parts = StringBuilder()
            for (i in 0 until content.length()) {
                val part = content.getJSONObject(i)
                if (part.optString("type") == "text") {
                    parts.append(part.getString("text"))
                }
            }
            parts.toString()
        }
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}

data class LlmImage(
    val mimeType: String,
    val base64Data: String
)

class LlmException(message: String) : Exception(message)
