package codes.tashif.paisa.backup

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

internal actual fun backupHttpEngine(): HttpClientEngine = OkHttp.create {
    config {
        connectTimeout(30, TimeUnit.SECONDS)
        readTimeout(120, TimeUnit.SECONDS)
        writeTimeout(120, TimeUnit.SECONDS)
        // Ktor decides redirects itself, per client.
        followRedirects(false)
        followSslRedirects(false)
    }
}
