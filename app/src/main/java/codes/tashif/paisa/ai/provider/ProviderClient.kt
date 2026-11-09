package codes.tashif.paisa.ai.provider

import codes.tashif.paisa.ai.LlmException
import codes.tashif.paisa.ai.LlmFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Something that runs one model call. The ladder runner only needs this. */
fun interface ModelCaller {
    suspend fun complete(call: LlmCall, request: LlmRequest): String
}

/**
 * Sends one call to one model over OkHttp. A 429 or 5xx gets at most one
 * retry on the same model, so a ladder moves on quickly.
 */
@Singleton
class ProviderClient @Inject constructor() : ModelCaller {

    internal var http: OkHttpClient = defaultClient()

    override suspend fun complete(call: LlmCall, request: LlmRequest): String {
        BaseUrlSafety.check(call.baseUrl)
        if (call.apiKey.isBlank()) throw LlmException("No key saved for ${call.provider.name}")
        var attempt = 0
        while (true) {
            try {
                return completeOnce(call, request)
            } catch (e: LlmException) {
                if (!e.retryable || attempt >= MAX_RETRIES) throw e
                attempt++
                delay(RETRY_DELAY_MS)
            }
        }
    }

    private suspend fun completeOnce(call: LlmCall, request: LlmRequest): String =
        when (call.style) {
            ApiStyle.OPENAI -> {
                val first = execute(OpenAiStyle.build(call, request))
                val reply = if (request.json && !first.ok &&
                    OpenAiStyle.rejectsJsonMode(first.status, first.body)
                ) {
                    execute(OpenAiStyle.build(call, request, jsonMode = false))
                } else {
                    first
                }
                OpenAiStyle.parse(reply.orThrow(call))
            }
            ApiStyle.ANTHROPIC -> AnthropicStyle.parse(execute(AnthropicStyle.build(call, request)).orThrow(call))
            ApiStyle.GEMINI -> GeminiStyle.parse(execute(GeminiStyle.build(call, request)).orThrow(call))
            ApiStyle.RELAYROOM -> RelayRoomStyle.parse(execute(RelayRoomStyle.build(call, request)).orThrow(call))
        }

    /** A GET with the provider's auth headers, for model listing. Returns the body. */
    suspend fun get(url: String, headers: Map<String, String>, timeoutSeconds: Long? = null): String {
        val reply = execute(HttpSpec(url, headers), timeoutSeconds)
        if (!reply.ok) {
            throw LlmException("HTTP ${reply.status}: ${errorText(reply.body)}", LlmFailure.HTTP, reply.status)
        }
        return reply.body
    }

    internal class Reply(val status: Int, val body: String) {
        val ok: Boolean get() = status in 200..299
    }

    private fun Reply.orThrow(call: LlmCall): String {
        if (ok) return body
        val message = if (call.style == ApiStyle.RELAYROOM) {
            RelayRoomStyle.errorMessage(status, body)
        } else {
            "HTTP $status: ${errorText(body)}"
        }
        throw LlmException(message, LlmFailure.HTTP, status)
    }

    private suspend fun execute(spec: HttpSpec, timeoutSeconds: Long? = null): Reply =
        withContext(Dispatchers.IO) {
            // Never follow redirects: OkHttp would carry the key headers to
            // whatever host the server names, past the base URL check.
            val client = http.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .apply { if (timeoutSeconds != null) callTimeout(timeoutSeconds, TimeUnit.SECONDS) }
                .build()
            try {
                val builder = Request.Builder().url(spec.url)
                spec.headers.forEach { (name, value) -> builder.header(name, value) }
                if (spec.body != null) builder.post(spec.body.toString().toRequestBody(JSON_MEDIA))
                client.newCall(builder.build()).execute().use { response ->
                    if (response.code in 300..399) {
                        throw LlmException(
                            "the server redirected to another address (HTTP ${response.code}). " +
                                "Paisa doesn't follow redirects with your key. Check the base URL",
                            LlmFailure.HTTP,
                            response.code
                        )
                    }
                    Reply(response.code, response.body?.string().orEmpty())
                }
            } catch (e: InterruptedIOException) {
                throw LlmException("timed out", LlmFailure.TIMEOUT, cause = e)
            } catch (e: IOException) {
                throw LlmException(
                    "couldn't connect (${e.message ?: e.javaClass.simpleName})",
                    LlmFailure.NETWORK,
                    cause = e
                )
            } catch (e: IllegalArgumentException) {
                // Never echo e.message: OkHttp can quote a bad header value, which may be the key.
                throw LlmException("the URL or key has characters that can't be sent", LlmFailure.CONFIG)
            }
        }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        const val MAX_RETRIES = 1
        private const val RETRY_DELAY_MS = 1_500L

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        /** The `error.message` most providers return, or the start of the body. */
        fun errorText(body: String): String {
            val message = try {
                val json = JSONObject(body)
                json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: json.optString("message").takeIf { it.isNotBlank() }
            } catch (_: JSONException) {
                null
            }
            return (message ?: body).take(400)
        }
    }
}
