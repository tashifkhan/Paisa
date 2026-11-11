package codes.tashif.paisa.ui.settings

import androidx.compose.runtime.Composable

/** The device's unlock prompt: fingerprint, face or screen lock. */
interface Biometrics {
    /** True when the device can show the prompt. */
    val available: Boolean

    /** Why app lock can or can't be used, for the settings row. */
    val statusMessage: String

    fun authenticate(
        title: String = "Unlock Paisa",
        subtitle: String = "Authenticate to access your finances",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {},
        onFailed: () -> Unit = {}
    )
}

@Composable
expect fun rememberBiometrics(): Biometrics
