package codes.tashif.paisa.ai.provider

import codes.tashif.paisa.ai.LlmException
import codes.tashif.paisa.ai.LlmFailure
import codes.tashif.paisa.ai.json.JSONException
import codes.tashif.paisa.ai.json.JSONObject
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLParserException
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.io.IOException

/** Something that runs one model call. The ladder runner only needs this. */
fun interface ModelCaller {
    suspend fun complete(call: LlmCall, request: LlmRequest): String
}

/**
 * Sends one call to one model over Ktor. A 429 or 5xx gets at most one
 * retry on the same model, so a ladder moves on quickly.
 */
class ProviderClient() : ModelCaller {

    /** Tests swap this for a client built by [client] on a stub engine. */
    internal var http: HttpClient = client(aiHttpEngine())

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

    private suspend fun execute(spec: HttpSpec, timeoutSeconds: Long? = null): Reply {
        directFetch(spec.url, spec.headers, spec.body?.toString(), timeoutSeconds ?: CALL_TIMEOUT_SECONDS)?.let {
            checkRedirect(it.status)
            return Reply(it.status, it.body)
        }
        val response = try {
            send(spec, timeoutSeconds ?: CALL_TIMEOUT_SECONDS)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) {
            throw LlmException("timed out", LlmFailure.TIMEOUT, cause = e)
        } catch (e: ConnectTimeoutException) {
            throw LlmException("timed out", LlmFailure.TIMEOUT, cause = e)
        } catch (e: SocketTimeoutException) {
            throw LlmException("timed out", LlmFailure.TIMEOUT, cause = e)
        } catch (e: IOException) {
            if (e.isPlatformTimeout()) throw LlmException("timed out", LlmFailure.TIMEOUT, cause = e)
            throw networkError(e)
        } catch (e: IllegalArgumentException) {
            // Never echo e.message: the client can quote a bad header value, which may be the key.
            throw LlmException("the URL or key has characters that can't be sent", LlmFailure.CONFIG)
        } catch (e: URLParserException) {
            throw LlmException("the URL or key has characters that can't be sent", LlmFailure.CONFIG)
        } catch (e: Exception) {
            if (e.isPlatformNetworkError()) throw networkError(e)
            throw e
        }
        val status = response.status.value
        checkRedirect(status)
        val body = try {
            response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) {
            throw LlmException("timed out", LlmFailure.TIMEOUT, cause = e)
        } catch (e: SocketTimeoutException) {
            throw LlmException("timed out", LlmFailure.TIMEOUT, cause = e)
        } catch (e: IOException) {
            if (e.isPlatformTimeout()) throw LlmException("timed out", LlmFailure.TIMEOUT, cause = e)
            throw networkError(e)
        }
        return Reply(status, body)
    }

    /**
     * Never follow redirects: the client would carry the key headers to
     * whatever host the server names, past the base URL check. A browser
     * reports a redirect it was told not to follow as status 0.
     */
    private fun checkRedirect(status: Int) {
        if (status in 300..399 || status == 0) {
            throw LlmException(
                "the server redirected to another address (HTTP $status). " +
                    "Paisa doesn't follow redirects with your key. Check the base URL",
                LlmFailure.HTTP,
                status
            )
        }
    }

    private suspend fun send(spec: HttpSpec, timeoutSeconds: Long): HttpResponse {
        val configure: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {
            timeout { requestTimeoutMillis = timeoutSeconds * 1_000 }
            spec.headers.forEach { (name, value) ->
                // The body carries the content type, as OkHttp did; a GET has none.
                if (!name.equals(HttpHeaders.ContentType, ignoreCase = true)) header(name, value)
            }
            spec.body?.let { setBody(TextContent(it.toString(), JSON_UTF8)) }
        }
        return if (spec.body != null) http.post(spec.url, configure) else http.get(spec.url, configure)
    }

    private fun networkError(e: Throwable) = LlmException(
        "couldn't connect (${e.message ?: e::class.simpleName})",
        LlmFailure.NETWORK,
        cause = e
    )

    companion object {
        private val JSON_UTF8 = ContentType.Application.Json.withCharset(Charsets.UTF_8)
        const val MAX_RETRIES = 1
        private const val RETRY_DELAY_MS = 1_500L

        /** The whole call, unless a caller asks for less. Android's engine also sets connect, read and write limits. */
        private const val CALL_TIMEOUT_SECONDS = 180L

        /** A client on [engine] that never follows redirects and never throws on an error status. */
        internal fun client(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
            followRedirects = false
            expectSuccess = false
            install(HttpTimeout)
        }

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
