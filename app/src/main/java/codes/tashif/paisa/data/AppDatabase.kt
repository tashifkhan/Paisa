package codes.tashif.paisa.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Account::class,
        Category::class,
        Transaction::class,
        Budget::class,
        SavingsGoal::class,
        RecurringTransaction::class,
        Settings::class,
        UnrecognizedSms::class,
        MerchantMapping::class
    ],
    version = 9,
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
    abstract fun unrecognizedSmsDao(): UnrecognizedSmsDao
    abstract fun merchantMappingDao(): MerchantMappingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN expressiveUi INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN amoledDark INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN creditLimit REAL")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS merchant_mappings (
                        merchantName TEXT NOT NULL PRIMARY KEY,
                        categoryName TEXT NOT NULL,
                        categoryType TEXT NOT NULL DEFAULT 'expense',
                        createdAt TEXT NOT NULL,
                        updatedAt TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                // Seed extra expense categories used by CategoryMapping
                db.execSQL(
                    "INSERT OR IGNORE INTO categories (name, type, icon, color, isDefault, orderIndex) VALUES " +
                        "('Bank Charges', 'expense', 'account_balance', '#FFCAD4', 0, 17)," +
                        "('Credit Card Payment', 'expense', 'credit_card', '#A0C4FF', 0, 18)," +
                        "('Banking', 'expense', 'account_balance_wallet', '#BDB2FF', 0, 19)," +
                        "('Personal Care', 'expense', 'spa', '#FFDFBA', 0, 20)," +
                        "('Tax', 'expense', 'receipt', '#E8AEB2', 0, 21)," +
                        "('Investments', 'expense', 'trending_up', '#9BF6FF', 0, 22)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE accounts ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE accounts ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN hideBalancesByDefault INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // DEFAULT 1 must match Settings.onboardingCompleted @ColumnInfo(defaultValue).
                // Existing installs skip the tour; fresh installs get 0 via onCreate seed.
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN onboardingCompleted INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN highContrast INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "paisa_database"
                )
                    .addCallback(DatabaseCallback())
                    .addMigrations(
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
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
                    db.execSQL(
                        "INSERT OR IGNORE INTO categories (name, type, icon, color, isDefault, orderIndex) VALUES " +
                            "('Salary', 'income', 'payments', '#BFFCC6', 0, 1)," +
                            "('Business', 'income', 'storefront', '#CAFFBF', 0, 2)," +
                            "('Investments', 'income', 'trending_up', '#9BF6FF', 0, 3)," +
                            "('Gifts', 'income', 'card_giftcard', '#FFFFBA', 0, 4)," +
                            "('Freelance', 'income', 'computer', '#FFCAD4', 0, 5)," +
                            "('Refunds', 'income', 'receipt_long', '#D8B4FE', 0, 6)," +
                            "('Interest', 'income', 'savings', '#B9FBC0', 0, 7)," +
                            "('Dividends', 'income', 'account_balance', '#CAFFBF', 0, 8)," +
                            "('Cashback', 'income', 'redeem', '#FFCAD4', 0, 9)," +
                            "('Income', 'income', 'payments', '#BFFCC6', 0, 10)," +
                            "('Other', 'income', 'more_horiz', '#E8AEB2', 0, 11)"
                    )

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
                            "('Insurance', 'expense', 'shield', '#FCF6BD', 0, 12)," +
                            "('Fuel', 'expense', 'local_gas_station', '#FFD6A5', 0, 13)," +
                            "('UPI Transfer', 'expense', 'swap_horiz', '#A0C4FF', 0, 14)," +
                            "('Bank Charges', 'expense', 'account_balance', '#FFCAD4', 0, 15)," +
                            "('Credit Card Payment', 'expense', 'credit_card', '#A0C4FF', 0, 16)," +
                            "('Banking', 'expense', 'account_balance_wallet', '#BDB2FF', 0, 17)," +
                            "('Personal Care', 'expense', 'spa', '#FFDFBA', 0, 18)," +
                            "('Tax', 'expense', 'receipt', '#E8AEB2', 0, 19)," +
                            "('Investments', 'expense', 'trending_up', '#9BF6FF', 0, 20)," +
                            "('Others', 'expense', 'more_horiz', '#E8AEB2', 0, 21)," +
                            "('Other', 'expense', 'more_horiz', '#E8AEB2', 0, 22)"
                    )

                    db.execSQL(
                        "INSERT INTO accounts (id, name, type, openingBalance, currentBalance, icon, color, bankName, accountLast4) " +
                            "VALUES (1, 'Cash', 'Cash', 0.0, 0.0, 'wallet', '#FFD6A5', NULL, NULL)"
                    )
                    db.execSQL(
                        "INSERT INTO accounts (id, name, type, openingBalance, currentBalance, icon, color, bankName, accountLast4) " +
                            "VALUES (2, 'Bank Account', 'Bank Account', 0.0, 0.0, 'account_balance', '#A0C4FF', NULL, NULL)"
                    )

                    db.execSQL(
                        "INSERT OR IGNORE INTO settings " +
                            "(id, themeMode, currency, pinEnabled, biometricEnabled, notificationsEnabled, colorPalette, lastSmsScanAt, smsScanEnabled, hideBalancesByDefault, onboardingCompleted) " +
                            "VALUES (1, 'system', '₹', 0, 0, 1, 'dynamic', 0, 1, 1, 0)"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
