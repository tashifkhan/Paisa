package com.paisa.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Repository(private val db: AppDatabase) {

    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()
    private val transactionDao = db.transactionDao()
    private val budgetDao = db.budgetDao()
    private val savingsGoalDao = db.savingsGoalDao()
    private val recurringTransactionDao = db.recurringTransactionDao()
    private val settingsDao = db.settingsDao()
    private val unrecognizedSmsDao = db.unrecognizedSmsDao()

    // --- ACCOUNTS ---
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()

    suspend fun getAccountById(id: Int): Account? = accountDao.getAccountById(id)

    suspend fun getAccountByBankAndLast4(bankName: String, accountLast4: String): Account? =
        accountDao.getAccountByBankAndLast4(bankName, accountLast4)

    suspend fun getAccountByBankName(bankName: String): Account? =
        accountDao.getAccountByBankName(bankName)

    suspend fun insertAccount(account: Account): Long = accountDao.insertAccount(account)

    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)

    suspend fun deleteAccount(account: Account): Boolean {
        val count = transactionDao.getTransactionCountForAccount(account.id)
        if (count > 0) return false
        accountDao.deleteAccount(account)
        return true
    }

    // --- CATEGORIES ---
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    fun getCategoriesByType(type: String): Flow<List<Category>> =
        categoryDao.getCategoriesByType(type)

    suspend fun getCategoryById(id: Int): Category? = categoryDao.getCategoryById(id)

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: Category): Boolean {
        val count = transactionDao.getTransactionCountForCategory(category.id)
        if (count > 0) return false
        categoryDao.deleteCategory(category)
        return true
    }

    /**
     * Resolve a category by name+type, creating a lightweight default if missing.
     * Used by SMS import when merchant mapping produces a category not yet seeded.
     */
    suspend fun findOrCreateCategory(name: String, type: String): Int {
        categoryDao.getCategoryByName(name, type)?.let { return it.id }
        // also try common aliases
        val aliases = listOf(name, "Others", "Other")
        for (alias in aliases) {
            categoryDao.getCategoryByName(alias, type)?.let { return it.id }
        }
        val id = categoryDao.insertCategory(
            Category(
                name = name,
                type = type,
                icon = "more_horiz",
                color = if (type == "income") "#BFFCC6" else "#E8AEB2",
                isDefault = false,
                orderIndex = 99
            )
        )
        return id.toInt()
    }

    // --- TRANSACTIONS ---
    val allTransactions: Flow<List<TransactionWithDetails>> = transactionDao.getAllTransactions()

    fun getTransactionsForAccount(accountId: Int): Flow<List<TransactionWithDetails>> =
        transactionDao.getTransactionsForAccount(accountId)

    suspend fun getTransactionById(id: Int): TransactionWithDetails? =
        transactionDao.getTransactionById(id)

    suspend fun getTransactionByHash(hash: String): Transaction? =
        transactionDao.getTransactionByHash(hash)

    suspend fun insertTransaction(transaction: Transaction): Long {
        return db.withTransaction {
            val account = accountDao.getAccountById(transaction.accountId)
            if (account != null) {
                val newBalance = if (transaction.type == "income") {
                    account.currentBalance + transaction.amount
                } else {
                    account.currentBalance - transaction.amount
                }
                accountDao.updateAccount(account.copy(currentBalance = newBalance))
            }
            transactionDao.insertTransaction(transaction)
        }
    }

    /**
     * Insert SMS-sourced transaction. If the SMS carried an absolute balance,
     * prefer that over delta adjustment for the account's currentBalance.
     */
    suspend fun insertSmsTransaction(
        transaction: Transaction,
        absoluteBalance: Double?
    ): Long {
        return db.withTransaction {
            val rowId = transactionDao.insertTransaction(transaction)
            if (rowId == -1L) return@withTransaction -1L

            val account = accountDao.getAccountById(transaction.accountId)
            if (account != null) {
                val newBalance = when {
                    absoluteBalance != null -> absoluteBalance
                    transaction.type == "income" -> account.currentBalance + transaction.amount
                    else -> account.currentBalance - transaction.amount
                }
                accountDao.updateAccount(account.copy(currentBalance = newBalance))
            }
            rowId
        }
    }

    suspend fun updateTransaction(newTransaction: Transaction) {
        db.withTransaction {
            val oldTransaction = transactionDao.getRawTransactionById(newTransaction.id)
                ?: return@withTransaction

            val oldAccount = accountDao.getAccountById(oldTransaction.accountId)
            if (oldAccount != null) {
                val reverted = if (oldTransaction.type == "income") {
                    oldAccount.currentBalance - oldTransaction.amount
                } else {
                    oldAccount.currentBalance + oldTransaction.amount
                }
                accountDao.updateAccount(oldAccount.copy(currentBalance = reverted))
            }

            val newAccount = accountDao.getAccountById(newTransaction.accountId)
            if (newAccount != null) {
                val applied = if (newTransaction.type == "income") {
                    newAccount.currentBalance + newTransaction.amount
                } else {
                    newAccount.currentBalance - newTransaction.amount
                }
                accountDao.updateAccount(newAccount.copy(currentBalance = applied))
            }

            transactionDao.updateTransaction(newTransaction)
        }
    }

    suspend fun deleteTransaction(transactionId: Int) {
        db.withTransaction {
            val transaction = transactionDao.getRawTransactionById(transactionId)
                ?: return@withTransaction
            val account = accountDao.getAccountById(transaction.accountId)
            if (account != null && !transaction.isDeleted) {
                val reverted = if (transaction.type == "income") {
                    account.currentBalance - transaction.amount
                } else {
                    account.currentBalance + transaction.amount
                }
                accountDao.updateAccount(account.copy(currentBalance = reverted))
            }
            // Soft-delete so SMS re-scan won't re-import
            if (transaction.transactionHash != null) {
                transactionDao.softDeleteTransaction(transactionId, nowIso())
            } else {
                transactionDao.deleteTransaction(transaction)
            }
        }
    }

    // --- BUDGETS ---
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()

    fun getBudgetsForPeriod(month: Int, year: Int): Flow<List<Budget>> =
        budgetDao.getBudgetsForPeriod(month, year)

    suspend fun insertBudget(budget: Budget): Long = budgetDao.insertBudget(budget)

    suspend fun deleteBudget(budget: Budget) = budgetDao.deleteBudget(budget)

    // --- SAVINGS GOALS ---
    val allSavingsGoals: Flow<List<SavingsGoal>> = savingsGoalDao.getAllSavingsGoals()

    suspend fun insertSavingsGoal(goal: SavingsGoal): Long =
        savingsGoalDao.insertSavingsGoal(goal)

    suspend fun updateSavingsGoal(goal: SavingsGoal) = savingsGoalDao.updateSavingsGoal(goal)

    suspend fun deleteSavingsGoal(goal: SavingsGoal) = savingsGoalDao.deleteSavingsGoal(goal)

    // --- RECURRING ---
    val allRecurringTransactions: Flow<List<RecurringTransaction>> =
        recurringTransactionDao.getAllRecurringTransactions()

    suspend fun insertRecurringTransaction(rule: RecurringTransaction): Long =
        recurringTransactionDao.insertRecurringTransaction(rule)

    suspend fun updateRecurringTransaction(rule: RecurringTransaction) =
        recurringTransactionDao.updateRecurringTransaction(rule)

    suspend fun deleteRecurringTransaction(rule: RecurringTransaction) =
        recurringTransactionDao.deleteRecurringTransaction(rule)

    suspend fun processRecurringTransactions() {
        db.withTransaction {
            val nowStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dueRules = recurringTransactionDao.getEnabledRecurringTransactions()
            for (rule in dueRules) {
                if (rule.nextExecutionDate > nowStr) continue
                var execDateStr = rule.nextExecutionDate
                while (execDateStr <= nowStr) {
                    val transaction = Transaction(
                        amount = rule.amount,
                        type = rule.type,
                        categoryId = rule.categoryId,
                        accountId = rule.accountId,
                        note = "[Recurring] ${rule.note}",
                        transactionDate = "${execDateStr}T09:00:00",
                        createdAt = nowIso(),
                        updatedAt = nowIso(),
                        source = "manual"
                    )
                    val account = accountDao.getAccountById(rule.accountId)
                    if (account != null) {
                        val newBalance = if (rule.type == "income") {
                            account.currentBalance + rule.amount
                        } else {
                            account.currentBalance - rule.amount
                        }
                        accountDao.updateAccount(account.copy(currentBalance = newBalance))
                    }
                    transactionDao.insertTransaction(transaction)
                    execDateStr = advanceDate(execDateStr, rule.frequency)
                }
                recurringTransactionDao.updateRecurringTransaction(
                    rule.copy(nextExecutionDate = execDateStr)
                )
            }
        }
    }

    private fun advanceDate(dateStr: String, frequency: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: return dateStr
        val calendar = Calendar.getInstance().apply { time = date }
        when (frequency.lowercase(Locale.getDefault())) {
            "daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "yearly" -> calendar.add(Calendar.YEAR, 1)
            else -> calendar.add(Calendar.MONTH, 1)
        }
        return sdf.format(calendar.time)
    }

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

    // --- SETTINGS ---
    val appSettings: Flow<Settings?> = settingsDao.getSettings()

    suspend fun getSettingsDirect(): Settings? = settingsDao.getSettingsDirect()

    suspend fun updateSettings(settings: Settings) = settingsDao.updateSettings(settings)

    // --- UNRECOGNIZED SMS ---
    val pendingUnrecognizedSms: Flow<List<UnrecognizedSms>> = unrecognizedSmsDao.getPending()
    val pendingUnrecognizedCount: Flow<Int> = unrecognizedSmsDao.getPendingCount()

    suspend fun insertUnrecognizedSms(sms: UnrecognizedSms): Long =
        unrecognizedSmsDao.insert(sms)

    suspend fun markUnrecognizedReviewed(id: Int) = unrecognizedSmsDao.markReviewed(id)

    suspend fun deleteUnrecognized(id: Int) = unrecognizedSmsDao.delete(id)
}
