package codes.tashif.paisa.backup

import android.util.Xml
import codes.tashif.paisa.ai.provider.BaseUrlSafety
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import codes.tashif.paisa.sync.SyncFileNames
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Nextcloud over WebDAV. Backups live in `/Paisa` in the user's files, so
 * they are visible in the Nextcloud web UI and apps.
 */
class NextcloudStore(private val account: NextcloudAccount) : BackupStore {

    private val folder: HttpUrl = davRoot(account)

    override suspend fun list(): List<RemoteBackup> = withContext(Dispatchers.IO) {
        val body = """<?xml version="1.0"?>
            <d:propfind xmlns:d="DAV:"><d:prop><d:getcontentlength/><d:getlastmodified/></d:prop></d:propfind>"""
        val request = authed(folder)
            .method("PROPFIND", body.toRequestBody(XML))
            .header("Depth", "1")
            .build()
        http.newCall(request).execute().use { res ->
            if (res.code == 404) return@withContext emptyList()
            check(res)
            parsePropfind(res.body?.string().orEmpty(), BackupNames::isBackup)
        }
    }

    override suspend fun upload(name: String, bytes: ByteArray): RemoteBackup =
        withContext(Dispatchers.IO) {
            ensureFolder()
            val url = folder.newBuilder().addPathSegment(name).build()
            val request = authed(url).put(bytes.toRequestBody(BINARY)).build()
            http.newCall(request).execute().use { check(it) }
            RemoteBackup(
                id = name,
                name = name,
                sizeBytes = bytes.size.toLong(),
                createdAt = BackupNames.timeOf(name) ?: System.currentTimeMillis()
            )
        }

    override suspend fun download(backup: RemoteBackup): ByteArray = withContext(Dispatchers.IO) {
        val url = folder.newBuilder().addPathSegment(backup.name).build()
        http.newCall(authed(url).get().build()).execute().use { res ->
            check(res)
            res.body?.bytes() ?: throw IOException("Empty response from Nextcloud")
        }
    }

    override suspend fun delete(backup: RemoteBackup) = withContext(Dispatchers.IO) {
        val url = folder.newBuilder().addPathSegment(backup.name).build()
        http.newCall(authed(url).delete().build()).execute().use { res ->
            if (res.code != 404) check(res)
        }
    }

    // Sync files live in /Paisa/sync so they don't clutter the backup list.

    private val syncFolder: HttpUrl = folder.newBuilder().addPathSegment("sync").build()

    override suspend fun listSync(): List<RemoteBackup> = withContext(Dispatchers.IO) {
        val body = """<?xml version="1.0"?>
            <d:propfind xmlns:d="DAV:"><d:prop><d:getcontentlength/></d:prop></d:propfind>"""
        val request = authed(syncFolder)
            .method("PROPFIND", body.toRequestBody(XML))
            .header("Depth", "1")
            .build()
        http.newCall(request).execute().use { res ->
            if (res.code == 404) return@withContext emptyList()
            check(res)
            parsePropfind(res.body?.string().orEmpty(), SyncFileNames::isSyncFile)
        }
    }

    override suspend fun uploadSync(name: String, bytes: ByteArray): RemoteBackup =
        withContext(Dispatchers.IO) {
            ensureFolder()
            ensureFolder(syncFolder)
            val url = syncFolder.newBuilder().addPathSegment(name).build()
            http.newCall(authed(url).put(bytes.toRequestBody(BINARY)).build()).execute().use { check(it) }
            RemoteBackup(id = name, name = name, sizeBytes = bytes.size.toLong(), createdAt = 0L)
        }

    override suspend fun downloadSync(file: RemoteBackup): ByteArray = withContext(Dispatchers.IO) {
        val url = syncFolder.newBuilder().addPathSegment(file.name).build()
        http.newCall(authed(url).get().build()).execute().use { res ->
            check(res)
            res.body?.bytes() ?: throw IOException("Empty response from Nextcloud")
        }
    }

    override suspend fun deleteSync(file: RemoteBackup) = withContext(Dispatchers.IO) {
        val url = syncFolder.newBuilder().addPathSegment(file.name).build()
        http.newCall(authed(url).delete().build()).execute().use { res ->
            if (res.code != 404) check(res)
        }
    }

