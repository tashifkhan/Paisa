package codes.tashif.paisa.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
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
    /** syncId of the transaction this came from, if any. A soft link: deleting the transaction keeps the debt. */
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

@Dao
interface DebtDao {
    @Query(
        """
        SELECT p.*, COALESCE(SUM(e.amount), 0) AS balance
        FROM debt_people p LEFT JOIN debt_entries e ON e.personId = p.id
        GROUP BY p.id
        ORDER BY ABS(COALESCE(SUM(e.amount), 0)) DESC, p.name COLLATE NOCASE
        """
    )
    fun getPeopleWithBalances(): Flow<List<DebtPersonBalance>>

    @Query("SELECT * FROM debt_entries WHERE personId = :personId ORDER BY entryDate DESC, id DESC")
    fun getEntries(personId: Int): Flow<List<DebtEntry>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM debt_entries WHERE personId = :personId")
    suspend fun getBalance(personId: Int): Double

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
