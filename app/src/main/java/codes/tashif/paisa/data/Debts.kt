package codes.tashif.paisa.data

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/**
 * Someone you lend to or borrow from. Debts live apart from accounts and
 * transactions: they never change a balance or count as income or spending.
 */
@Entity(tableName = "debt_people", indices = [Index(value = ["syncId"], unique = true)])
data class DebtPerson(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: String,
    /** Stable id shared by every device that syncs this row. Set by the sync triggers. */
    val syncId: String? = null,
    /** Last-write-wins clock for sync. Set by the sync triggers; don't write it from app code. */
    @ColumnInfo(defaultValue = "0") val syncStamp: Long = 0
)

/**
 * One change to what a person owes. Positive [amount] means they owe you more
 * (you lent, or paid for them); negative means they owe you less (they paid
 * you back, or you borrowed from them). The person's balance is the sum.
 */
@Entity(
    tableName = "debt_entries",
    foreignKeys = [
        ForeignKey(
            entity = DebtPerson::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("personId"), Index(value = ["syncId"], unique = true)]
)
data class DebtEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personId: Int,
    val amount: Double,
    val note: String,
    /** ISO-8601, same format as [Transaction.transactionDate]. */
    val entryDate: String,
    /**
     * syncId of the transaction this came from, if any. Every entry with the same
     * transaction is that transaction's split. Editing the transaction rescales
     * them and deleting it removes them.
     */
    val transactionSyncId: String? = null,
    val createdAt: String,
    /** Stable id shared by every device that syncs this row. Set by the sync triggers. */
    val syncId: String? = null,
    /** Last-write-wins clock for sync. Set by the sync triggers; don't write it from app code. */
    @ColumnInfo(defaultValue = "0") val syncStamp: Long = 0
)

/** A person and what they owe you right now (negative: what you owe them). */
data class DebtPersonBalance(
    @Embedded val person: DebtPerson,
    val balance: Double
)

/** A debt entry with the name of the person it belongs to, for a transaction's split. */
data class DebtEntryWithPerson(
    @Embedded val entry: DebtEntry,
    val personName: String
)

/**
 * Entries count only when they stand alone or their transaction still exists.
 * A safety net for entries left behind by a delete on another device or by old data.
 */
private const val LIVE_ENTRY = """
    (e.transactionSyncId IS NULL OR EXISTS (
        SELECT 1 FROM transactions t WHERE t.syncId = e.transactionSyncId AND t.isDeleted = 0
    ))
"""

@Dao
interface DebtDao {
    @Query(
        """
        SELECT p.*, COALESCE(SUM(e.amount), 0) AS balance
        FROM debt_people p LEFT JOIN debt_entries e ON e.personId = p.id AND $LIVE_ENTRY
        GROUP BY p.id
        ORDER BY ABS(COALESCE(SUM(e.amount), 0)) DESC, p.name COLLATE NOCASE
        """
    )
    fun getPeopleWithBalances(): Flow<List<DebtPersonBalance>>

    @Query(
        """
        SELECT e.* FROM debt_entries e
        WHERE e.personId = :personId AND $LIVE_ENTRY
        ORDER BY e.entryDate DESC, e.id DESC
        """
    )
    fun getEntries(personId: Int): Flow<List<DebtEntry>>

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM debt_entries e WHERE e.personId = :personId AND $LIVE_ENTRY")
    suspend fun getBalance(personId: Int): Double

    @Query("SELECT * FROM debt_entries WHERE transactionSyncId = :transactionSyncId ORDER BY id")
    suspend fun getEntriesForTransaction(transactionSyncId: String): List<DebtEntry>

    @Query(
        """
        SELECT e.*, p.name AS personName
        FROM debt_entries e INNER JOIN debt_people p ON p.id = e.personId
        WHERE e.transactionSyncId = :transactionSyncId
        ORDER BY e.id
        """
    )
    fun observeEntriesForTransaction(transactionSyncId: String): Flow<List<DebtEntryWithPerson>>

    @Query("DELETE FROM debt_entries WHERE transactionSyncId = :transactionSyncId")
    suspend fun deleteEntriesForTransaction(transactionSyncId: String)

    @Query("SELECT * FROM debt_entries WHERE id = :id")
    suspend fun getEntry(id: Int): DebtEntry?

    @Update
    suspend fun updateEntry(entry: DebtEntry)

    @Query("SELECT * FROM debt_people WHERE id = :id")
    suspend fun getPerson(id: Int): DebtPerson?

    @Query("SELECT * FROM debt_people WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getPersonByName(name: String): DebtPerson?

    @Insert
    suspend fun insertPerson(person: DebtPerson): Long

    @Update
    suspend fun updatePerson(person: DebtPerson)

    @Delete
    suspend fun deletePerson(person: DebtPerson)

    @Insert
    suspend fun insertEntry(entry: DebtEntry): Long

    @Query("DELETE FROM debt_entries WHERE id = :id")
    suspend fun deleteEntry(id: Int)
}
