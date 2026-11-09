package codes.tashif.paisa.ui.app

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import codes.tashif.paisa.security.BiometricAuth
import codes.tashif.paisa.ui.backup.BackupScreen
import codes.tashif.paisa.ui.onboarding.OnboardingScreen
import codes.tashif.paisa.ui.settings.AppLockScreen
import codes.tashif.paisa.ui.theme.PaisaPalette
import codes.tashif.paisa.ui.theme.PaisaTheme

/**
 * The whole app: theme from settings, then onboarding, the lock screen or the
 * main shell. Waits for the settings row so main content doesn't flash before
 * onboarding.
 */
@Composable
fun PaisaApp(viewModel: AppViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val isDark = when (settings?.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    PaisaTheme(
        darkTheme = isDark,
        palette = PaisaPalette.fromId(settings?.colorPalette),
        expressive = settings?.expressiveUi ?: true,
        amoledDark = settings?.amoledDark ?: false,
        highContrast = settings?.highContrast ?: false
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val current = settings
            if (current == null) {
                Box(Modifier.fillMaxSize())
                return@Surface
            }

            val needsOnboarding = !current.onboardingCompleted
            val appLockOn = current.biometricEnabled
            var restoringFromOnboarding by remember { mutableStateOf(false) }
            when {
                needsOnboarding && restoringFromOnboarding -> {
                    // A successful restore sets onboardingCompleted, which leaves this branch.
                    CenteredContent {
                        BackupScreen(onBack = { restoringFromOnboarding = false })
                    }
                }
                needsOnboarding -> {
                    // OnboardingScreen marks onboarding done itself.
                    CenteredContent {
                        OnboardingScreen(onRestoreBackup = { restoringFromOnboarding = true })
                    }
                }
                appLockOn && isLocked -> CenteredContent {
                    LockGate(onUnlocked = viewModel::unlockApp)
                }
                else -> AppShell(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun LockGate(onUnlocked: () -> Unit) {
    val activity = LocalContext.current.findFragmentActivity()
    var lockError by remember { mutableStateOf<String?>(null) }
    val biometricAvailable = remember { BiometricAuth.isAvailable(activity) }
    AppLockScreen(
        errorMessage = lockError,
        biometricAvailable = biometricAvailable,
        onUnlockClick = {
            BiometricAuth.authenticate(
                activity = activity,
                onSuccess = {
                    lockError = null
                    onUnlocked()
                },
                onError = { lockError = it },
                onFailed = { lockError = "Authentication failed — try again" }
            )
        }
    )
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> error("PaisaApp must be hosted in a FragmentActivity")
}
