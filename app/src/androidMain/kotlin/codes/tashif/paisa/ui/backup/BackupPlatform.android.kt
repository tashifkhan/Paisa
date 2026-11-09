package codes.tashif.paisa.ui.backup

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import codes.tashif.paisa.backup.BackupNames
import codes.tashif.paisa.backup.GoogleDriveAuth
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

actual class BackupFile(val uri: Uri)

actual fun urlHost(url: String): String? = Uri.parse(url).host

@Composable
actual fun rememberBackupLaunchers(viewModel: BackupViewModel): BackupLaunchers {
    val backup = viewModel
    val context = LocalContext.current

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        try {
            val result = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(res.data)
            backup.onGoogleAuthorized(result.accessToken)
        } catch (e: ApiException) {
            backup.onGoogleAuthFailed("Google sign-in was cancelled")
        }
    }

    fun connectGoogle() {
        if (!GoogleDriveAuth.isAvailable(context)) {
            backup.onGoogleAuthFailed("Google Drive needs Google Play services. Use Nextcloud instead.")
            return
        }
        Identity.getAuthorizationClient(context)
            .authorize(GoogleDriveAuth.request())
            .addOnSuccessListener { result ->
                val pending = result.pendingIntent
                if (result.hasResolution() && pending != null) {
                    backup.onExternalActivityLaunched()
                    googleLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                } else {
                    backup.onGoogleAuthorized(result.accessToken)
                }
            }
            .addOnFailureListener { e ->
                val code = (e as? ApiException)?.statusCode
                backup.onGoogleAuthFailed(
                    if (code == CommonStatusCodes.DEVELOPER_ERROR) {
                        "Google sign-in isn't set up for this build of Paisa"
                    } else {
                        "Couldn't reach Google: ${e.message ?: "unknown error"}"
                    }
                )
            }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupNames.MIME)
    ) { uri -> if (uri != null) backup.exportToFile(BackupFile(uri)) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) backup.restoreFromFile(BackupFile(uri)) }

    return BackupLaunchers(
        connectGoogle = { connectGoogle() },
        saveToFile = { exportLauncher.launch(BackupNames.forTime(System.currentTimeMillis())) },
        restoreFromFile = { importLauncher.launch(arrayOf("*/*")) },
        openUrl = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    )
}
