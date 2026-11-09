package codes.tashif.paisa.data.repository

import androidx.room.withTransaction
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.RecurringTransaction
import codes.tashif.paisa.data.Transaction
import codes.tashif.paisa.data.TransactionWithDetails
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Transactions and recurring rules. Every write keeps the account balance in
 * step, and edits and deletes keep the transaction's debt split in step too.
 */
class TransactionRepository(
    private val db: AppDatabase,
    private val accountRepository: AccountRepository,
    private val debtRepository: DebtRepository
) {

    private val accountDao = db.accountDao()
    private val transactionDao = db.transactionDao()
    private val recurringTransactionDao = db.recurringTransactionDao()

    // --- TRANSACTIONS ---
    val allTransactions: Flow<List<TransactionWithDetails>> = transactionDao.getAllTransactions()

    fun getTransactionsForAccount(accountId: Int): Flow<List<TransactionWithDetails>> =
        transactionDao.getTransactionsForAccount(accountId)

    suspend fun getTransactionById(id: Int): TransactionWithDetails? =
        transactionDao.getTransactionById(id)

    suspend fun getRawTransactionById(id: Int): Transaction? =
        transactionDao.getRawTransactionById(id)

    suspend fun getTransactionByHash(hash: String): Transaction? =
        transactionDao.getTransactionByHash(hash)

    /**
     * Fuzzy match against already-logged transactions (SMS/manual/statement):
     * same type, same amount (±0.01), date within ±[windowDays] days — bank posting
     * dates often lag the SMS by a day or two.
     */
    suspend fun findSimilarTransaction(
        amount: Double,
        type: String,
        dateIso: String,
        windowDays: Int = 2
    ): Transaction? {
        val day = dateIso.take(10)
        if (day.length != 10) return null
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = runCatching { fmt.parse(day) }.getOrNull() ?: return null
        val cal = Calendar.getInstance()
        cal.time = parsed
        cal.add(Calendar.DAY_OF_MONTH, -windowDays)
        val from = fmt.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, 2 * windowDays)
        val to = fmt.format(cal.time)
        return transactionDao.findSimilarTransactions(
            type = type,
            amountLow = amount - 0.01,
            amountHigh = amount + 0.01,
            fromDate = from,
            toDate = to
        ).minByOrNull {
            // Prefer the entry closest to the statement row's date
            runCatching { fmt.parse(it.transactionDate.take(10))?.time }
                .getOrNull()
                ?.let { t -> kotlin.math.abs(t - parsed.time) }
                ?: Long.MAX_VALUE
        }
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return db.withTransaction {
            val id = transactionDao.insertTransaction(transaction)
            accountDao.recomputeBalance(transaction.accountId)
            id
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

            if (absoluteBalance != null) {
                // The reported balance already includes this transaction.
                accountRepository.applyReportedBalance(
                    transaction.accountId,
                    absoluteBalance,
                    transaction.transactionDate
                )
            }
            accountDao.recomputeBalance(transaction.accountId)
            rowId
        }
    }

    suspend fun updateTransaction(newTransaction: Transaction) {
        db.withTransaction {
            val oldTransaction = transactionDao.getRawTransactionById(newTransaction.id)
                ?: return@withTransaction
            transactionDao.updateTransaction(newTransaction)
            debtRepository.onTransactionChanged(oldTransaction, newTransaction)
            accountDao.recomputeBalance(oldTransaction.accountId)
            if (newTransaction.accountId != oldTransaction.accountId) {
                accountDao.recomputeBalance(newTransaction.accountId)
            }
        }
    }

    suspend fun deleteTransaction(transactionId: Int) {
        db.withTransaction {
            val transaction = transactionDao.getRawTransactionById(transactionId)
                ?: return@withTransaction
            // Soft-delete so SMS re-scan won't re-import
            if (transaction.transactionHash != null) {
                transactionDao.softDeleteTransaction(transactionId, nowIso())
            } else {
                transactionDao.deleteTransaction(transaction)
            }
            debtRepository.onTransactionDeleted(transaction)
            accountDao.recomputeBalance(transaction.accountId)
        }
    }

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
            val touched = mutableSetOf<Int>()
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
                        source = "manual",
                        // Same id on every device, so two synced phones that both run
                        // this rule for the same day produce one transaction, not two.
                        syncId = rule.syncId?.let { "rec:$it:$execDateStr" }
                    )
                    transactionDao.insertTransaction(transaction)
                    touched += rule.accountId
                    execDateStr = advanceDate(execDateStr, rule.frequency)
                }
                recurringTransactionDao.updateRecurringTransaction(
                    rule.copy(nextExecutionDate = execDateStr)
                )
            }
            touched.forEach { accountDao.recomputeBalance(it) }
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
}
