package codes.tashif.paisa.ai.provider

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import kotlinx.coroutines.CancellationException

/**
 * The browser's fetch. Only the whole-call timeout applies here; browsers
 * don't expose connect or read limits.
 */
internal actual fun aiHttpEngine(): HttpClientEngine = Js.create()

internal actual fun Throwable.isPlatformTimeout(): Boolean = false

/** A failed fetch (offline, DNS, CORS) reaches Kotlin as a plain exception. */
internal actual fun Throwable.isPlatformNetworkError(): Boolean = this !is CancellationException
