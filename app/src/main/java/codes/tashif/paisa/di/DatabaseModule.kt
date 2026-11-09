package codes.tashif.paisa.di

import android.content.Context
import codes.tashif.paisa.data.AccountDao
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.BudgetDao
import codes.tashif.paisa.data.CategoryDao
import codes.tashif.paisa.data.DebtDao
import codes.tashif.paisa.data.MerchantMappingDao
import codes.tashif.paisa.data.RecurringTransactionDao
import codes.tashif.paisa.data.SavingsGoalDao
import codes.tashif.paisa.data.SettingsDao
import codes.tashif.paisa.data.TransactionDao
import codes.tashif.paisa.data.UnrecognizedSmsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** Same instance as [AppDatabase.getDatabase], which sync and tests still call. */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideSavingsGoalDao(db: AppDatabase): SavingsGoalDao = db.savingsGoalDao()

    @Provides
    fun provideRecurringTransactionDao(db: AppDatabase): RecurringTransactionDao =
        db.recurringTransactionDao()

    @Provides fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()

    @Provides
    fun provideUnrecognizedSmsDao(db: AppDatabase): UnrecognizedSmsDao = db.unrecognizedSmsDao()

    @Provides
    fun provideMerchantMappingDao(db: AppDatabase): MerchantMappingDao = db.merchantMappingDao()

    @Provides fun provideDebtDao(db: AppDatabase): DebtDao = db.debtDao()
}
