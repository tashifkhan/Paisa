package codes.tashif.paisa.di

import androidx.work.WorkManager
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
import codes.tashif.paisa.backup.BackupWorker
import codes.tashif.paisa.core.AppLockGuard
import codes.tashif.paisa.core.AppNavigator
import codes.tashif.paisa.core.PrivacySession
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.PaisaDatabase
import codes.tashif.paisa.data.repository.AccountRepository
import codes.tashif.paisa.data.repository.BudgetRepository
import codes.tashif.paisa.data.repository.CategoryRepository
import codes.tashif.paisa.data.repository.DebtRepository
import codes.tashif.paisa.data.repository.SettingsRepository
import codes.tashif.paisa.data.repository.SmsInboxRepository
import codes.tashif.paisa.data.repository.TransactionRepository
import codes.tashif.paisa.sms.SmsReaderWorker
import codes.tashif.paisa.sms.SmsTransactionProcessor
import codes.tashif.paisa.sync.SyncManager
import codes.tashif.paisa.sync.SyncWorker
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
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Database, its DAOs and WorkManager. */
val databaseModule = module {
    // Same instance as AppDatabase.getDatabase, which sync and tests still call.
    single { PaisaDatabase.get(androidContext()) }
    single { get<AppDatabase>().accountDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().transactionDao() }
    single { get<AppDatabase>().budgetDao() }
    single { get<AppDatabase>().savingsGoalDao() }
    single { get<AppDatabase>().recurringTransactionDao() }
    single { get<AppDatabase>().settingsDao() }
    single { get<AppDatabase>().unrecognizedSmsDao() }
    single { get<AppDatabase>().merchantMappingDao() }
    single { get<AppDatabase>().debtDao() }
    single { WorkManager.getInstance(androidContext()) }
}

/** App-wide singletons: repositories, stores, AI, backup and sync. */
val appModule = module {
    singleOf(::AppNavigator)
    singleOf(::AppLockGuard)
    singleOf(::PrivacySession)
    singleOf(::SnackbarManager)

    singleOf(::AccountRepository)
    singleOf(::BudgetRepository)
    singleOf(::CategoryRepository)
    singleOf(::DebtRepository)
    singleOf(::SettingsRepository)
    singleOf(::SmsInboxRepository)
    singleOf(::TransactionRepository)
    singleOf(::SmsTransactionProcessor)

    single { AiConfigStore(codes.tashif.paisa.ai.store.SharedPrefsAiStore.aiConfig(androidContext())) }
    single { AiCredentialsStore(codes.tashif.paisa.ai.store.SharedPrefsAiStore.aiCredentials(androidContext())) }
    singleOf(::AiKeyStore)
    singleOf(::LegacyMigrator)
    singleOf(::ModelCatalog)
    singleOf(::LiveModelLister)
    singleOf(::ProviderClient)
    singleOf(::AiLadderRunner)
    singleOf(::AiRepository)
    singleOf(::StatementExtractionService)
    singleOf(::StatementImportUseCase)

    single { BackupPrefs(androidContext()) }
    singleOf(::BackupManager)
    single { SyncManager(androidContext(), get(), get()) }

    factoryOf(::TransactionExporter)
}

val viewModelModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::AnalyticsViewModel)
    viewModelOf(::BudgetsViewModel)
    viewModelOf(::AccountsViewModel)
    viewModelOf(::DebtsViewModel)
    viewModelOf(::TransactionViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::CategoriesViewModel)
    viewModelOf(::SmsViewModel)
    viewModelOf(::StatementImportViewModel)
    viewModelOf(::BackupViewModel)
    viewModelOf(::AiViewModel)
    viewModelOf(::AiProviderViewModel)
    viewModelOf(::AiLadderViewModel)
}

val workerModule = module {
    workerOf(::SmsReaderWorker)
    workerOf(::BackupWorker)
    workerOf(::SyncWorker)
}

val paisaModules = listOf(databaseModule, appModule, viewModelModule, workerModule)
