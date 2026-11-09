package codes.tashif.paisa.ai.provider

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

/** OkHttp with the timeouts the AI client always used, and no redirects. */
internal actual fun aiHttpEngine(): HttpClientEngine = OkHttp.create {
    preconfigured = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
    config {
        followRedirects(false)
        followSslRedirects(false)
    }
}

/** OkHttp reports read, write and call timeouts as InterruptedIOException. */
internal actual fun Throwable.isPlatformTimeout(): Boolean = this is InterruptedIOException

/** On Android every connection failure is an IOException, which the client handles already. */
internal actual fun Throwable.isPlatformNetworkError(): Boolean = false
