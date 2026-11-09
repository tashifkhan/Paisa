package codes.tashif.paisa.backup

import codes.tashif.paisa.platform.nowMillis
import codes.tashif.paisa.platform.parseDate
import codes.tashif.paisa.platform.randomHexId
import codes.tashif.paisa.sync.SyncFileNames
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
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.TextContent
import io.ktor.http.formUrlEncode
import io.ktor.http.parameters
import kotlinx.coroutines.delay
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** A Google access token for `drive.appdata`. Throws [BackupAuthException] when the user must connect again. */
fun interface DriveTokens {
    suspend fun token(): String
}

/**
 * Drive v3 REST against the hidden `appDataFolder`. Backups count against the
 * user's Google storage but don't show up in the Drive file list, the same as
 * WhatsApp's chat backups. Sync files sit next to backups, told apart by name.
 *
 * Google's APIs send CORS headers, so the browser calls them directly.
 */
class GoogleDriveStore(private val tokens: DriveTokens) : BackupStore {

    override suspend fun list(): List<RemoteBackup> {
        val url = "$API/files?spaces=appDataFolder" +
            "&fields=files(id,name,size,createdTime)&orderBy=createdTime%20desc&pageSize=100"
        val body = call(HttpMethod.Get, url).bodyAsText()
        return files(body).mapNotNull { f ->
            val name = f.str("name") ?: return@mapNotNull null
            if (!BackupNames.isBackup(name)) return@mapNotNull null
            RemoteBackup(
                id = f.str("id") ?: return@mapNotNull null,
                name = name,
                sizeBytes = f.str("size")?.toLongOrNull() ?: 0L,
                createdAt = BackupNames.timeOf(name) ?: f.str("createdTime")?.let(::parseRfc3339) ?: 0L
            )
        }.sortedByDescending { it.createdAt }
    }

    override suspend fun upload(name: String, bytes: ByteArray): RemoteBackup {
        val metadata = buildJsonObject {
            put("name", name)
            putJsonArray("parents") { add("appDataFolder") }
        }.toString()
        val boundary = "paisa" + randomHexId()
        // multipart/related: JSON metadata, then the file bytes.
        val body = (
            "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n" +
                "--$boundary\r\nContent-Type: ${BackupNames.MIME}\r\n\r\n"
            ).encodeToByteArray() + bytes + "\r\n--$boundary--\r\n".encodeToByteArray()
        val res = call(HttpMethod.Post, "$UPLOAD/files?uploadType=multipart&fields=id,name,size,createdTime") {
            setBody(ByteArrayContent(body, ContentType.parse("multipart/related; boundary=$boundary")))
        }
        val f = json.parseToJsonElement(res.bodyAsText()).jsonObject
        return RemoteBackup(
            id = f.str("id") ?: throw IOException("Google Drive returned no file id"),
            name = f.str("name") ?: name,
            sizeBytes = f.str("size")?.toLongOrNull() ?: bytes.size.toLong(),
            createdAt = BackupNames.timeOf(name) ?: nowMillis()
        )
    }

    override suspend fun download(backup: RemoteBackup): ByteArray =
        call(HttpMethod.Get, "$API/files/${backup.id}?alt=media").bodyAsBytes()

    override suspend fun delete(backup: RemoteBackup) {
        try {
            call(HttpMethod.Delete, "$API/files/${backup.id}")
        } catch (e: RemoteMissing) {
            // Already gone.
        }
    }

    override suspend fun listSync(): List<RemoteBackup> {
        val out = mutableListOf<RemoteBackup>()
        var pageToken: String? = null
        do {
            val url = URLBuilder("$API/files").apply {
                parameters.append("spaces", "appDataFolder")
                parameters.append("q", "name contains '${SyncFileNames.PREFIX}' and trashed = false")
                parameters.append("fields", "nextPageToken,files(id,name,size)")
                parameters.append("pageSize", "1000")
                pageToken?.let { parameters.append("pageToken", it) }
            }.buildString()
            val root = json.parseToJsonElement(call(HttpMethod.Get, url).bodyAsText()).jsonObject
            for (f in (root["files"] as? JsonArray).orEmpty().map { it.jsonObject }) {
                val name = f.str("name") ?: continue
                if (!SyncFileNames.isSyncFile(name)) continue
                out += RemoteBackup(
                    id = f.str("id") ?: continue,
                    name = name,
                    sizeBytes = f.str("size")?.toLongOrNull() ?: 0L,
                    createdAt = 0L
                )
            }
            pageToken = root.str("nextPageToken")?.ifBlank { null }
        } while (pageToken != null)
        return out
    }

    override suspend fun uploadSync(name: String, bytes: ByteArray): RemoteBackup = upload(name, bytes)

    override suspend fun downloadSync(file: RemoteBackup): ByteArray = download(file)

    override suspend fun deleteSync(file: RemoteBackup) = delete(file)

    private class RemoteMissing : IOException("Backup not found on Google Drive")

    /** Runs a request, retrying 429/5xx with backoff. Returns a successful response. */
    private suspend fun call(
        method: HttpMethod,
        url: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        val token = tokens.token()
        var attempt = 0
        while (true) {
            val res = try {
                BackupHttp.client.request(url) {
                    this.method = method
                    header("Authorization", "Bearer $token")
                    block()
                }
            } catch (e: IOException) {
                throw e
            } catch (e: Exception) {
                throw IOException(e.message?.takeIf { it.isNotBlank() } ?: "Couldn't reach Google Drive")
            }
            val code = res.status.value
            when {
                code in 200..299 -> return res
                code == 401 -> throw BackupAuthException("Google Drive access expired. Connect it again.")
                code == 404 -> throw RemoteMissing()
                code == 403 && res.bodyAsText().take(4096).contains("storageQuotaExceeded") ->
                    throw IOException("Your Google storage is full")
                (code == 429 || code >= 500) && attempt < 3 -> {
                    attempt++
                    delay(1000L shl attempt)
                }
                else -> throw IOException("Google Drive error $code")
            }
        }
    }

    private fun files(body: String): List<JsonObject> =
        (json.parseToJsonElement(body).jsonObject["files"] as? JsonArray).orEmpty().map { it.jsonObject }

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.content

    private fun parseRfc3339(value: String): Long? =
        parseDate(value.substringBefore('.').removeSuffix("Z"), "yyyy-MM-dd'T'HH:mm:ss", utc = true, machine = true)

    companion object {
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private val json = Json { ignoreUnknownKeys = true }

        /** The signed-in account's email, for the settings row. Null if Google won't say. */
        suspend fun email(token: String): String? = try {
            val res = BackupHttp.client.request("https://www.googleapis.com/oauth2/v3/userinfo") {
                header("Authorization", "Bearer $token")
            }
            if (res.status.value !in 200..299) null
            else (json.parseToJsonElement(res.bodyAsText()).jsonObject["email"] as? JsonPrimitive)
                ?.content?.ifBlank { null }
        } catch (e: Exception) {
            null
        }

        /** Revokes the grant so the next connect shows the account picker again. Best effort. */
        suspend fun revoke(token: String) {
            try {
                BackupHttp.client.request("https://oauth2.googleapis.com/revoke") {
                    method = HttpMethod.Post
                    setBody(
                        TextContent(
                            parameters { append("token", token) }.formUrlEncode(),
                            ContentType.Application.FormUrlEncoded
                        )
                    )
                }
            } catch (e: Exception) {
                // Local state is cleared either way.
            }
        }
    }
}
