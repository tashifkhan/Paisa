package codes.tashif.paisa.ui.app

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import codes.tashif.paisa.ui.ai.AiScreen
import codes.tashif.paisa.ui.backup.BackupScreen
import codes.tashif.paisa.ui.onboarding.OnboardingViewModel
import codes.tashif.paisa.ui.settings.AboutScreen
import codes.tashif.paisa.ui.settings.AppearanceScreen
import codes.tashif.paisa.ui.settings.CategoriesScreen
import codes.tashif.paisa.ui.settings.CurrencyScreen
import codes.tashif.paisa.ui.settings.MerchantMappingsScreen
import codes.tashif.paisa.ui.sms.SmsSetupScreen
import codes.tashif.paisa.ui.statement.StatementImportScreen

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
