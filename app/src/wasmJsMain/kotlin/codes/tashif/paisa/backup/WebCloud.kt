package codes.tashif.paisa.backup

import codes.tashif.paisa.platform.nowMillis
import codes.tashif.paisa.ui.backup.BackupFile
import codes.tashif.paisa.ui.backup.BackupServiceHooks
import codes.tashif.paisa.ui.backup.downloadBytes
import codes.tashif.paisa.ui.backup.readBytes
import kotlinx.io.IOException

/**
 * Google access tokens in the browser. Google Identity Services gives no
 * refresh token to a browser app, so a token lasts about an hour; after that
 * the user taps Connect again. Kept in sessionStorage so a reload doesn't
 * lose it.
 */
object WebGoogleTokens : DriveTokens {
    private const val KEY = "paisa.google_token"
    private const val EXPIRY_KEY = "paisa.google_token_expiry"

    fun remember(token: String, expiresInSeconds: Int = 3500) {
        jsSessionSet(KEY, token)
        jsSessionSet(EXPIRY_KEY, (nowMillis() + expiresInSeconds * 1000L).toString())
    }

    fun current(): String? {
        val token = jsSessionGet(KEY) ?: return null
        val expiry = jsSessionGet(EXPIRY_KEY)?.toLongOrNull() ?: 0L
        return token.takeIf { nowMillis() < expiry - 60_000 }
    }

    fun forget() {
        jsSessionRemove(KEY)
        jsSessionRemove(EXPIRY_KEY)
    }

    override suspend fun token(): String =
        current() ?: throw BackupAuthException("Google Drive needs you to connect again in this browser session.")
}

/** Backup and sync in the browser. */
class WebCloudPlatform(private val prefs: BackupPrefs) : CloudPlatform {
    override val appVersion: String = "web"

    override val deviceName: String by lazy { browserDeviceName() }

    override fun storeFor(config: BackupConfig): BackupStore? = when (config.provider) {
        BackupProvider.GOOGLE_DRIVE -> GoogleDriveStore(WebGoogleTokens)
        BackupProvider.NEXTCLOUD -> config.nextcloud?.let { NextcloudStore(it, nextcloudTransport(prefs)) }
        BackupProvider.NONE -> null
    }

    override fun reschedule() = WebSyncScheduler.reschedule()

    override suspend fun onDataReplaced() = Unit
}

private const val K_PROXY = "nc_proxy"

/** Requests to Nextcloud go through the user's CORS proxy when one is set. */
internal fun nextcloudTransport(prefs: BackupPrefs): NextcloudTransport =
    prefs.store.getString(K_PROXY, null)?.takeIf { it.isNotBlank() }
        ?.let { NextcloudTransport.proxy(it) } ?: NextcloudTransport.Direct

class WebBackupHooks(private val prefs: BackupPrefs) : BackupServiceHooks {
    override suspend fun writeFile(file: BackupFile, bytes: ByteArray) =
        downloadBytes(file.name, bytes, BackupNames.MIME)

    override suspend fun readFile(file: BackupFile): ByteArray {
        val picked = file.file ?: throw IOException("Pick a backup file first")
        return readBytes(picked)
    }

    override suspend fun googleTokenForRevoke(): String? = WebGoogleTokens.current()

    override fun rememberGoogleToken(token: String) = WebGoogleTokens.remember(token)

    override fun forgetGoogle() = WebGoogleTokens.forget()

    override fun nextcloudTransport(): NextcloudTransport = nextcloudTransport(prefs)

    override val nextcloudNeedsProxy: Boolean = true

    override fun nextcloudProxy(): String? = prefs.store.getString(K_PROXY, null)

    override fun setNextcloudProxy(url: String?) {
        prefs.store.edit().putString(K_PROXY, url?.trim()?.trimEnd('/')?.ifBlank { null }).apply()
    }
}

/** "Chrome on macOS" and the like, for other devices' sync status. */
private fun browserDeviceName(): String {
    val ua = jsUserAgent()
    val browser = when {
        "Edg/" in ua -> "Edge"
        "Firefox/" in ua -> "Firefox"
        "Chrome/" in ua -> "Chrome"
        "Safari/" in ua -> "Safari"
        else -> "Browser"
    }
    val os = when {
        "Android" in ua -> "Android"
        "iPhone" in ua || "iPad" in ua -> "iOS"
        "Mac OS X" in ua || "Macintosh" in ua -> "macOS"
        "Windows" in ua -> "Windows"
        "Linux" in ua -> "Linux"
        else -> null
    }
    return if (os != null) "$browser on $os" else browser
}

private fun jsUserAgent(): String = js("navigator.userAgent")
private fun jsSessionGet(key: String): String? = js("sessionStorage.getItem(key)")
private fun jsSessionSet(key: String, value: String): Unit = js("sessionStorage.setItem(key, value)")
private fun jsSessionRemove(key: String): Unit = js("sessionStorage.removeItem(key)")
