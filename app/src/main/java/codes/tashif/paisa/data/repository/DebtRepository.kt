package codes.tashif.paisa.data.repository

import androidx.room.withTransaction
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.DebtEntry
import codes.tashif.paisa.data.DebtPerson
import codes.tashif.paisa.data.DebtPersonBalance
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** People you lend to or borrow from, and the entries that make up each balance. */
@Singleton
class DebtRepository @Inject constructor(private val db: AppDatabase) {

    private val debtDao = db.debtDao()
    private val transactionDao = db.transactionDao()

    /** Everyone you lend to or borrow from, with what they owe you now. */
    val debtPeople: Flow<List<DebtPersonBalance>> = debtDao.getPeopleWithBalances()

    fun debtEntries(personId: Int): Flow<List<DebtEntry>> = debtDao.getEntries(personId)

    /** Finds [name] (any case) or adds them. A non-zero [startingAmount] is recorded as their first entry. */
    suspend fun addDebtPerson(name: String, startingAmount: Double = 0.0, note: String = ""): Int =
        db.withTransaction {
            val clean = name.trim()
            require(clean.isNotEmpty()) { "Name required" }
            val now = nowIso()
            val id = debtDao.getPersonByName(clean)?.id
                ?: debtDao.insertPerson(DebtPerson(name = clean, createdAt = now)).toInt()
            if (startingAmount != 0.0) {
                debtDao.insertEntry(
                    DebtEntry(
                        personId = id,
                        amount = startingAmount,
                        note = note.ifBlank { "Starting balance" },
                        entryDate = now,
                        createdAt = now
                    )
                )
            }
            id
        }

    suspend fun renameDebtPerson(person: DebtPerson, name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty()) debtDao.updatePerson(person.copy(name = clean))
    }

    /** Removes the person and their whole history. */
    suspend fun deleteDebtPerson(person: DebtPerson) = debtDao.deletePerson(person)

    /** [amount] > 0: they owe you more. < 0: they owe you less (or you owe them). */
    suspend fun addDebtEntry(
        personId: Int,
        amount: Double,
        note: String,
        entryDate: String = nowIso(),
        transactionSyncId: String? = null
    ): Long = debtDao.insertEntry(
        DebtEntry(
            personId = personId,
            amount = amount,
            note = note.trim(),
            entryDate = entryDate,
            transactionSyncId = transactionSyncId,
            createdAt = nowIso()
        )
    )

    suspend fun deleteDebtEntry(id: Int) = debtDao.deleteEntry(id)

    /** Brings the person's balance to zero with one closing entry. */
    suspend fun settleDebt(personId: Int) = db.withTransaction {
        val balance = debtDao.getBalance(personId)
        if (kotlin.math.abs(balance) >= 0.005) {
            addDebtEntry(personId, -balance, "Settled up")
        }
    }

    /**
     * Records [amount] of a transaction against a person. Money you paid out
     * (an expense) means they owe you; money you received means they paid you
     * back, or you now owe them. The transaction itself is left as it is.
     */
    suspend fun linkTransactionToDebt(
        transactionId: Int,
        personId: Int?,
        newPersonName: String?,
        amount: Double
    ): Int = db.withTransaction {
        val tx = requireNotNull(transactionDao.getRawTransactionById(transactionId)) { "No transaction $transactionId" }
        val id = personId ?: addDebtPerson(requireNotNull(newPersonName) { "Person name required" })
        val what = tx.merchantName?.takeIf { it.isNotBlank() } ?: tx.note.ifBlank { "Transaction" }
        addDebtEntry(
            personId = id,
            amount = if (tx.type == "expense") amount else -amount,
            note = what,
            entryDate = tx.transactionDate,
            transactionSyncId = tx.syncId
        )
        id
    }
}
