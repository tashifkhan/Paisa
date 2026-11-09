package codes.tashif.paisa.ui.settings

import androidx.compose.runtime.Composable

private object WebBiometrics : Biometrics {
    override val available = false
    override val statusMessage = "App lock needs the Android app"

    override fun authenticate(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) = onError(statusMessage)
}

@Composable
actual fun rememberBiometrics(): Biometrics = WebBiometrics
