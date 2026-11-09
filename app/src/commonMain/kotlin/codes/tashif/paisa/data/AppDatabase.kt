package codes.tashif.paisa.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import codes.tashif.paisa.data.sql.Sql
import codes.tashif.paisa.sync.SyncSchema

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
        MerchantMapping::class,
        DebtPerson::class,
        DebtEntry::class
    ],
    version = AppDatabase.SCHEMA_VERSION,
    exportSchema = false
)
@ConstructedBy(AppDatabaseConstructor::class)
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
    abstract fun debtDao(): DebtDao

    companion object {
        /** Bump with every migration. Backups made on a newer schema are refused. */
        const val SCHEMA_VERSION = 12

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                val db = Sql.of(connection)
                db.exec(
                    "ALTER TABLE settings ADD COLUMN expressiveUi INTEGER NOT NULL DEFAULT 1"
                )
                db.exec(
                    "ALTER TABLE settings ADD COLUMN amoledDark INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override suspend fun migrate(connection: SQLiteConnection) {
                val db = Sql.of(connection)
                db.exec("ALTER TABLE accounts ADD COLUMN creditLimit REAL")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override suspend fun migrate(connection: SQLiteConnection) {
                val db = Sql.of(connection)
                db.exec(
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
                db.exec(
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
            override suspend fun migrate(connection: SQLiteConnection) {
                val db = Sql.of(connection)
                db.exec(
                    "ALTER TABLE accounts ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0"
                )
                db.exec(
                    "ALTER TABLE accounts ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override suspend fun migrate(connection: SQLiteConnection) {
                val db = Sql.of(connection)
                db.exec(
                    "ALTER TABLE settings ADD COLUMN hideBalancesByDefault INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override suspend fun migrate(connection: SQLiteConnection) {
                val db = Sql.of(connection)
                // DEFAULT 1 must match Settings.onboardingCompleted @ColumnInfo(defaultValue).
                // Existing installs skip the tour; fresh installs get 0 via onCreate seed.
                db.exec(
                    "ALTER TABLE settings ADD COLUMN onboardingCompleted INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override suspend fun migrate(connection: SQLiteConnection) {
                val db = Sql.of(connection)
                db.exec(
                    "ALTER TABLE settings ADD COLUMN highContrast INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override suspend fun migrate(connection: SQLiteConnection) {
                val db = Sql.of(connection)
                // Only tables that existed in 9; later ones are created with these columns.
                for (table in SyncSchema.existingTables(db)) {
                    db.exec("ALTER TABLE `${table.name}` ADD COLUMN syncId TEXT")
                    db.exec(
                        "ALTER TABLE `${table.name}` ADD COLUMN syncStamp INTEGER NOT NULL DEFAULT 0"
                    )
                    db.exec(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_${table.name}_syncId " +
                            "ON `${table.name}` (syncId)"
                    )
                }
                db.exec("ALTER TABLE accounts ADD COLUMN balanceAnchor REAL NOT NULL DEFAULT 0")
                db.exec("ALTER TABLE accounts ADD COLUMN balanceAnchorAt TEXT NOT NULL DEFAULT ''")
                // Stamp 1: existing data beats a fresh install's seed rows (stamp 0)
                // and loses to any edit made from now on.
                SyncSchema.backfillIds(db, stamp = 1)
                SyncSchema.deriveAnchors(db)
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override suspend fun migrate(connection: SQLiteConnection) {
                val db = Sql.of(connection)
                db.exec("ALTER TABLE settings ADD COLUMN floatingNavBar INTEGER NOT NULL DEFAULT 0")
                db.exec("ALTER TABLE settings ADD COLUMN navBarLabels INTEGER NOT NULL DEFAULT 1")
            }
        }

        /** Debts (people and what they owe) and a configurable navbar. */
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override suspend fun migrate(connection: SQLiteConnection) {
                val db = Sql.of(connection)
                db.exec(
                    "CREATE TABLE IF NOT EXISTS `debt_people` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `createdAt` TEXT NOT NULL, " +
                        "`syncId` TEXT, `syncStamp` INTEGER NOT NULL DEFAULT 0)"
                )
                db.exec(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_debt_people_syncId` ON `debt_people` (`syncId`)"
                )
                db.exec(
                    "CREATE TABLE IF NOT EXISTS `debt_entries` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`personId` INTEGER NOT NULL, `amount` REAL NOT NULL, `note` TEXT NOT NULL, " +
                        "`entryDate` TEXT NOT NULL, `transactionSyncId` TEXT, `createdAt` TEXT NOT NULL, " +
                        "`syncId` TEXT, `syncStamp` INTEGER NOT NULL DEFAULT 0, " +
                        "FOREIGN KEY(`personId`) REFERENCES `debt_people`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.exec(
                    "CREATE INDEX IF NOT EXISTS `index_debt_entries_personId` ON `debt_entries` (`personId`)"
                )
                db.exec(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_debt_entries_syncId` ON `debt_entries` (`syncId`)"
                )
                db.exec(
                    "ALTER TABLE settings ADD COLUMN navTabs TEXT NOT NULL " +
                        "DEFAULT '${Settings.DEFAULT_NAV_TABS}'"
                )
            }
        }

        internal val MIGRATIONS: Array<Migration>
            get() = arrayOf(
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12
            )

        /** Seeds a new database and keeps the sync triggers installed. Tests add it too. */
        val callback: RoomDatabase.Callback get() = DatabaseCallback

        private object DatabaseCallback : RoomDatabase.Callback() {
            override suspend fun onCreate(connection: SQLiteConnection) {
                super.onCreate(connection)
                seed(Sql.of(connection))
            }

            override suspend fun onDestructiveMigration(connection: SQLiteConnection) {
                super.onDestructiveMigration(connection)
                seed(Sql.of(connection))
            }

            override suspend fun onOpen(connection: SQLiteConnection) {
                super.onOpen(connection)
                SyncSchema.install(Sql.of(connection))
            }

            /**
             * Seed rows get natural sync ids with stamp 0, so a second device's fresh
             * defaults never overwrite edits the first device made to the same rows.
             */
            private suspend fun seed(db: Sql) {
                SyncSchema.install(db)
                SyncSchema.setApplying(db, true)
                try {
                    seedDatabase(db)
                    SyncSchema.backfillIds(db, stamp = 0)
                } finally {
                    SyncSchema.setApplying(db, false)
                }
            }

            private suspend fun seedDatabase(db: Sql) {
                try {
                    db.exec(
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

                    db.exec(
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

                    db.exec(
                        "INSERT INTO accounts (id, name, type, openingBalance, currentBalance, icon, color, bankName, accountLast4) " +
                            "VALUES (1, 'Cash', 'Cash', 0.0, 0.0, 'wallet', '#FFD6A5', NULL, NULL)"
                    )
                    db.exec(
                        "INSERT INTO accounts (id, name, type, openingBalance, currentBalance, icon, color, bankName, accountLast4) " +
                            "VALUES (2, 'Bank Account', 'Bank Account', 0.0, 0.0, 'account_balance', '#A0C4FF', NULL, NULL)"
                    )

                    db.exec(
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

/** The callback, migrations and fallback every platform's builder needs. */
fun RoomDatabase.Builder<AppDatabase>.withPaisaDefaults(): RoomDatabase.Builder<AppDatabase> =
    addCallback(AppDatabase.callback)
        .addMigrations(*AppDatabase.MIGRATIONS)
        .fallbackToDestructiveMigration(dropAllTables = true)

/** File name of the database on every platform. */
const val DATABASE_NAME = "paisa_database"

// Room's KSP processor writes the actual for each platform.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
