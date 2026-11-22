package sh.taf.billi.di

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
import sh.taf.billi.ui.onboarding.OnboardingViewModel
import sh.taf.billi.ui.categories.CategoriesViewModel
import sh.taf.billi.ui.settings.SettingsViewModel
import sh.taf.billi.ui.sms.SmsViewModel
import sh.taf.billi.ui.notifications.NotificationImportViewModel
import sh.taf.billi.ui.notifications.AccountApprovalViewModel
import sh.taf.billi.notifications.AccountApproval
import sh.taf.billi.ui.statement.StatementImportViewModel
import sh.taf.billi.ui.quickadd.AiAddViewModel
import sh.taf.billi.ai.BillScanService
import sh.taf.billi.ai.QuickAddService
import sh.taf.billi.ui.transaction.TransactionViewModel
import sh.taf.billi.ai.AiRepository
import sh.taf.billi.statement.StatementExtractionService
import sh.taf.billi.statement.StatementImportUseCase
import sh.taf.billi.ai.catalog.LiveModelLister
import sh.taf.billi.ai.catalog.ModelCatalog
import sh.taf.billi.ai.ladder.AiLadderRunner
import sh.taf.billi.ai.provider.ProviderClient
import sh.taf.billi.ai.store.AiKeyStore
import sh.taf.billi.ai.store.LegacyMigrator
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/** DAOs from the platform's [AppDatabase] binding. */
val daoModule = module {
    single { get<AppDatabase>().accountDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().transactionDao() }
    single { get<AppDatabase>().budgetDao() }
    single { get<AppDatabase>().savingsGoalDao() }
    single { get<AppDatabase>().recurringTransactionDao() }
    single { get<AppDatabase>().settingsDao() }
    single { get<AppDatabase>().unrecognizedSmsDao() }
    single { get<AppDatabase>().merchantMappingDao() }
    single { get<AppDatabase>().merchantRenameDao() }
    single { get<AppDatabase>().debtDao() }
}

/** Repositories and app-wide state, the same on every platform. */
val coreModule = module {
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
    single { sh.taf.billi.assistant.AssistantActions(get(), get(), get(), get(), get()) }
    // Needs NotificationImportService, which each platform binds.
    singleOf(::AccountApproval)
}

/** The AI services. Each platform binds the stores, the cache and the notifier. */
val aiServicesModule = module {
    singleOf(::AiKeyStore)
    singleOf(::LegacyMigrator)
    singleOf(::ModelCatalog)
    singleOf(::LiveModelLister)
    singleOf(::ProviderClient)
    singleOf(::AiLadderRunner)
    singleOf(::AiRepository)
    singleOf(::StatementExtractionService)
    singleOf(::StatementImportUseCase)
    singleOf(::QuickAddService)
    singleOf(::BillScanService)
}

/** Every screen's viewmodel. */
val viewModelModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::AnalyticsViewModel)
    viewModelOf(::BudgetsViewModel)
    viewModelOf(::AccountsViewModel)
    viewModelOf(::DebtsViewModel)
    viewModelOf(::TransactionViewModel)
    viewModelOf(::AiAddViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::CategoriesViewModel)
    viewModelOf(::SmsViewModel)
    viewModelOf(::NotificationImportViewModel)
    viewModelOf(::AccountApprovalViewModel)
    viewModelOf(::StatementImportViewModel)
    viewModelOf(::BackupViewModel)
    viewModelOf(::AiViewModel)
    viewModelOf(::AiProviderViewModel)
    viewModelOf(::AiLadderViewModel)
    viewModelOf(::LocalAiViewModel)
}

/** Backup and sync. Each platform binds BackupPrefs, CloudPlatform and BackupServiceHooks. */
val cloudModule = module {
    single { sh.taf.billi.backup.BackupManager(get(), get(), get()) }
    single { sh.taf.billi.sync.SyncManager(get(), get(), get()) }
    single<sh.taf.billi.backup.BackupService> {
        sh.taf.billi.backup.CloudBackupService(get(), get(), get())
    }
}
