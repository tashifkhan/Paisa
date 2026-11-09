package codes.tashif.paisa.backup

import codes.tashif.paisa.ai.provider.BaseUrlSafety
import codes.tashif.paisa.platform.nowMillis
import codes.tashif.paisa.sync.SyncFileNames
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.TextContent
import io.ktor.http.decodeURLPart
import io.ktor.http.formUrlEncode
import io.ktor.http.parameters
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64

/**
 * How requests reach Nextcloud. Android talks to the server directly. A
 * browser can't, because Nextcloud sends no CORS headers, so the web sends
 * each request to a small proxy that forwards it (see tools/nextcloud-proxy).
 */
fun interface NextcloudTransport {
    /** The URL to call for [target], plus any extra headers. */
    fun route(target: String): Pair<String, Map<String, String>>

    companion object {
        val Direct = NextcloudTransport { it to emptyMap() }

        /** Sends every request to [proxy], naming the real address in a header. */
        fun proxy(proxy: String) = NextcloudTransport { target ->
            proxy.trimEnd('/') + "/" to mapOf(PROXY_TARGET_HEADER to target)
        }

        const val PROXY_TARGET_HEADER = "X-Paisa-Target"
    }
}

/**
 * Nextcloud over WebDAV. Backups live in `/Paisa` in the user's files, so
 * they are visible in the Nextcloud web UI and apps. Sync files live in
 * `/Paisa/sync`.
 */
