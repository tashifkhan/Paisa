package codes.tashif.paisa.backup

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import codes.tashif.paisa.data.AppDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupArchiveTest {

    private lateinit var db: AppDatabase
    private val sql: SupportSQLiteDatabase get() = db.openHelper.writableDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sql.execSQL(
            "INSERT INTO accounts (id, name, type, openingBalance, currentBalance, icon, color, bankName, accountLast4, creditLimit, orderIndex, isDefault) " +
                "VALUES (5, 'HDFC', 'Bank Account', 100.5, 1234.75, 'bank', '#fff', 'HDFC Bank', '1234', NULL, 2, 1)"
        )
        sql.execSQL("INSERT INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (7, 'Food & Dining', 'expense', 'r', '#000', 0, 1)")
        sql.execSQL(
            "INSERT INTO transactions (id, amount, type, categoryId, accountId, note, transactionDate, createdAt, updatedAt, tags, smsBody, transactionHash, source, currency, isDeleted) " +
                "VALUES (42, 99.99, 'expense', 7, 5, 'chai ☕ \"quoted\"', '2026-11-01', 'c', 'u', '', 'Rs 99.99 debited', 'h1', 'sms', 'INR', 0)"
        )
        // Soft-deleted rows must survive, or the next SMS rescan resurrects them.
        sql.execSQL(
            "INSERT INTO transactions (id, amount, type, categoryId, accountId, note, transactionDate, createdAt, updatedAt, tags, transactionHash, source, currency, isDeleted) " +
                "VALUES (43, 10.0, 'expense', 7, 5, '', '2026-11-02', 'c', 'u', '', 'h2', 'sms', 'INR', 1)"
        )
        sql.execSQL("INSERT INTO settings (id, themeMode, currency, pinEnabled, biometricEnabled, notificationsEnabled, colorPalette, lastSmsScanAt, smsScanEnabled) VALUES (1, 'dark', '€', 0, 0, 1, 'dynamic', 999, 1)")
        sql.execSQL("INSERT INTO merchant_mappings (merchantName, categoryName, categoryType, createdAt, updatedAt) VALUES ('SWIGGY', 'Food & Dining', 'expense', 'c', 'u')")
    }

    @After
    fun tearDown() = db.close()

    private fun count(table: String): Int =
        sql.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }

    private fun export(): ByteArray {
        var bytes: ByteArray? = null
        db.runInTransaction { bytes = BackupArchive.export(sql) }
        return bytes!!
    }

    @Test
    fun encryptedRoundTripReplacesEverything() {
        val key = BackupCrypto.deriveKey("pw123456".toCharArray(), BackupCrypto.newSalt(), 1_000)
        val file = BackupCrypto.seal(export(), key)

        // Change the live data after the backup.
        sql.execSQL("DELETE FROM transactions WHERE id = 42")
        sql.execSQL("INSERT INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (8, 'New', 'expense', 'x', '#1', 0, 2)")
        sql.execSQL("UPDATE settings SET themeMode = 'light'")

        val header = BackupCrypto.readHeader(file)
        val snapshot = BackupArchive.parse(BackupCrypto.open(file, header, key))
        assertEquals(AppDatabase.SCHEMA_VERSION, snapshot.dbVersion)
        assertEquals(2, snapshot.transactionCount)
        db.runInTransaction { BackupArchive.restore(sql, snapshot) }

        assertEquals(1, count("accounts"))
        assertEquals(1, count("categories"))
        assertEquals(2, count("transactions"))
        assertEquals(1, count("merchant_mappings"))
        sql.query("SELECT amount, note, smsBody, isDeleted FROM transactions WHERE id = 42").use {
            assertTrue(it.moveToFirst())
            assertEquals(99.99, it.getDouble(0), 0.0)
            assertEquals("chai ☕ \"quoted\"", it.getString(1))
            assertEquals("Rs 99.99 debited", it.getString(2))
            assertEquals(0, it.getInt(3))
        }
        sql.query("SELECT isDeleted FROM transactions WHERE id = 43").use {
            assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0))
        }
        sql.query("SELECT currentBalance, creditLimit, isDefault FROM accounts WHERE id = 5").use {
            assertTrue(it.moveToFirst())
            assertEquals(1234.75, it.getDouble(0), 0.0)
            assertTrue(it.isNull(1))
            assertEquals(1, it.getInt(2))
        }
        sql.query("SELECT themeMode, currency FROM settings").use {
            assertTrue(it.moveToFirst())
            assertEquals("dark", it.getString(0))
            assertEquals("€", it.getString(1))
        }
    }

    @Test
    fun brokenBackupLeavesLiveDataUntouched() {
        val snapshot = BackupArchive.parse(export())
        // Point a transaction at an account that isn't in the backup.
        val broken = snapshot.copy(
            tables = snapshot.tables + ("accounts" to emptyList())
        )
        try {
            db.runInTransaction { BackupArchive.restore(sql, broken) }
            fail("restore should have thrown")
        } catch (e: Exception) {
            // expected
        }
        assertEquals(1, count("accounts"))
        assertEquals(2, count("transactions"))
    }

    @Test(expected = BackupCrypto.NotABackupException::class)
    fun rejectsGarbagePayload() {
        val gz = java.io.ByteArrayOutputStream()
        java.util.zip.GZIPOutputStream(gz).use { it.write("{\"tables\": [1,2".toByteArray()) }
        BackupArchive.parse(gz.toByteArray())
    }
}
