package codes.tashif.paisa.web

import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

/** One row per page load, to show the database outlives a reload. */
@Entity(tableName = "launches")
data class Launch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val openedAt: Long
)

/** A backup or sync file this browser decrypted. */
@Entity(tableName = "opened_files")
data class OpenedFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** "sync" or "backup". */
    val kind: String,
    val deviceName: String?,
    val rows: Int,
    val openedAt: Long
)

@Dao
interface SpikeDao {
    @Insert
    suspend fun insertLaunch(launch: Launch)

    @Query("SELECT COUNT(*) FROM launches")
    suspend fun launchCount(): Int

    @Insert
    suspend fun insertFile(file: OpenedFile)

    @Query("SELECT * FROM opened_files ORDER BY openedAt DESC LIMIT 20")
    suspend fun recentFiles(): List<OpenedFile>
}

/**
 * The spike's database. It only proves Room 3 runs in the browser and keeps
 * its data. The real schema arrives when the Android entities move to
 * :shared.
 */
@Database(entities = [Launch::class, OpenedFile::class], version = 1)
@ConstructedBy(WebDatabaseConstructor::class)
abstract class WebDatabase : RoomDatabase() {
    abstract fun dao(): SpikeDao
}

// Room's KSP processor writes the actual.
@Suppress("KotlinNoActualForExpect")
expect object WebDatabaseConstructor : RoomDatabaseConstructor<WebDatabase> {
    override fun initialize(): WebDatabase
}
