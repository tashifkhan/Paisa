package codes.tashif.paisa.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts", indices = [Index(value = ["syncId"], unique = true)])
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // Cash, Bank Account, UPI, Credit Card, Debit Card, Wallet, Other
    val openingBalance: Double,
    val currentBalance: Double,
    val icon: String,
    val color: String,
    /** Bank name from SMS parsers, e.g. "HDFC Bank" */
    val bankName: String? = null,
    /** Last 4 digits of account/card when known from SMS */
    val accountLast4: String? = null,
    /** Total credit limit for Credit Card accounts, from SMS or manual entry */
    val creditLimit: Double? = null,
    /** Manual sort position in the accounts list (lower = first) */
    @ColumnInfo(defaultValue = "0") val orderIndex: Int = 0,
    /** Preselected account for new transactions; at most one is true */
    @ColumnInfo(defaultValue = "0") val isDefault: Boolean = false,
    /**
     * Known balance at [balanceAnchorAt]: the opening balance, a balance a bank SMS
     * reported, or one the user typed in. [currentBalance] is this plus every
     * transaction dated after [balanceAnchorAt], so every synced device computes
     * the same number from the same rows.
     */
    @ColumnInfo(defaultValue = "0") val balanceAnchor: Double = 0.0,
    /** ISO date-time the anchor applies to. Empty means before every transaction. */
    @ColumnInfo(defaultValue = "''") val balanceAnchorAt: String = "",
    /** Stable id shared by every device that syncs this row. Set by the sync triggers. */
    val syncId: String? = null,
    /** Last-write-wins clock for sync. Set by the sync triggers; don't write it from app code. */
    @ColumnInfo(defaultValue = "0") val syncStamp: Long = 0
)

@Entity(tableName = "categories", indices = [Index(value = ["syncId"], unique = true)])
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // income or expense
    val icon: String,
    val color: String,
    val isDefault: Boolean = false,
    val orderIndex: Int = 0,
    /** Stable id shared by every device that syncs this row. Set by the sync triggers. */
    val syncId: String? = null,
    /** Last-write-wins clock for sync. Set by the sync triggers; don't write it from app code. */
    @ColumnInfo(defaultValue = "0") val syncStamp: Long = 0
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("transactionDate"),
        Index("categoryId"),
        Index("accountId"),
        Index(value = ["transactionHash"], unique = true),
        Index(value = ["syncId"], unique = true)
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // income or expense
    val categoryId: Int,
    val accountId: Int,
    val note: String,
    val transactionDate: String, // ISO-8601
    val createdAt: String,
    val updatedAt: String,
    val attachmentPath: String? = null,
    val tags: String = "",
    // SMS / bank fields
    val merchantName: String? = null,
    val bankName: String? = null,
    val smsBody: String? = null,
    val smsSender: String? = null,
    val accountNumber: String? = null,
    val balanceAfter: Double? = null,
    val transactionHash: String? = null,
    val source: String = "manual", // manual | sms
    val currency: String = "INR",
    val reference: String? = null,
    val isDeleted: Boolean = false,
    /** Stable id shared by every device that syncs this row. Set by the sync triggers. */
    val syncId: String? = null,
    /** Last-write-wins clock for sync. Set by the sync triggers; don't write it from app code. */
    @ColumnInfo(defaultValue = "0") val syncStamp: Long = 0
)

@Entity(tableName = "budgets", indices = [Index(value = ["syncId"], unique = true)])
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int?,
    val budgetAmount: Double,
    val month: Int,
    val year: Int,
    val budgetType: String = "MONTHLY",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val budgetName: String? = null,
    /** Stable id shared by every device that syncs this row. Set by the sync triggers. */
    val syncId: String? = null,
    /** Last-write-wins clock for sync. Set by the sync triggers; don't write it from app code. */
    @ColumnInfo(defaultValue = "0") val syncStamp: Long = 0
)

