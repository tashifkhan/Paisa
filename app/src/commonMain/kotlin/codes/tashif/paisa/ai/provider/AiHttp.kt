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