class NextcloudStore(
    private val account: NextcloudAccount,
    private val transport: NextcloudTransport = NextcloudTransport.Direct
) : BackupStore {

    private val folder: String = davRoot(account)
    private val syncFolder: String = child(folder, "sync")

    override suspend fun list(): List<RemoteBackup> {
        val body = """<?xml version="1.0"?>
            <d:propfind xmlns:d="DAV:"><d:prop><d:getcontentlength/><d:getlastmodified/></d:prop></d:propfind>"""
        val res = send(HttpMethod("PROPFIND"), folder, depth = true) { setBody(TextContent(body, XML)) }
        if (res.status.value == 404) return emptyList()
        check(res)
        return parsePropfind(res.bodyAsText(), BackupNames::isBackup)
    }

    override suspend fun upload(name: String, bytes: ByteArray): RemoteBackup {
        ensureFolder(folder)
        check(send(HttpMethod.Put, child(folder, name)) { setBody(ByteArrayContent(bytes, BINARY)) })
        return RemoteBackup(
            id = name,
            name = name,
            sizeBytes = bytes.size.toLong(),
            createdAt = BackupNames.timeOf(name) ?: nowMillis()
        )
    }

    override suspend fun download(backup: RemoteBackup): ByteArray = get(child(folder, backup.name))

    override suspend fun delete(backup: RemoteBackup) = delete(child(folder, backup.name))

    override suspend fun listSync(): List<RemoteBackup> {
        val body = """<?xml version="1.0"?>
            <d:propfind xmlns:d="DAV:"><d:prop><d:getcontentlength/></d:prop></d:propfind>"""
        val res = send(HttpMethod("PROPFIND"), syncFolder, depth = true) { setBody(TextContent(body, XML)) }
        if (res.status.value == 404) return emptyList()
        check(res)
        return parsePropfind(res.bodyAsText(), SyncFileNames::isSyncFile)
    }

    override suspend fun uploadSync(name: String, bytes: ByteArray): RemoteBackup {
        ensureFolder(folder)
        ensureFolder(syncFolder)
        check(send(HttpMethod.Put, child(syncFolder, name)) { setBody(ByteArrayContent(bytes, BINARY)) })
        return RemoteBackup(id = name, name = name, sizeBytes = bytes.size.toLong(), createdAt = 0L)
    }

    override suspend fun downloadSync(file: RemoteBackup): ByteArray = get(child(syncFolder, file.name))

    override suspend fun deleteSync(file: RemoteBackup) = delete(child(syncFolder, file.name))

    private suspend fun get(url: String): ByteArray {
        val res = send(HttpMethod.Get, url)
        check(res)
        return res.bodyAsBytes()
    }

    private suspend fun delete(url: String) {
        val res = send(HttpMethod.Delete, url)
        if (res.status.value != 404) check(res)
    }

    private suspend fun ensureFolder(url: String) {
        val res = send(HttpMethod("MKCOL"), url)
        // 405 = already exists
        if (res.status.value != 405) check(res)
    }

    /** Every request goes through here, so an account saved with plain http stops before sending the password. */
    private suspend fun send(
        method: HttpMethod,
        url: String,
        depth: Boolean = false,
        body: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        requireSafeServer(account.serverUrl)
        return call(BackupHttp.client, transport, method, url) {
            header("Authorization", basicAuth(account.loginName, account.appPassword))
            if (depth) header("Depth", "1")
            body()
        }
    }

    companion object {
        private const val USER_AGENT = "Paisa"
        private val XML = ContentType.parse("application/xml; charset=utf-8")
        private val BINARY = ContentType.parse(BackupNames.MIME)
        private val json = Json { ignoreUnknownKeys = true }

        private suspend fun call(
            client: HttpClient,
            transport: NextcloudTransport,
            method: HttpMethod,
            target: String,
            block: HttpRequestBuilder.() -> Unit
        ): HttpResponse {
            val (url, extra) = transport.route(target)
            return try {
                client.request(url) {
                    this.method = method
                    header("User-Agent", USER_AGENT)
                    extra.forEach { (k, v) -> header(k, v) }
                    block()
                }
            } catch (e: IOException) {
                throw e
            } catch (e: Exception) {
                // Browsers report CORS and network failures as a bare TypeError.
                throw IOException(e.message?.takeIf { it.isNotBlank() } ?: "Couldn't reach Nextcloud")
            }
        }

        /** OkHttp's Credentials.basic: ISO-8859-1, so existing passwords encode the same. */
        internal fun basicAuth(user: String, password: String): String {
            val raw = "$user:$password"
            val bytes = ByteArray(raw.length) { i -> raw[i].code.let { if (it < 256) it.toByte() else '?'.code.toByte() } }
            return "Basic " + Base64.encode(bytes)
        }

        private fun davRoot(account: NextcloudAccount): String {
            val base = parseUrl(account.serverUrl) ?: throw IOException("Invalid Nextcloud address")
            return URLBuilder(base).apply {
                appendPathSegments("remote.php", "dav", "files", account.userId, "Paisa")
            }.buildString()
        }

        private fun child(folder: String, name: String): String =
            URLBuilder(folder).apply { appendPathSegments(name) }.buildString()

        /** An absolute http(s) URL, or null. Stricter than Ktor, which reads "not a url" as a relative path. */
        private fun parseUrl(value: String): Url? {
            if (!(value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true))) return null
            if (value.any { it.isWhitespace() }) return null
            return try {
                Url(value).takeIf { it.host.isNotEmpty() }
            } catch (e: Exception) {
                null
            }
        }

        private suspend fun check(res: HttpResponse) {
            val code = res.status.value
            when {
                code in 200..299 -> Unit
                code == 401 || code == 403 ->
                    throw BackupAuthException("Nextcloud rejected the app password. Sign in again.")
                code == 507 -> throw IOException("Your Nextcloud storage is full")
                else -> throw IOException("Nextcloud error $code")
            }
        }

        /**
         * Reads a WebDAV multistatus reply. The namespace prefix differs between
         * servers (`d:`, `D:` or none), so match on local names.
         */
        internal fun parsePropfind(xml: String, accept: (String) -> Boolean): List<RemoteBackup> {
            fun tag(name: String) = Regex("<(?:[A-Za-z0-9_-]+:)?$name(?:\\s[^>]*)?>(.*?)</(?:[A-Za-z0-9_-]+:)?$name>", RegexOption.DOT_MATCHES_ALL)
            val responses = tag("response").findAll(xml).map { it.groupValues[1] }
            val out = mutableListOf<RemoteBackup>()
            for (r in responses) {
                val href = tag("href").find(r)?.groupValues?.get(1)?.trim()?.let(::unescapeXml) ?: continue
                val size = tag("getcontentlength").find(r)?.groupValues?.get(1)?.trim()?.toLongOrNull() ?: 0L
                val modified = tag("getlastmodified").find(r)?.groupValues?.get(1)?.trim()?.let(::parseHttpDate)
                val name = href.trimEnd('/').substringAfterLast('/').decodeURLPart()
                if (accept(name)) {
                    out += RemoteBackup(
                        id = name,
                        name = name,
                        sizeBytes = size,
                        createdAt = BackupNames.timeOf(name) ?: modified ?: 0L
                    )
                }
            }
            return out.sortedByDescending { it.createdAt }
        }

        private fun unescapeXml(s: String) = s.replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")

        private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        /** RFC 1123, as WebDAV's getlastmodified uses: "Mon, 09 Nov 2026 15:29:37 GMT". */
        internal fun parseHttpDate(value: String): Long? {
            val m = Regex("""\w{3}, (\d{1,2}) (\w{3}) (\d{4}) (\d{2}):(\d{2}):(\d{2}) GMT""").find(value) ?: return null
            val (d, mon, y, h, mi, s) = m.destructured
            val month = MONTHS.indexOf(mon).takeIf { it >= 0 } ?: return null
            return try {
                LocalDateTime(y.toInt(), month + 1, d.toInt(), h.toInt(), mi.toInt(), s.toInt())
                    .toInstant(TimeZone.UTC).toEpochMilliseconds()
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        /** Adds https:// when missing and drops trailing slashes and `/index.php`. */
        fun normalizeServer(input: String): String? {
            var s = input.trim().trimEnd('/')
            if (s.isEmpty()) return null
            if (!s.startsWith("http://") && !s.startsWith("https://")) s = "https://$s"
            s = s.removeSuffix("/index.php")
            val url = parseUrl(s) ?: return null
            // OkHttp's HttpUrl lower-cased the host and dropped default ports; match that.
            return URLBuilder(url).apply { host = host.lowercase() }.buildString().trimEnd('/')
        }

        /**
         * Why [server] can't be used, or null when it can. Plain http is only
         * accepted for this device, the emulator host or a private network.
         */
        fun serverError(server: String): String? {
            val url = normalizeServer(server)?.let(::parseUrl)
                ?: return "Enter a full address, like cloud.example.com"
            if (!url.user.isNullOrEmpty() || !url.password.isNullOrEmpty()) {
                return "Don't put a user name or password in the address."
            }
            if (url.protocol.name != "https" && !BaseUrlSafety.isLocalHost(url.host)) {
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
        suspend fun startLogin(server: String, transport: NextcloudTransport = NextcloudTransport.Direct): LoginFlow {
            requireSafeServer(server)
            val res = call(BackupHttp.client, transport, HttpMethod.Post, "$server/index.php/login/v2") {
                setBody(TextContent("", ContentType.Application.FormUrlEncoded))
            }
            if (res.status.value !in 200..299) {
                throw IOException("That doesn't look like a Nextcloud server (HTTP ${res.status.value})")
            }
            return parseLoginFlow(res.bodyAsText())
        }

        /**
         * Reads the Login Flow v2 reply. The server names the page to open and
         * the address to poll, so both must pass the same rule as the server
         * address before the token goes anywhere.
         */
        fun parseLoginFlow(body: String): LoginFlow {
            val root = try {
                json.parseToJsonElement(body).jsonObject
            } catch (e: Exception) {
                throw IOException("Nextcloud sent an unexpected sign-in reply")
            }
            val poll = root["poll"]?.jsonObject ?: throw IOException("Nextcloud sent an unexpected sign-in reply")
            val flow = LoginFlow(
                loginUrl = root["login"]?.jsonPrimitive?.content ?: throw IOException("No sign-in address"),
                pollEndpoint = poll["endpoint"]?.jsonPrimitive?.content ?: throw IOException("No sign-in address"),
                token = poll["token"]?.jsonPrimitive?.content ?: throw IOException("No sign-in token")
            )
            requireSafeLoginUrl(flow.loginUrl)
            requireSafeLoginUrl(flow.pollEndpoint)
            return flow
        }

        /** Throws when a server-supplied sign-in address breaks the server address rule. */
        private fun requireSafeLoginUrl(url: String) {
            if (parseUrl(url) == null) throw IOException("Nextcloud returned an invalid sign-in address")
            serverError(url)?.let { throw IOException("Nextcloud returned an unsafe sign-in address. $it") }
        }

        /** Polls until the user approves in the browser. Gives up after 20 minutes. */
        suspend fun pollLogin(flow: LoginFlow, transport: NextcloudTransport = NextcloudTransport.Direct): NextcloudAccount {
            requireSafeLoginUrl(flow.pollEndpoint)
            val deadline = nowMillis() + 20 * 60 * 1000
            while (nowMillis() < deadline) {
                val form = parameters { append("token", flow.token) }.formUrlEncode()
                val res = try {
                    call(BackupHttp.noRedirects, transport, HttpMethod.Post, flow.pollEndpoint) {
                        setBody(TextContent(form, ContentType.Application.FormUrlEncoded))
                    }
                } catch (e: IOException) {
                    null
                }
                val code = res?.status?.value ?: 0
                if (code in 300..399) {
                    throw IOException("Nextcloud redirected the sign-in check (HTTP $code). Try again with the server's final address.")
                }
                if (code == 200) {
                    val result = json.parseToJsonElement(res!!.bodyAsText()).jsonObject
                    val server = normalizeServer(result["server"]?.jsonPrimitive?.content.orEmpty())
                        ?: throw IOException("Nextcloud returned an invalid address")
                    return verify(
                        server,
                        result["loginName"]?.jsonPrimitive?.content.orEmpty(),
                        result["appPassword"]?.jsonPrimitive?.content.orEmpty(),
                        transport
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
        suspend fun verify(
            server: String,
            loginName: String,
            appPassword: String,
            transport: NextcloudTransport = NextcloudTransport.Direct
        ): NextcloudAccount {
            requireSafeServer(server)
            val res = call(BackupHttp.client, transport, HttpMethod.Get, "$server/ocs/v2.php/cloud/user?format=json") {
                header("Authorization", basicAuth(loginName, appPassword))
                header("OCS-APIRequest", "true")
            }
            if (res.status.value == 401) throw BackupAuthException("Wrong username or app password")
            if (res.status.value !in 200..299) throw IOException("Nextcloud error ${res.status.value}")
            val id = try {
                json.parseToJsonElement(res.bodyAsText()).jsonObject["ocs"]!!.jsonObject["data"]!!
                    .jsonObject["id"]!!.jsonPrimitive.content
            } catch (e: Exception) {
                throw IOException("Nextcloud sent an unexpected user reply")
            }
            return NextcloudAccount(server, loginName, id, appPassword)
        }
    }
}