@Entity(tableName = "savings_goals", indices = [Index(value = ["syncId"], unique = true)])
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: String,
    val status: String,
    val icon: String,
    val color: String,
    /** Stable id shared by every device that syncs this row. Set by the sync triggers. */
    val syncId: String? = null,
    /** Last-write-wins clock for sync. Set by the sync triggers; don't write it from app code. */
    @ColumnInfo(defaultValue = "0") val syncStamp: Long = 0
)

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("nextExecutionDate"),
        Index("categoryId"),
        Index("accountId"),
        Index(value = ["syncId"], unique = true)
    ]
)
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String,
    val categoryId: Int,
    val accountId: Int,
    val note: String,
    val frequency: String, // daily, weekly, monthly, yearly
    val nextExecutionDate: String,
    val enabled: Boolean = true,
    /** Stable id shared by every device that syncs this row. Set by the sync triggers. */
    val syncId: String? = null,
    /** Last-write-wins clock for sync. Set by the sync triggers; don't write it from app code. */
    @ColumnInfo(defaultValue = "0") val syncStamp: Long = 0
)

@Entity(tableName = "settings", indices = [Index(value = ["syncId"], unique = true)])
data class Settings(
    @PrimaryKey val id: Int = 1,
    val themeMode: String = "system", // system, light, dark
    val currency: String = "₹",
    val pinEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val colorPalette: String = "dynamic", // PaisaPalette id; "dynamic" = wallpaper colors
    val lastSmsScanAt: Long = 0L,
    val smsScanEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "1") val expressiveUi: Boolean = true,
    @ColumnInfo(defaultValue = "0") val amoledDark: Boolean = false,
    /** Stronger text/outline contrast (accessibility / less soft M3 look). */
    @ColumnInfo(defaultValue = "0") val highContrast: Boolean = false,
    /** When true, home-screen balances and amounts start hidden. */
    @ColumnInfo(defaultValue = "1") val hideBalancesByDefault: Boolean = true,
    /**
     * First-run feature tour completed (or skipped).
     *
     * SQL default is 1 so existing installs that migrate skip the tour.
     * Fresh installs still get 0 via [AppDatabase] seed insert.
     * Kotlin default stays false for in-memory construction.
     */
    @ColumnInfo(defaultValue = "1") val onboardingCompleted: Boolean = false,
    /** Pill-shaped bottom bar floating above the screen edge instead of the docked one. */
    @ColumnInfo(defaultValue = "0") val floatingNavBar: Boolean = false,
    /** Docked bar only: label every tab, not just the selected one. */
    @ColumnInfo(defaultValue = "1") val navBarLabels: Boolean = true,
    /** Sync covers only [currency]; see SyncSchema. Set by the sync triggers. */
    val syncId: String? = null,
    @ColumnInfo(defaultValue = "0") val syncStamp: Long = 0
)

/**
 * Bank-like SMS that no registered parser claimed — kept for review / future parsers.
 */
@Entity(
    tableName = "unrecognized_sms",
    indices = [Index(value = ["sender", "timestamp", "bodyHash"], unique = true)]
)
data class UnrecognizedSms(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,
    val body: String,
    val bodyHash: String,
    val timestamp: Long,
    val reviewed: Boolean = false,
    val createdAt: String
)

/**
 * User-learned merchant → category mapping (PennyWise-style).
 * Applied before built-in keyword rules when saving SMS / statement rows.
 */
@Entity(tableName = "merchant_mappings", indices = [Index(value = ["syncId"], unique = true)])
data class MerchantMapping(
    @PrimaryKey val merchantName: String,
    val categoryName: String,
    val categoryType: String = "expense", // income | expense
    val createdAt: String,
    val updatedAt: String,
    /** Stable id shared by every device that syncs this row. Set by the sync triggers. */
    val syncId: String? = null,
    /** Last-write-wins clock for sync. Set by the sync triggers; don't write it from app code. */
    @ColumnInfo(defaultValue = "0") val syncStamp: Long = 0
)
