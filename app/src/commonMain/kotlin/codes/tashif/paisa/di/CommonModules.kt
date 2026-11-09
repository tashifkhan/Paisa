package codes.tashif.paisa.di

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
}
