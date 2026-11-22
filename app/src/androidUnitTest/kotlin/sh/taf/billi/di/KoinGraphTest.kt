package sh.taf.billi.di

import sh.taf.billi.ai.AiCredentialsStore
import sh.taf.billi.ai.AiRepository
import sh.taf.billi.ai.BillScanService
import sh.taf.billi.ai.QuickAddService
import sh.taf.billi.statement.StatementExtractionService
import sh.taf.billi.statement.StatementImportUseCase
import sh.taf.billi.ai.catalog.LiveModelLister
import sh.taf.billi.ai.catalog.ModelCatalog
import sh.taf.billi.ai.ladder.AiLadderRunner
import sh.taf.billi.ai.local.LocalAiEngine
import sh.taf.billi.ai.provider.ProviderClient
import sh.taf.billi.ai.store.AiConfigStore
import sh.taf.billi.ai.store.AiKeyStore
import sh.taf.billi.ai.store.LegacyMigrator
import sh.taf.billi.backup.BackupManager
import sh.taf.billi.backup.BackupPrefs
import sh.taf.billi.core.AppLockGuard
import sh.taf.billi.core.AppNavigator
import sh.taf.billi.core.PrivacySession
import sh.taf.billi.core.SnackbarManager
import sh.taf.billi.data.AppDatabase
import sh.taf.billi.data.repository.AccountRepository
import sh.taf.billi.data.repository.BudgetRepository
import sh.taf.billi.data.repository.CategoryRepository
import sh.taf.billi.data.repository.DebtRepository
import sh.taf.billi.data.repository.SettingsRepository
import sh.taf.billi.data.repository.SmsInboxRepository
import sh.taf.billi.data.repository.TransactionRepository
import sh.taf.billi.notifications.NotificationImportService
import sh.taf.billi.ingest.PendingAiProcessor
import sh.taf.billi.ingest.TransactionIngestor
import sh.taf.billi.sync.SyncManager
import sh.taf.billi.ui.accounts.AccountsViewModel
import sh.taf.billi.ui.ai.AiLadderViewModel
import sh.taf.billi.ui.ai.AiProviderViewModel
import sh.taf.billi.ui.ai.AiViewModel
import sh.taf.billi.ui.ai.LocalAiViewModel
import sh.taf.billi.ui.analytics.AnalyticsViewModel
import sh.taf.billi.ui.app.AppViewModel
import sh.taf.billi.ui.backup.BackupViewModel
import sh.taf.billi.ui.budgets.BudgetsViewModel
import sh.taf.billi.ui.debts.DebtsViewModel
import sh.taf.billi.ui.home.HomeViewModel
import sh.taf.billi.ui.notifications.NotificationImportViewModel
import sh.taf.billi.ui.onboarding.OnboardingViewModel
import sh.taf.billi.ui.categories.CategoriesViewModel
import sh.taf.billi.ui.settings.SettingsViewModel
import sh.taf.billi.export.TransactionExporter
import sh.taf.billi.ui.sms.SmsViewModel
import sh.taf.billi.ui.statement.StatementImportViewModel
import sh.taf.billi.ui.transaction.TransactionViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.mp.KoinPlatform
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Koin finds a missing or wrong binding only when something asks for it.
 * BilliApplication starts the real graph; this resolves every definition.
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
        koin.get<TransactionIngestor>()
        koin.get<PendingAiProcessor>()
        koin.get<NotificationImportService>()
        koin.get<AiKeyStore>()
        koin.get<LegacyMigrator>()
        koin.get<ModelCatalog>()
        koin.get<LiveModelLister>()
        koin.get<ProviderClient>()
        koin.get<LocalAiEngine>()
        koin.get<AiLadderRunner>()
        koin.get<AiRepository>()
        koin.get<StatementExtractionService>()
        koin.get<StatementImportUseCase>()
        koin.get<QuickAddService>()
        koin.get<BillScanService>()
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
        koin.get<NotificationImportViewModel>()
        koin.get<StatementImportViewModel>()
        koin.get<BackupViewModel>()
        koin.get<AiViewModel>()
        koin.get<AiProviderViewModel>()
        koin.get<AiLadderViewModel>()
        koin.get<LocalAiViewModel>()
        koin.get<AiConfigStore>()
        koin.get<AiCredentialsStore>()
        koin.get<BackupPrefs>()
        koin.get<SyncManager>()
        koin.get<AppDatabase>()
    }
}
