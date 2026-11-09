package codes.tashif.paisa.backup

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine

/** The engine for Google Drive and Nextcloud: 30 s to connect, 120 s to read or write on Android. */
internal expect fun backupHttpEngine(): HttpClientEngine

/** Shared clients. Errors come back as responses so each store can explain them. */
internal object BackupHttp {
    val client: HttpClient by lazy {
        HttpClient(backupHttpEngine()) { expectSuccess = false }
    }

    /** For Nextcloud's sign-in poll: a redirect would carry the token to another host. */
    val noRedirects: HttpClient by lazy {
        HttpClient(backupHttpEngine()) {
            expectSuccess = false
            followRedirects = false
        }
    }
}
