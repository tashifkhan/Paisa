package codes.tashif.paisa.di

import codes.tashif.paisa.ai.AiCredentialsStore
import codes.tashif.paisa.ai.AiRepository
import codes.tashif.paisa.ai.StatementExtractionService
import codes.tashif.paisa.ai.StatementImportUseCase
import codes.tashif.paisa.ai.catalog.LiveModelLister
import codes.tashif.paisa.ai.catalog.ModelCatalog
import codes.tashif.paisa.ai.ladder.AiLadderRunner
import codes.tashif.paisa.ai.provider.ProviderClient
import codes.tashif.paisa.ai.store.AiConfigStore
import codes.tashif.paisa.ai.store.AiKeyStore
import codes.tashif.paisa.ai.store.LegacyMigrator
import codes.tashif.paisa.backup.BackupManager
import codes.tashif.paisa.backup.BackupPrefs
import codes.tashif.paisa.core.AppLockGuard
import codes.tashif.paisa.core.AppNavigator
import codes.tashif.paisa.core.PrivacySession
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.repository.AccountRepository
import codes.tashif.paisa.data.repository.BudgetRepository
import codes.tashif.paisa.data.repository.CategoryRepository
import codes.tashif.paisa.data.repository.DebtRepository
import codes.tashif.paisa.data.repository.SettingsRepository
import codes.tashif.paisa.data.repository.SmsInboxRepository
import codes.tashif.paisa.data.repository.TransactionRepository
import codes.tashif.paisa.sms.SmsTransactionProcessor
import codes.tashif.paisa.sync.SyncManager
import codes.tashif.paisa.ui.accounts.AccountsViewModel
import codes.tashif.paisa.ui.ai.AiLadderViewModel
import codes.tashif.paisa.ui.ai.AiProviderViewModel
import codes.tashif.paisa.ui.ai.AiViewModel
import codes.tashif.paisa.ui.analytics.AnalyticsViewModel
import codes.tashif.paisa.ui.app.AppViewModel
import codes.tashif.paisa.ui.backup.BackupViewModel
import codes.tashif.paisa.ui.budgets.BudgetsViewModel
import codes.tashif.paisa.ui.debts.DebtsViewModel
import codes.tashif.paisa.ui.home.HomeViewModel
import codes.tashif.paisa.ui.onboarding.OnboardingViewModel
import codes.tashif.paisa.ui.settings.CategoriesViewModel
import codes.tashif.paisa.ui.settings.SettingsViewModel
import codes.tashif.paisa.ui.settings.TransactionExporter
import codes.tashif.paisa.ui.sms.SmsViewModel
import codes.tashif.paisa.ui.statement.StatementImportViewModel
import codes.tashif.paisa.ui.transaction.TransactionViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.mp.KoinPlatform
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Koin finds a missing or wrong binding only when something asks for it.
 * PaisaApplication starts the real graph; this resolves every definition.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KoinGraphTest {
    @Test
    fun everyDefinitionResolves() {
        val koin = KoinPlatform.getKoin()
        koin.get<AppNavigator>()
        koin.get<AppLockGuard>()
        koin.get<PrivacySession>()
        koin.get<SnackbarManager>()
        koin.get<AccountRepository>()
        koin.get<BudgetRepository>()
        koin.get<CategoryRepository>()
        koin.get<DebtRepository>()
        koin.get<SettingsRepository>()
        koin.get<SmsInboxRepository>()
        koin.get<TransactionRepository>()
        koin.get<SmsTransactionProcessor>()
        koin.get<AiKeyStore>()
        koin.get<LegacyMigrator>()
        koin.get<ModelCatalog>()
        koin.get<LiveModelLister>()
        koin.get<ProviderClient>()
        koin.get<AiLadderRunner>()
        koin.get<AiRepository>()
        koin.get<StatementExtractionService>()
        koin.get<StatementImportUseCase>()
        koin.get<BackupManager>()
        koin.get<TransactionExporter>()
        koin.get<AppViewModel>()
        koin.get<HomeViewModel>()
        koin.get<AnalyticsViewModel>()
        koin.get<BudgetsViewModel>()
        koin.get<AccountsViewModel>()
        koin.get<DebtsViewModel>()
        koin.get<TransactionViewModel>()
        koin.get<OnboardingViewModel>()
        koin.get<SettingsViewModel>()
        koin.get<CategoriesViewModel>()
        koin.get<SmsViewModel>()
        koin.get<StatementImportViewModel>()
        koin.get<BackupViewModel>()
        koin.get<AiViewModel>()
        koin.get<AiProviderViewModel>()
        koin.get<AiLadderViewModel>()
        koin.get<AiConfigStore>()
        koin.get<AiCredentialsStore>()
        koin.get<BackupPrefs>()
        koin.get<SyncManager>()
        koin.get<AppDatabase>()
    }
}
