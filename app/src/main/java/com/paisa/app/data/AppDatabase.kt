package com.paisa.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Account::class,
        Category::class,
        Transaction::class,
        Budget::class,
        SavingsGoal::class,
        RecurringTransaction::class,
        Settings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "paisa_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedDatabase(db)
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                seedDatabase(db)
            }

            private fun seedDatabase(db: SupportSQLiteDatabase) {
                try {
                    // Income categories
                    db.execSQL(
                        "INSERT OR IGNORE INTO categories (name, type, icon, color, isDefault, orderIndex) VALUES " +
                            "('Salary', 'income', 'payments', '#BFFCC6', 0, 1)," +
                            "('Business', 'income', 'storefront', '#CAFFBF', 0, 2)," +
                            "('Investments', 'income', 'trending_up', '#9BF6FF', 0, 3)," +
                            "('Gifts', 'income', 'card_giftcard', '#FFFFBA', 0, 4)," +
                            "('Freelance', 'income', 'computer', '#FFCAD4', 0, 5)," +
                            "('Other', 'income', 'more_horiz', '#E8AEB2', 0, 6)"
                    )

                    // Expense categories
                    db.execSQL(
                        "INSERT OR IGNORE INTO categories (name, type, icon, color, isDefault, orderIndex) VALUES " +
                            "('Food & Dining', 'expense', 'restaurant', '#FFDFBA', 0, 1)," +
                            "('Shopping', 'expense', 'shopping_bag', '#FFB3BA', 0, 2)," +
                            "('Transport', 'expense', 'directions_car', '#BDB2FF', 0, 3)," +
                            "('Bills & Utilities', 'expense', 'receipt_long', '#D8B4FE', 0, 4)," +
                            "('Health & Fitness', 'expense', 'medical_services', '#FFCAD4', 0, 5)," +
                            "('Entertainment', 'expense', 'sports_esports', '#A0C4FF', 0, 6)," +
                            "('Groceries', 'expense', 'shopping_cart', '#D0F4DE', 0, 7)," +
                            "('Rent & Housing', 'expense', 'home', '#FCE1E4', 0, 8)," +
                            "('Education', 'expense', 'school', '#E2F0D9', 0, 9)," +
                            "('Subscriptions', 'expense', 'subscriptions', '#E4C1F9', 0, 10)," +
                            "('Travel', 'expense', 'flight', '#D0F4EA', 0, 11)," +
                            "('Other', 'expense', 'more_horiz', '#E8AEB2', 0, 12)"
                    )

                    // Default accounts
                    db.execSQL(
                        "INSERT INTO accounts (id, name, type, openingBalance, currentBalance, icon, color) " +
                            "VALUES (1, 'Cash', 'Cash', 0.0, 0.0, 'wallet', '#FFD6A5')"
                    )
                    db.execSQL(
                        "INSERT INTO accounts (id, name, type, openingBalance, currentBalance, icon, color) " +
                            "VALUES (2, 'Bank Account', 'Bank Account', 0.0, 0.0, 'account_balance', '#A0C4FF')"
                    )

                    // Settings (INR default)
                    db.execSQL(
                        "INSERT OR IGNORE INTO settings " +
                            "(id, themeMode, currency, pinEnabled, biometricEnabled, notificationsEnabled, colorPalette) " +
                            "VALUES (1, 'system', '₹', 0, 0, 1, 'Default')"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
