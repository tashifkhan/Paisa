package codes.tashif.paisa.data.repository

import androidx.room.withTransaction
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.DebtEntry
import codes.tashif.paisa.data.DebtEntryWithPerson
import codes.tashif.paisa.data.DebtPerson
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.data.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * One person's part of a split. [personId] is someone already in Debts; when
 * it's null, [newName] is found or added. The sign of [amount] depends on the call.
 */
data class DebtShare(val personId: Int?, val newName: String?, val amount: Double)

/** People you lend to or borrow from, and the entries that make up each balance. */
@Singleton
class DebtRepository @Inject constructor(private val db: AppDatabase) {

    private val debtDao = db.debtDao()
    private val transactionDao = db.transactionDao()

    /** Everyone you lend to or borrow from, with what they owe you now. */
    val debtPeople: Flow<List<DebtPersonBalance>> = debtDao.getPeopleWithBalances()

    fun debtEntries(personId: Int): Flow<List<DebtEntry>> = debtDao.getEntries(personId)

    /** The entries tied to a transaction, which together are its split. */
    fun transactionSplit(transactionSyncId: String): Flow<List<DebtEntryWithPerson>> =
        debtDao.observeEntriesForTransaction(transactionSyncId)

    /** The live transaction with [syncId], for opening it from a debt entry. */
    suspend fun transactionIdForSyncId(syncId: String): Int? = transactionDao.getIdBySyncId(syncId)

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

    /** Changes an entry added by hand. Entries from a transaction follow the transaction instead. */
    suspend fun updateDebtEntry(id: Int, amount: Double, note: String, entryDate: String) {
        val entry = debtDao.getEntry(id) ?: return
        debtDao.updateEntry(entry.copy(amount = amount, note = note.trim(), entryDate = entryDate))
    }

    suspend fun deleteDebtEntry(id: Int) = debtDao.deleteEntry(id)

    /** Brings the person's balance to zero with one closing entry. */
    suspend fun settleDebt(personId: Int) = db.withTransaction {
        val balance = debtDao.getBalance(personId)
        if (abs(balance) >= 0.005) {
            addDebtEntry(personId, -balance, "Settled up")
        }
    }

    /**
     * Records [amount] of a transaction against one person, replacing any split
     * the transaction already had. Returns the person's id.
     */
    suspend fun linkTransactionToDebt(
        transactionId: Int,
        personId: Int?,
        newPersonName: String?,
        amount: Double
    ): Int = saveTransactionSplit(transactionId, listOf(DebtShare(personId, newPersonName, amount))).first()

    /**
     * Makes [shares] the transaction's split. Amounts are positive. Money you
     * paid out (an expense) means they owe you; money you received means they
     * paid you back, or you now owe them. The transaction itself is left as it
     * is. Returns the people's ids in order.
     *
     * Each linked entry has the id `split:<transaction syncId>:<person syncId>`,
     * so two devices that save the same split write the same rows and sync
     * merges them row by row. Saving updates the rows of people who stay,
     * inserts new people and deletes the rest, which leaves tombstones.
     * Older entries with random ids move onto the fixed id here.
     */
    suspend fun saveTransactionSplit(transactionId: Int, shares: List<DebtShare>): List<Int> =
        db.withTransaction {
            val tx = requireNotNull(transactionDao.getRawTransactionById(transactionId)) { "No transaction $transactionId" }
            val txSyncId = requireNotNull(tx.syncId) { "Transaction $transactionId has no syncId" }
            val sign = if (tx.type == "income") -1.0 else 1.0
            val title = transactionTitle(tx)

            // Resolve people first, in order, summing two shares for the same person.
            val order = ArrayList<Int>()
            val totals = LinkedHashMap<Int, Double>()
            shares.filter { it.amount > 0.0 }.forEach { share ->
                val id = resolvePerson(share)
                order += id
                totals[id] = toPaise((totals[id] ?: 0.0) + share.amount)
            }

            val existing = debtDao.getEntriesForTransaction(txSyncId).groupBy { it.personId }
            // People no longer in the split lose every row they had.
            existing.filterKeys { it !in totals }.values.flatten().forEach { debtDao.deleteEntry(it.id) }

            totals.forEach { (personId, amount) ->
                val wanted = debtDao.getPerson(personId)?.syncId?.let { splitEntrySyncId(txSyncId, it) }
                val rows = existing[personId].orEmpty()
                val keep = rows.firstOrNull { wanted != null && it.syncId == wanted }
                    ?: rows.firstOrNull().takeIf { wanted == null }
                rows.filter { it !== keep }.forEach { debtDao.deleteEntry(it.id) }
                if (keep != null) {
                    debtDao.updateEntry(
                        keep.copy(amount = sign * amount, note = title, entryDate = tx.transactionDate)
                    )
                } else {
                    debtDao.insertEntry(
                        DebtEntry(
                            personId = personId,
                            amount = sign * amount,
                            note = title,
                            entryDate = tx.transactionDate,
                            transactionSyncId = txSyncId,
                            createdAt = nowIso(),
                            syncId = wanted
                        )
                    )
                }
            }
            order
        }

