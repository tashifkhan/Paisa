package sh.taf.billi.ui.app

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import sh.taf.billi.ui.ai.AiScreen
import sh.taf.billi.ui.backup.BackupScreen
import sh.taf.billi.ui.onboarding.OnboardingViewModel
import sh.taf.billi.ui.settings.AboutScreen
import sh.taf.billi.ui.settings.AppearanceScreen
import sh.taf.billi.ui.categories.CategoriesScreen
import sh.taf.billi.ui.settings.CurrencyScreen
import sh.taf.billi.ui.categories.MerchantMappingsScreen
import sh.taf.billi.ui.notifications.NotificationImportScreen
import sh.taf.billi.ui.sms.SmsSetupScreen
import sh.taf.billi.ui.statement.StatementImportScreen

/** Shows [screen] in place of the tabs. Back closes it. */
@Composable
fun FullScreenHost(
    screen: FullScreen,
    state: ShellState,
    onExternalActivityLaunched: () -> Unit
) {
    val close = { state.close(screen) }
    when (screen) {
        FullScreen.SMS_SETUP -> SmsSetupScreen(
            onBack = close,
            onExternalActivityLaunched = onExternalActivityLaunched
        )
        FullScreen.NOTIFICATION_IMPORT -> NotificationImportScreen(
            onBack = close,
            onExternalActivityLaunched = onExternalActivityLaunched
        )
        FullScreen.APPEARANCE -> AppearanceScreen(onBack = close)
        FullScreen.AI -> AiScreen(onBack = close)
        FullScreen.STATEMENT_IMPORT -> StatementImportScreen(
            onBack = close,
            onOpenAiSettings = {
                state.close(FullScreen.STATEMENT_IMPORT)
                state.open(FullScreen.AI)
            }
        )
        FullScreen.MERCHANT_MAPPINGS -> MerchantMappingsScreen(onBack = close)
        FullScreen.CATEGORIES -> CategoriesScreen(onBack = close)
        FullScreen.CURRENCY -> CurrencyScreen(onBack = close)
        FullScreen.BACKUP -> BackupScreen(onBack = close)
        FullScreen.ABOUT -> {
            val onboarding: OnboardingViewModel = koinViewModel()
            AboutScreen(
                onBack = close,
                onReplayOnboarding = {
                    close()
                    onboarding.resetOnboarding()
                }
            )
        }
    }
}
