package com.paisa.app.data

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
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val accountName: String,
    val accountIcon: String,
    val accountColor: String
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Int): Account?

    @Query("SELECT * FROM accounts WHERE name = :name LIMIT 1")
    suspend fun getAccountByName(name: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int
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
        WHERE t.accountId = :accountId
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

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun getTransactionCountForCategory(categoryId: Int): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun getTransactionCountForAccount(accountId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

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
