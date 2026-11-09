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

    /** Drive store that asks Play services for a fresh token on every call. */
    fun store(context: Context): GoogleDriveStore =
        GoogleDriveStore { silentToken(context.applicationContext) }

    suspend fun email(token: String): String? = GoogleDriveStore.email(token)

    suspend fun revoke(token: String) = GoogleDriveStore.revoke(token)
}
