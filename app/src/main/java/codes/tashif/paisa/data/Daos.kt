package codes.tashif.paisa.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class TransactionWithDetails(
    val id: Int,
    val amount: Double,
    val type: String,
    val categoryId: Int,
    val accountId: Int,
    val note: String,
    val transactionDate: String,
    val createdAt: String,
    val updatedAt: String,
    val attachmentPath: String?,
    val tags: String,
    val merchantName: String?,
    val bankName: String?,
    val smsBody: String?,
    val smsSender: String?,
    val accountNumber: String?,
    val balanceAfter: Double?,
    val transactionHash: String?,
    val source: String,
    val currency: String,
    val reference: String?,
    val isDeleted: Boolean,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val accountName: String,
    val accountIcon: String,
    val accountColor: String
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY orderIndex ASC, name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Int): Account?

    @Query("SELECT * FROM accounts WHERE name = :name LIMIT 1")
    suspend fun getAccountByName(name: String): Account?

    @Query(
        "SELECT * FROM accounts WHERE bankName = :bankName AND accountLast4 = :accountLast4 LIMIT 1"
    )
    suspend fun getAccountByBankAndLast4(bankName: String, accountLast4: String): Account?

    @Query("SELECT * FROM accounts WHERE bankName = :bankName LIMIT 1")
    suspend fun getAccountByBankName(bankName: String): Account?

    @Query(
        """
        SELECT * FROM accounts
        WHERE bankName = :bankName AND (accountLast4 IS NULL OR accountLast4 = 'WALLET')
        LIMIT 1
        """
    )
    suspend fun getAccountByBankWithoutLast4(bankName: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int

    @Query("UPDATE transactions SET accountId = :targetId WHERE accountId IN (:sourceIds)")
    suspend fun reassignTransactions(sourceIds: List<Int>, targetId: Int)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsForAccount(accountId: Int)

    @Query("UPDATE accounts SET isDefault = 0")
    suspend fun clearDefaultAccount()

    @Query("UPDATE accounts SET isDefault = 1 WHERE id = :id")
    suspend fun markDefaultAccount(id: Int)

    @Query("UPDATE accounts SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun setAccountOrder(id: Int, orderIndex: Int)

    @Query(
        """
        SELECT balanceAfter FROM transactions
        WHERE accountId = :accountId AND balanceAfter IS NOT NULL AND isDeleted = 0
        ORDER BY transactionDate DESC LIMIT 1
        """
    )
    suspend fun getLatestBalanceAfter(accountId: Int): Double?

    @Query(
        "UPDATE recurring_transactions SET accountId = :targetId WHERE accountId IN (:sourceIds)"
    )
    suspend fun reassignRecurringTransactions(sourceIds: List<Int>, targetId: Int)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY orderIndex ASC, name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY orderIndex ASC, name ASC")
    fun getCategoriesByType(type: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): Category?

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type LIMIT 1")
    suspend fun getCategoryByName(name: String, type: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Update
    suspend fun updateCategories(categories: List<Category>)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon, c.color as categoryColor,
               a.name as accountName, a.icon as accountIcon, a.color as accountColor
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        INNER JOIN accounts a ON t.accountId = a.id
        WHERE t.isDeleted = 0
        ORDER BY t.transactionDate DESC
        """
    )
    fun getAllTransactions(): Flow<List<TransactionWithDetails>>

    @Query(
        """
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon, c.color as categoryColor,
               a.name as accountName, a.icon as accountIcon, a.color as accountColor
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        INNER JOIN accounts a ON t.accountId = a.id
        WHERE t.accountId = :accountId AND t.isDeleted = 0
        ORDER BY t.transactionDate DESC
        """
    )
    fun getTransactionsForAccount(accountId: Int): Flow<List<TransactionWithDetails>>

    @Query(
        """
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon, c.color as categoryColor,
               a.name as accountName, a.icon as accountIcon, a.color as accountColor
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        INNER JOIN accounts a ON t.accountId = a.id
        WHERE t.id = :id
        """
    )
    suspend fun getTransactionById(id: Int): TransactionWithDetails?

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getRawTransactionById(id: Int): Transaction?

    @Query("SELECT * FROM transactions WHERE transactionHash = :hash LIMIT 1")
    suspend fun getTransactionByHash(hash: String): Transaction?

    @Query(
        """
        SELECT * FROM transactions
        WHERE isDeleted = 0
          AND type = :type
          AND amount BETWEEN :amountLow AND :amountHigh
          AND substr(transactionDate, 1, 10) BETWEEN :fromDate AND :toDate
        ORDER BY transactionDate DESC
        """
    )
    suspend fun findSimilarTransactions(
        type: String,
        amountLow: Double,
        amountHigh: Double,
        fromDate: String,
        toDate: String
    ): List<Transaction>

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId AND isDeleted = 0")
    suspend fun getTransactionCountForCategory(categoryId: Int): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId AND isDeleted = 0")
    suspend fun getTransactionCountForAccount(accountId: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteTransaction(id: Int, updatedAt: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getBudgetsForPeriod(month: Int, year: Int): Flow<List<Budget>>

    @Query(
        "SELECT * FROM budgets WHERE categoryId = :categoryId AND month = :month AND year = :year LIMIT 1"
    )
    suspend fun getBudgetForCategory(categoryId: Int?, month: Int, year: Int): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY targetDate ASC")
    fun getAllSavingsGoals(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getSavingsGoalById(id: Int): SavingsGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(savingsGoal: SavingsGoal): Long

    @Update
    suspend fun updateSavingsGoal(savingsGoal: SavingsGoal)

    @Delete
    suspend fun deleteSavingsGoal(savingsGoal: SavingsGoal)

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAllSavingsGoals()
}

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE enabled = 1")
    suspend fun getEnabledRecurringTransactions(): List<RecurringTransaction>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getRecurringTransactionById(id: Int): RecurringTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction): Long

    @Update
    suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction)

    @Delete
    suspend fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction)

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAllRecurringTransactions()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<Settings?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): Settings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: Settings): Long

    @Update
    suspend fun updateSettings(settings: Settings)
}

@Dao
interface UnrecognizedSmsDao {
    @Query("SELECT * FROM unrecognized_sms WHERE reviewed = 0 ORDER BY timestamp DESC")
    fun getPending(): Flow<List<UnrecognizedSms>>

    @Query("SELECT COUNT(*) FROM unrecognized_sms WHERE reviewed = 0")
    fun getPendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sms: UnrecognizedSms): Long

    @Query("UPDATE unrecognized_sms SET reviewed = 1 WHERE id = :id")
    suspend fun markReviewed(id: Int)

    @Query("DELETE FROM unrecognized_sms WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM unrecognized_sms")
    suspend fun deleteAll()
}

@Dao
interface MerchantMappingDao {
    @Query("SELECT * FROM merchant_mappings ORDER BY merchantName ASC")
    fun getAllMappings(): Flow<List<MerchantMapping>>

    @Query("SELECT * FROM merchant_mappings ORDER BY merchantName ASC")
    suspend fun getAllMappingsList(): List<MerchantMapping>

    @Query(
        """
        SELECT * FROM merchant_mappings
        WHERE lower(merchantName) = lower(:merchantName)
        LIMIT 1
        """
    )
    suspend fun getMappingForMerchant(merchantName: String): MerchantMapping?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mapping: MerchantMapping)

    @Query("DELETE FROM merchant_mappings WHERE merchantName = :merchantName")
    suspend fun deleteByMerchant(merchantName: String)

    @Query(
        """
        UPDATE transactions SET categoryId = :categoryId, updatedAt = :updatedAt
        WHERE isDeleted = 0 AND merchantName IS NOT NULL
          AND lower(merchantName) = lower(:merchantName)
        """
    )
    suspend fun updateTransactionsForMerchant(
        merchantName: String,
        categoryId: Int,
        updatedAt: String
    ): Int
}
