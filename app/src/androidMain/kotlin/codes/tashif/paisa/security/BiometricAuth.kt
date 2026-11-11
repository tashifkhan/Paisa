package codes.tashif.paisa.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricStatus {
    Available,
    NoneEnrolled,
    HardwareUnavailable,
    Unsupported
}

object BiometricAuth {

    private const val ALLOWED_AUTHENTICATORS =
        Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL

    fun status(context: Context): BiometricStatus {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HardwareUnavailable
            else -> BiometricStatus.Unsupported
        }
    }

    fun isAvailable(context: Context): Boolean = status(context) == BiometricStatus.Available

    fun statusMessage(status: BiometricStatus): String = when (status) {
        BiometricStatus.Available -> "Protect Paisa with biometrics or device lock"
        BiometricStatus.NoneEnrolled -> "Set up fingerprint, face, or screen lock in system settings first"
        BiometricStatus.HardwareUnavailable -> "Biometric hardware is unavailable right now"
        BiometricStatus.Unsupported -> "This device does not support app lock"
    }

    /**
     * Shows the system biometric / device-credential prompt.
     * Uses DEVICE_CREDENTIAL as a fallback so PIN/pattern/password still works.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Paisa",
        subtitle: String = "Authenticate to access your finances",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {},
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // User cancel / negative button — not a hard failure to surface loudly
                    if (
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        return
                    }
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    onFailed()
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()

        try {
            prompt.authenticate(info)
        } catch (e: Exception) {
            onError(e.message ?: "Could not start authentication")
        }
    }
}