    /** Removes the transaction's split. The people stay in Debts. */
    suspend fun removeTransactionSplit(transactionId: Int) {
        val syncId = transactionDao.getRawTransactionById(transactionId)?.syncId ?: return
        debtDao.deleteEntriesForTransaction(syncId)
    }

    /**
     * A split that isn't tied to a transaction, such as a bill someone else paid.
     * [shares] are signed: > 0 they owe you, < 0 you owe them.
     */
    suspend fun recordBillSplit(shares: List<DebtShare>, note: String, entryDate: String) =
        db.withTransaction {
            val what = note.trim().ifBlank { "Split bill" }
            shares.filter { it.amount != 0.0 }.forEach { share ->
                addDebtEntry(resolvePerson(share), share.amount, what, entryDate)
            }
        }

    /**
     * Keeps a transaction's split in step after an edit. Called by
     * [TransactionRepository] inside its own database transaction.
     * Entries scale with the amount, so shares stay proportional, flip sign when
     * the type flips between expense and income, and take the new date. A note
     * that still matches the old title takes the new title.
     */
    suspend fun onTransactionChanged(old: Transaction, new: Transaction) {
        val syncId = old.syncId ?: return
        val entries = debtDao.getEntriesForTransaction(syncId)
        if (entries.isEmpty()) return
        val oldPaise = (old.amount * 100).roundToLong()
        val newPaise = (new.amount * 100).roundToLong()
        val scaled = if (oldPaise > 0L) {
            SplitScaling.scale(entries.map { it.id to (it.amount * 100).roundToLong() }, newPaise, oldPaise)
        } else {
            entries.associate { it.id to (it.amount * 100).roundToLong() }
        }
        val flip = old.type != new.type && setOf(old.type, new.type) == setOf("expense", "income")
        val oldTitle = transactionTitle(old)
        val newTitle = transactionTitle(new)
        entries.forEach { entry ->
            val paise = scaled.getValue(entry.id) * if (flip) -1 else 1
            debtDao.updateEntry(
                entry.copy(
                    amount = paise / 100.0,
                    entryDate = new.transactionDate,
                    note = if (entry.note == oldTitle) newTitle else entry.note
                )
            )
        }
    }

    /** Drops the split of a deleted transaction. Called inside the delete's database transaction. */
    suspend fun onTransactionDeleted(transaction: Transaction) {
        transaction.syncId?.let { debtDao.deleteEntriesForTransaction(it) }
    }

    private suspend fun resolvePerson(share: DebtShare): Int =
        share.personId ?: addDebtPerson(requireNotNull(share.newName) { "Person name required" })

    private fun transactionTitle(tx: Transaction): String =
        tx.merchantName?.takeIf { it.isNotBlank() } ?: tx.note.ifBlank { "Transaction" }

    private fun toPaise(amount: Double): Double = (amount * 100).roundToLong() / 100.0

    companion object {
        /** The fixed syncId of one person's entry in a transaction's split. */
        fun splitEntrySyncId(transactionSyncId: String, personSyncId: String): String =
            "split:$transactionSyncId:$personSyncId"
    }
}
