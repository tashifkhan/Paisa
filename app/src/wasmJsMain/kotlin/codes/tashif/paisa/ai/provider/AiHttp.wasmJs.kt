package codes.tashif.paisa.ai.provider

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.await

/**
 * The browser's fetch. Only the whole-call timeout applies here; browsers
 * don't expose connect or read limits.
 */
internal actual fun aiHttpEngine(): HttpClientEngine = Js.create()

internal actual fun Throwable.isPlatformTimeout(): Boolean = false

/** A failed fetch (offline, DNS, CORS) reaches Kotlin as a plain exception. */
internal actual fun Throwable.isPlatformNetworkError(): Boolean = this !is CancellationException

internal actual suspend fun directFetch(
    url: String,
    headers: Map<String, String>,
    body: String?,
    timeoutSeconds: Long,
): DirectReply? {
    val withType = if (body != null && headers.keys.none { it.equals("Content-Type", ignoreCase = true) }) {
        headers + ("Content-Type" to "application/json; charset=utf-8")
    } else {
        headers
    }
    val headerJson = codes.tashif.paisa.ai.json.JSONObject(withType as Map<*, *>).toString()
    val raw = jsFetch(url, headerJson, body, (timeoutSeconds * 1000).toInt()).await<JsString>().toString()
    val result = codes.tashif.paisa.ai.json.JSONObject(raw)
    when (result.optString("error")) {
        "" -> return DirectReply(result.optInt("status"), result.optString("body"))
        "timeout" -> throw codes.tashif.paisa.ai.LlmException("timed out", codes.tashif.paisa.ai.LlmFailure.TIMEOUT)
        else -> throw codes.tashif.paisa.ai.LlmException(
            "couldn't connect (the browser blocked the request or the server is unreachable. " +
                "If the server works elsewhere, it may not allow browser requests)",
            codes.tashif.paisa.ai.LlmFailure.NETWORK
        )
    }
}

/**
 * fetch with a timeout and no redirects. Always resolves, to a JSON string:
 * {status, body} on a response, {error} when fetch rejects or times out.
 */
private fun jsFetch(url: String, headers: String, body: String?, timeoutMs: Int): kotlin.js.Promise<JsString> = js(
    """(() => {
        const ctrl = new AbortController();
        const timer = setTimeout(() => ctrl.abort(), timeoutMs);
        const init = { method: body === null ? 'GET' : 'POST', headers: JSON.parse(headers), redirect: 'manual', signal: ctrl.signal };
        if (body !== null) init.body = body;
        return fetch(url, init)
            .then(async (r) => {
                const text = r.type === 'opaqueredirect' ? '' : await r.text();
                clearTimeout(timer);
                return JSON.stringify({ status: r.type === 'opaqueredirect' ? 0 : r.status, body: text });
            })
            .catch((e) => {
                clearTimeout(timer);
                return JSON.stringify({ error: e && e.name === 'AbortError' ? 'timeout' : 'network' });
            });
    })()"""
)
