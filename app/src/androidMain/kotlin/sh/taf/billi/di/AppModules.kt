package sh.taf.billi.di

import androidx.work.WorkManager
import sh.taf.billi.ai.AiCredentialsStore
import sh.taf.billi.ai.AiRepository
import sh.taf.billi.statement.StatementExtractionService
import sh.taf.billi.statement.StatementImportUseCase
import sh.taf.billi.ai.catalog.LiveModelLister
import sh.taf.billi.ai.catalog.ModelCatalog
import sh.taf.billi.ai.ladder.AiLadderRunner
import sh.taf.billi.ai.provider.ProviderClient
import sh.taf.billi.ai.store.AiConfigStore
import sh.taf.billi.ai.store.AiKeyStore
import sh.taf.billi.ai.store.LegacyMigrator
import sh.taf.billi.backup.BackupManager
import sh.taf.billi.backup.BackupPrefs
import sh.taf.billi.backup.BackupWorker
import sh.taf.billi.core.AppLockGuard
import sh.taf.billi.core.AppNavigator
import sh.taf.billi.core.PrivacySession
import sh.taf.billi.core.SnackbarManager
import sh.taf.billi.data.AppDatabase
import sh.taf.billi.data.BilliDatabase
import sh.taf.billi.data.repository.AccountRepository
import sh.taf.billi.data.repository.BudgetRepository
import sh.taf.billi.data.repository.CategoryRepository
import sh.taf.billi.data.repository.DebtRepository
import sh.taf.billi.data.repository.SettingsRepository
import sh.taf.billi.data.repository.SmsInboxRepository
import sh.taf.billi.data.repository.TransactionRepository
import sh.taf.billi.sms.SmsReaderWorker
import sh.taf.billi.ingest.TransactionIngestor
import sh.taf.billi.ingest.TransactionWriter
import sh.taf.billi.sync.SyncManager
import sh.taf.billi.sync.SyncWorker
import sh.taf.billi.ui.accounts.AccountsViewModel
import sh.taf.billi.ui.ai.AiLadderViewModel
import sh.taf.billi.ui.ai.AiProviderViewModel
import sh.taf.billi.ui.ai.AiViewModel
import sh.taf.billi.ui.analytics.AnalyticsViewModel
import sh.taf.billi.ui.app.AppViewModel
import sh.taf.billi.ui.backup.BackupViewModel
import sh.taf.billi.ui.budgets.BudgetsViewModel
import sh.taf.billi.ui.debts.DebtsViewModel
import sh.taf.billi.ui.home.HomeViewModel
import sh.taf.billi.ui.onboarding.OnboardingViewModel
import sh.taf.billi.ui.categories.CategoriesViewModel
import sh.taf.billi.ui.settings.SettingsViewModel
import sh.taf.billi.export.TransactionExporter
import sh.taf.billi.export.PdfStatementRenderer
import sh.taf.billi.export.AndroidPdfStatementRenderer
import sh.taf.billi.ui.app.AppPlatform
import sh.taf.billi.ui.app.AndroidAppPlatform
import sh.taf.billi.ui.sms.SmsViewModel
import sh.taf.billi.ui.statement.StatementImportViewModel
import sh.taf.billi.ui.transaction.TransactionViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Database, its DAOs and WorkManager. */
val databaseModule = module {
    // Same instance as AppDatabase.getDatabase, which sync and tests still call.
    single { BilliDatabase.get(androidContext()) }
    single { WorkManager.getInstance(androidContext()) }
}

/** Android-only singletons: SMS, the AI stores on SharedPreferences, backup and sync. */
val appModule = module {

    single { TransactionWriter(get(), get(), get(), get()) }
    single { TransactionIngestor(get(), get(), get()) }

    single { AiConfigStore(sh.taf.billi.ai.store.SharedPrefsAiStore.aiConfig(androidContext())) }
    single { AiCredentialsStore(sh.taf.billi.ai.store.SharedPrefsAiStore.aiCredentials(androidContext())) }


    factory { TransactionExporter(get(), get(), getOrNull()) }
    single<PdfStatementRenderer> { AndroidPdfStatementRenderer() }
    single<AppPlatform> { AndroidAppPlatform(androidContext()) }
    single<sh.taf.billi.assistant.AssistantAccess> { sh.taf.billi.assistant.PrefsAssistantAccess(androidContext()) }
}


val workerModule = module {
    workerOf(::SmsReaderWorker)
    workerOf(::BackupWorker)
    workerOf(::SyncWorker)
}

val billiModules = listOf(
    databaseModule, daoModule, coreModule, aiServicesModule, cloudModule, appModule, viewModelModule, workerModule
)
