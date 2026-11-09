package codes.tashif.paisa.ui.settings

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import codes.tashif.paisa.security.BiometricAuth
import codes.tashif.paisa.security.BiometricStatus

@Composable
actual fun rememberBiometrics(): Biometrics {
    val context = LocalContext.current
    return remember(context) {
        val status = BiometricAuth.status(context)
        object : Biometrics {
            override val available: Boolean = status == BiometricStatus.Available
            override val statusMessage: String = BiometricAuth.statusMessage(status)

            override fun authenticate(
                title: String,
                subtitle: String,
                onSuccess: () -> Unit,
                onError: (String) -> Unit,
                onFailed: () -> Unit
            ) {
                val activity = context.findFragmentActivity() ?: return
                BiometricAuth.authenticate(activity, title, subtitle, onSuccess, onError, onFailed)
            }
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
