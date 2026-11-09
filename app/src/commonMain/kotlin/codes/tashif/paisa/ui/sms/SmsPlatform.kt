package codes.tashif.paisa.ui.sms

import androidx.compose.runtime.Composable

/** False where the platform can't read SMS at all, such as the browser. */
expect val smsImportSupported: Boolean

/** Current SMS-related permission grants, plus a way to ask for them. */
interface SmsPermissions {
    val hasReadSms: Boolean
    val hasReceiveSms: Boolean
    val hasNotifications: Boolean

    /** Opens the system permission prompt. */
    fun request()
}

/**
 * [onExternalActivityLaunched] runs right before the system dialog opens.
 * [onReadSmsGranted] runs after the dialog closes with READ_SMS granted.
 */
@Composable
expect fun rememberSmsPermissions(
    onExternalActivityLaunched: () -> Unit,
    onReadSmsGranted: () -> Unit
): SmsPermissions
