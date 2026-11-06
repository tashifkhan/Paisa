package codes.tashif.paisa.backup

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Google sign-in for backups, the same Play Services account sheet WhatsApp uses.
 *
 * Only `drive.appdata` is requested: Paisa can see its own hidden folder and
 * nothing else in the user's Drive.
 */
object GoogleDriveAuth {
    private const val SCOPE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
    private const val SCOPE_EMAIL = "email"

    fun isAvailable(context: Context): Boolean =
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    fun request(): AuthorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(SCOPE_APPDATA), Scope(SCOPE_EMAIL)))
        .build()

    /** Token without UI. Throws [BackupAuthException] when the user must approve again. */
    suspend fun silentToken(context: Context): String = withContext(Dispatchers.IO) {
        val result: AuthorizationResult = try {
            Tasks.await(
                Identity.getAuthorizationClient(context.applicationContext).authorize(request()),
                30,
                TimeUnit.SECONDS
            )
        } catch (e: Exception) {
            throw BackupAuthException("Google sign-in failed. Connect Google Drive again.")
        }
        if (result.hasResolution()) {
            throw BackupAuthException("Google Drive access expired. Connect it again.")
        }
        result.accessToken ?: throw BackupAuthException("Google returned no access token")
    }

    suspend fun email(token: String): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://www.googleapis.com/oauth2/v3/userinfo")
            .header("Authorization", "Bearer $token")
            .build()
        try {
            GoogleDriveStore.http.newCall(request).execute().use { res ->
                if (!res.isSuccessful) null
                else JSONObject(res.body?.string().orEmpty()).optString("email").ifBlank { null }
            }
        } catch (e: IOException) {
            null
        }
    }

    /** Revokes the grant so the next connect shows the account picker again. */
    suspend fun revoke(token: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://oauth2.googleapis.com/revoke")
            .post(FormBody.Builder().add("token", token).build())
            .build()
        try {
            GoogleDriveStore.http.newCall(request).execute().close()
        } catch (e: IOException) {
            // Best effort. Local state is cleared either way.
        }
    }
}

/**
 * Drive v3 REST against the hidden `appDataFolder`. Backups count against the
 * user's Google storage but don't show up in the Drive file list, the same as
 * WhatsApp's chat backups.
 */
class GoogleDriveStore(private val context: Context) : BackupStore {

    override suspend fun list(): List<RemoteBackup> = withContext(Dispatchers.IO) {
        val url = "$API/files?spaces=appDataFolder" +
            "&fields=files(id,name,size,createdTime)&orderBy=createdTime%20desc&pageSize=100"
        call { token ->
            Request.Builder().url(url).header("Authorization", "Bearer $token").get().build()
        }.use { res ->
            val files = JSONObject(res.body?.string().orEmpty()).optJSONArray("files")
            val out = mutableListOf<RemoteBackup>()
            if (files != null) {
                for (i in 0 until files.length()) {
                    val f = files.getJSONObject(i)
                    val name = f.optString("name")
                    if (!BackupNames.isBackup(name)) continue
                    out += RemoteBackup(
                        id = f.getString("id"),
                        name = name,
                        sizeBytes = f.optString("size").toLongOrNull() ?: 0L,
                        createdAt = BackupNames.timeOf(name)
                            ?: parseRfc3339(f.optString("createdTime")) ?: 0L
                    )
                }
            }
            out.sortedByDescending { it.createdAt }
        }
    }

    override suspend fun upload(name: String, bytes: ByteArray): RemoteBackup =
        withContext(Dispatchers.IO) {
            val metadata = JSONObject()
                .put("name", name)
                .put("parents", org.json.JSONArray().put("appDataFolder"))
                .toString()
            call { token ->
                val body = MultipartBody.Builder()
                    .setType("multipart/related".toMediaType())
                    .addPart(metadata.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                    .addPart(bytes.toRequestBody(BackupNames.MIME.toMediaType()))
                    .build()
                Request.Builder()
                    .url("$UPLOAD/files?uploadType=multipart&fields=id,name,size,createdTime")
                    .header("Authorization", "Bearer $token")
                    .post(body)
                    .build()
            }.use { res ->
                val f = JSONObject(res.body?.string().orEmpty())
                RemoteBackup(
                    id = f.getString("id"),
                    name = f.optString("name", name),
                    sizeBytes = f.optString("size").toLongOrNull() ?: bytes.size.toLong(),
                    createdAt = BackupNames.timeOf(name) ?: System.currentTimeMillis()
                )
            }
        }

    override suspend fun download(backup: RemoteBackup): ByteArray = withContext(Dispatchers.IO) {
        call { token ->
            Request.Builder()
                .url("$API/files/${backup.id}?alt=media")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
        }.use { res -> res.body?.bytes() ?: throw IOException("Empty response from Google Drive") }
    }

    override suspend fun delete(backup: RemoteBackup) = withContext(Dispatchers.IO) {
        try {
            call { token ->
                Request.Builder()
                    .url("$API/files/${backup.id}")
                    .header("Authorization", "Bearer $token")
                    .delete()
                    .build()
            }.close()
        } catch (e: RemoteMissing) {
            // Already gone.
        }
    }

    private class RemoteMissing : IOException("Backup not found on Google Drive")

    /** Runs a request, retrying 429/5xx with backoff. Returns a successful response. */
    private suspend fun call(build: (token: String) -> Request): Response {
        val token = GoogleDriveAuth.silentToken(context)
        var attempt = 0
        while (true) {
            val res = http.newCall(build(token)).execute()
            when {
                res.isSuccessful -> return res
                res.code == 401 -> {
                    res.close()
                    throw BackupAuthException("Google Drive access expired. Connect it again.")
                }
                res.code == 404 -> { res.close(); throw RemoteMissing() }
                res.code == 403 && res.peekBody(4096).string().contains("storageQuotaExceeded") -> {
                    res.close()
                    throw IOException("Your Google storage is full")
                }
                (res.code == 429 || res.code >= 500) && attempt < 3 -> {
                    res.close()
                    attempt++
                    kotlinx.coroutines.delay(1000L shl attempt)
                }
                else -> {
                    val code = res.code
                    res.close()
                    throw IOException("Google Drive error $code")
                }
            }
        }
    }

    private fun parseRfc3339(value: String): Long? = try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(value)?.time
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"

        internal val http = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }
}
