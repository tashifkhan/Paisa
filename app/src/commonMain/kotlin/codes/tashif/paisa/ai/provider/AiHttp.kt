package codes.tashif.paisa.ai.provider

import io.ktor.client.engine.HttpClientEngine

/**
 * The HTTP engine for AI calls. Android uses OkHttp with the connect, read,
 * write and call timeouts the app always had; the web uses the browser's fetch.
 * Neither follows redirects.
 */
internal expect fun aiHttpEngine(): HttpClientEngine

/** True for a platform exception that means the call ran out of time. */
internal expect fun Throwable.isPlatformTimeout(): Boolean

/**
 * True for a platform exception that means the server couldn't be reached
 * and isn't already a kotlinx.io IOException (a browser fetch failure, for example).
 */
internal expect fun Throwable.isPlatformNetworkError(): Boolean

/** What a [directFetch] call got back: the status and the body text. */
internal class DirectReply(val status: Int, val body: String)

/**
 * Sends the call without Ktor when the platform needs that. The web does:
 * Ktor's fetch engine on wasm leaves the call hanging when the browser
 * rejects it (CORS, offline), so the page sits on a spinner until the
 * timeout. Returns null to use Ktor. Throws LlmException for timeouts and
 * network failures. A redirect comes back as status 0.
 */
internal expect suspend fun directFetch(
    url: String,
    headers: Map<String, String>,
    body: String?,
    timeoutSeconds: Long,
): DirectReply?