    private fun ensureFolder(url: HttpUrl = folder) {
        val request = authed(url).method("MKCOL", null).build()
        http.newCall(request).execute().use { res ->
            // 405 = already exists
            if (res.code != 405) check(res)
        }
    }

    /** Every request goes through here, so an account saved with plain http stops before sending the password. */
    private fun authed(url: HttpUrl) = Request.Builder()
        .also { requireSafeServer(account.serverUrl) }
        .url(url)
        .header("Authorization", Credentials.basic(account.loginName, account.appPassword))
        .header("User-Agent", USER_AGENT)

    private fun parsePropfind(xml: String, accept: (String) -> Boolean): List<RemoteBackup> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(xml.reader())
        val out = mutableListOf<RemoteBackup>()
        var href: String? = null
        var size = 0L
        var modified: Long? = null
        var text = StringBuilder()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    text = StringBuilder()
                    if (parser.name == "response") {
                        href = null; size = 0L; modified = null
                    }
                }
                XmlPullParser.TEXT -> text.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "href" -> href = text.toString().trim()
                    "getcontentlength" -> size = text.toString().trim().toLongOrNull() ?: 0L
                    "getlastmodified" -> modified = parseHttpDate(text.toString().trim())
                    "response" -> {
                        val name = href?.trimEnd('/')?.substringAfterLast('/')
                            ?.let { URLDecoder.decode(it, "UTF-8") }
                        if (name != null && accept(name)) {
                            out += RemoteBackup(
                                id = name,
                                name = name,
                                sizeBytes = size,
                                createdAt = BackupNames.timeOf(name) ?: modified ?: 0L
                            )
                        }
                    }
                }
            }
        }
        return out.sortedByDescending { it.createdAt }
    }

    private fun parseHttpDate(value: String): Long? = try {
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).parse(value)?.time
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val USER_AGENT = "Paisa (Android)"
        private val XML = "application/xml; charset=utf-8".toMediaType()
        private val BINARY = BackupNames.MIME.toMediaType()

        private val http = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        /** For the sign-in token POST: a redirect would carry the token to another host. */
        private val noRedirects = http.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        private fun davRoot(account: NextcloudAccount): HttpUrl {
            val base = account.serverUrl.toHttpUrlOrNull()
                ?: throw IOException("Invalid Nextcloud address")
            return base.newBuilder()
                .addPathSegments("remote.php/dav/files")
                .addPathSegment(account.userId)
                .addPathSegment("Paisa")
                .build()
        }

        private fun check(res: Response) {
            when {
                res.isSuccessful -> Unit
                res.code == 401 || res.code == 403 ->
                    throw BackupAuthException("Nextcloud rejected the app password. Sign in again.")
                res.code == 507 -> throw IOException("Your Nextcloud storage is full")
                else -> throw IOException("Nextcloud error ${res.code}")
            }
        }

        /** Adds https:// when missing and drops trailing slashes and `/index.php`. */
        fun normalizeServer(input: String): String? {
            var s = input.trim().trimEnd('/')
            if (s.isEmpty()) return null
            if (!s.startsWith("http://") && !s.startsWith("https://")) s = "https://$s"
            s = s.removeSuffix("/index.php")
            return s.toHttpUrlOrNull()?.toString()?.trimEnd('/')
        }

        /**
         * Why [server] can't be used, or null when it can. The app allows
         * cleartext traffic, so plain http is only accepted for this device,
         * the emulator host or a private network. Pure.
         */
        fun serverError(server: String): String? {
            val url = normalizeServer(server)?.toHttpUrlOrNull()
                ?: return "Enter a full address, like cloud.example.com"
            if (url.username.isNotEmpty() || url.password.isNotEmpty()) {
                return "Don't put a user name or password in the address."
            }
            if (!url.isHttps && !BaseUrlSafety.isLocalHost(url.host)) {
                return "Use https for Nextcloud. Plain http only works for this device or your " +
                    "local network, and would send your password unencrypted."
            }
            return null
        }

        /** Returns [server] or throws an [IOException] with [serverError]'s message. */
        fun requireSafeServer(server: String): String {
            serverError(server)?.let { throw IOException(it) }
            return server
        }

        data class LoginFlow(val loginUrl: String, val pollEndpoint: String, val token: String)

        /** Starts Nextcloud Login Flow v2. Open [LoginFlow.loginUrl] in a browser, then [pollLogin]. */
        suspend fun startLogin(server: String): LoginFlow = withContext(Dispatchers.IO) {
            requireSafeServer(server)
            val url = "$server/index.php/login/v2".toHttpUrlOrNull()
                ?: throw IOException("Invalid Nextcloud address")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .post(FormBody.Builder().build())
                .build()
            http.newCall(request).execute().use { res ->
                if (!res.isSuccessful) {
                    throw IOException("That doesn't look like a Nextcloud server (HTTP ${res.code})")
                }
                parseLoginFlow(res.body?.string().orEmpty())
            }
        }

        /**
         * Reads the Login Flow v2 reply. The server names the page to open and
         * the address to poll, so both must pass the same rule as the server
         * address before the token goes anywhere.
         */
        fun parseLoginFlow(body: String): LoginFlow {
            val json = JSONObject(body)
            val poll = json.getJSONObject("poll")
            val flow = LoginFlow(
                loginUrl = json.getString("login"),
                pollEndpoint = poll.getString("endpoint"),
                token = poll.getString("token")
            )
            requireSafeLoginUrl(flow.loginUrl)
            requireSafeLoginUrl(flow.pollEndpoint)
            return flow
        }

        /** Throws when a server-supplied sign-in address breaks the server address rule. */
        private fun requireSafeLoginUrl(url: String) {
            if (url.toHttpUrlOrNull() == null) throw IOException("Nextcloud returned an invalid sign-in address")
            serverError(url)?.let { throw IOException("Nextcloud returned an unsafe sign-in address. $it") }
        }

        /** Polls until the user approves in the browser. Gives up after 20 minutes. */
        suspend fun pollLogin(flow: LoginFlow): NextcloudAccount = withContext(Dispatchers.IO) {
            requireSafeLoginUrl(flow.pollEndpoint)
            val deadline = System.currentTimeMillis() + 20 * 60 * 1000
            while (System.currentTimeMillis() < deadline) {
                val request = Request.Builder()
                    .url(flow.pollEndpoint)
                    .header("User-Agent", USER_AGENT)
                    .post(FormBody.Builder().add("token", flow.token).build())
                    .build()
                var redirected = 0
                val result = try {
                    noRedirects.newCall(request).execute().use { res ->
                        if (res.code in 300..399) redirected = res.code
                        if (res.code == 200) JSONObject(res.body?.string().orEmpty()) else null
                    }
                } catch (e: IOException) {
                    null
                }
                if (redirected != 0) {
                    throw IOException("Nextcloud redirected the sign-in check (HTTP $redirected). Try again with the server's final address.")
                }
                if (result != null) {
                    val server = normalizeServer(result.getString("server"))
                        ?: throw IOException("Nextcloud returned an invalid address")
                    return@withContext verify(
                        server,
                        result.getString("loginName"),
                        result.getString("appPassword")
                    )
                }
                delay(2_000)
            }
            throw IOException("Nextcloud sign-in timed out")
        }

        /**
         * Checks the credentials and looks up the user id. The WebDAV path uses the
         * id, which can differ from the login name (e.g. email logins).
         */
        suspend fun verify(server: String, loginName: String, appPassword: String): NextcloudAccount =
            withContext(Dispatchers.IO) {
                requireSafeServer(server)
                val url = "$server/ocs/v2.php/cloud/user?format=json".toHttpUrlOrNull()
                    ?: throw IOException("Invalid Nextcloud address")
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", Credentials.basic(loginName, appPassword))
                    .header("OCS-APIRequest", "true")
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build()
                http.newCall(request).execute().use { res ->
                    if (res.code == 401) throw BackupAuthException("Wrong username or app password")
                    if (!res.isSuccessful) throw IOException("Nextcloud error ${res.code}")
                    val id = JSONObject(res.body?.string().orEmpty())
                        .getJSONObject("ocs").getJSONObject("data").getString("id")
                    NextcloudAccount(server, loginName, id, appPassword)
                }
            }
    }
}
