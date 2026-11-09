package codes.tashif.paisa.ui.sms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

actual val smsImportSupported: Boolean = true

@Composable
actual fun rememberSmsPermissions(
    onExternalActivityLaunched: () -> Unit,
    onReadSmsGranted: () -> Unit
): SmsPermissions {
    val context = LocalContext.current

    var hasReadSms by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasReceiveSms by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotifications by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true
            else ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasReadSms = result[Manifest.permission.READ_SMS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
        hasReceiveSms = result[Manifest.permission.RECEIVE_SMS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotifications = result[Manifest.permission.POST_NOTIFICATIONS] == true ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        }
        if (hasReadSms) {
            onReadSmsGranted()
        }
    }

    return object : SmsPermissions {
        override val hasReadSms: Boolean get() = hasReadSms
        override val hasReceiveSms: Boolean get() = hasReceiveSms
        override val hasNotifications: Boolean get() = hasNotifications

        override fun request() {
            val perms = buildList {
                add(Manifest.permission.READ_SMS)
                add(Manifest.permission.RECEIVE_SMS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            onExternalActivityLaunched()
            permissionLauncher.launch(perms.toTypedArray())
        }
    }
}
