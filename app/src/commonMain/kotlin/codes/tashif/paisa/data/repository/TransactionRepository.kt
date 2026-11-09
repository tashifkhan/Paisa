package codes.tashif.paisa.data.repository

import codes.tashif.paisa.platform.PaisaCalendar
import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.platform.parseDate
import codes.tashif.paisa.platform.nowMillis
import androidx.room3.withWriteTransaction
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.RecurringTransaction
import codes.tashif.paisa.data.Transaction
import codes.tashif.paisa.data.TransactionWithDetails
import kotlinx.coroutines.flow.Flow

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
        val parsed = parseDate(day, "yyyy-MM-dd", machine = true) ?: return null
        val cal = PaisaCalendar.getInstance()
        cal.timeInMillis = parsed
        cal.add(PaisaCalendar.DAY_OF_MONTH, -windowDays)
        val from = formatDate(cal.timeInMillis, "yyyy-MM-dd", machine = true)
        cal.add(PaisaCalendar.DAY_OF_MONTH, 2 * windowDays)
        val to = formatDate(cal.timeInMillis, "yyyy-MM-dd", machine = true)
        return transactionDao.findSimilarTransactions(
            type = type,
            amountLow = amount - 0.01,
            amountHigh = amount + 0.01,
            fromDate = from,
            toDate = to
        ).minByOrNull {
            // Prefer the entry closest to the statement row's date
            parseDate(it.transactionDate.take(10), "yyyy-MM-dd", machine = true)
                ?.let { t -> kotlin.math.abs(t - parsed) }
                ?: Long.MAX_VALUE
        }
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return db.withWriteTransaction {
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
        return db.withWriteTransaction {
            val rowId = transactionDao.insertTransaction(transaction)
            if (rowId == -1L) return@withWriteTransaction -1L

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
        db.withWriteTransaction {
            val oldTransaction = transactionDao.getRawTransactionById(newTransaction.id)
                ?: return@withWriteTransaction
            transactionDao.updateTransaction(newTransaction)
            debtRepository.onTransactionChanged(oldTransaction, newTransaction)
            accountDao.recomputeBalance(oldTransaction.accountId)
            if (newTransaction.accountId != oldTransaction.accountId) {
                accountDao.recomputeBalance(newTransaction.accountId)
            }
        }
    }

    suspend fun deleteTransaction(transactionId: Int) {
        db.withWriteTransaction {
            val transaction = transactionDao.getRawTransactionById(transactionId)
                ?: return@withWriteTransaction
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
        db.withWriteTransaction {
            val nowStr = formatDate(nowMillis(), "yyyy-MM-dd")
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
        val date = parseDate(dateStr, "yyyy-MM-dd") ?: return dateStr
        val calendar = PaisaCalendar.getInstance().apply { timeInMillis = date }
        when (frequency.lowercase()) {
            "daily" -> calendar.add(PaisaCalendar.DAY_OF_YEAR, 1)
            "weekly" -> calendar.add(PaisaCalendar.WEEK_OF_YEAR, 1)
            "yearly" -> calendar.add(PaisaCalendar.YEAR, 1)
            else -> calendar.add(PaisaCalendar.MONTH, 1)
        }
        return formatDate(calendar.timeInMillis, "yyyy-MM-dd")
    }
}
