package sh.taf.billi.ui.lock

import androidx.compose.runtime.Composable

/** The device's unlock prompt: fingerprint, face or screen lock. */
interface Biometrics {
    /** True on platforms that provide device authentication. */
    val supported: Boolean get() = true

    /** True when the device can show the prompt. */
    val available: Boolean

    /** Why app lock can or can't be used, for the settings row. */
    val statusMessage: String

    fun authenticate(
        title: String = "Unlock Billi",
        subtitle: String = "Authenticate to access your finances",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {},
        onFailed: () -> Unit = {}
    )
}

internal fun Biometrics.authenticateToShowBalances(
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    when {
        !supported -> onSuccess()
        !available -> onError(statusMessage)
        else -> authenticate(
            title = "Show balances",
            subtitle = "Confirm it's you to reveal your amounts",
            onSuccess = onSuccess,
            onError = onError
        )
    }
}

@Composable
expect fun rememberBiometrics(): Biometrics
