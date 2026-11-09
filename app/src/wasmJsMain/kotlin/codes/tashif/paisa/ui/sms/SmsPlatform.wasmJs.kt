package codes.tashif.paisa.ui.sms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual val smsImportSupported: Boolean = false

@Composable
actual fun rememberSmsPermissions(
    onExternalActivityLaunched: () -> Unit,
    onReadSmsGranted: () -> Unit
): SmsPermissions = remember {
    object : SmsPermissions {
        override val hasReadSms = false
        override val hasReceiveSms = false
        override val hasNotifications = false
        override fun request() = Unit
    }
}
