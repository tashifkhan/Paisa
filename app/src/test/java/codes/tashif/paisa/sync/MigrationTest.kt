package codes.tashif.paisa.sync

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import codes.tashif.paisa.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Builds a real version 9 database file (the shipped 1.1.0 schema, made by
 * stripping what 10 adds), fills it, and opens it through the migration.
 * Room validates the result against the entities, so a schema mismatch fails here
 * instead of crashing every upgrading user.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    @Test
    fun upgradeFrom9KeepsDataAndBalances() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)

        // 1. Current schema, then strip it back to version 9.
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            .allowMainThreadQueries()
            .build()
            .also { it.openHelper.writableDatabase; it.close() }
        val raw = SQLiteDatabase.openDatabase(
            context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE
        )
        // No DROP COLUMN before SQLite 3.35, so recreate each (still empty) table
        // from its own CREATE statement minus the version 10 columns.
        val added = Regex(""",\s*`(syncId|syncStamp|balanceAnchor|balanceAnchorAt|floatingNavBar|navBarLabels)`[^,)]*""")
        for (table in SyncSchema.TABLE_NAMES) {
            val create = raw.rawQuery("SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(table))
                .use { it.moveToFirst(); it.getString(0) }
            val indexes = raw.rawQuery(
                "SELECT sql FROM sqlite_master WHERE type = 'index' AND tbl_name = ? AND sql IS NOT NULL AND name NOT LIKE '%syncId'",
                arrayOf(table)
            ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }
            raw.execSQL("DROP TABLE `$table`")
            raw.execSQL(create.replace(added, ""))
            indexes.forEach { raw.execSQL(it) }
        }
        raw.execSQL("DELETE FROM room_master_table")
        raw.version = 9

        // 2. A 1.1.0 user's data. Account 3's balance came from an SMS, then a
        // manual entry; account 4 was set by hand and matches neither formula.
        raw.execSQL("INSERT INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (1, 'Food & Dining', 'expense', 'r', '#000', 0, 1)")
        raw.execSQL("INSERT INTO settings (id, themeMode, currency, pinEnabled, biometricEnabled, notificationsEnabled, colorPalette, lastSmsScanAt, smsScanEnabled) VALUES (1, 'dark', '$', 0, 0, 1, 'dynamic', 0, 1)")
        raw.execSQL("INSERT INTO accounts (id, name, type, openingBalance, currentBalance, icon, color, orderIndex, isDefault) VALUES (1, 'Cash', 'Cash', 100, 70, 'w', '#fff', 0, 0)")
        raw.execSQL("INSERT INTO accounts (id, name, type, openingBalance, currentBalance, icon, color, bankName, accountLast4, orderIndex, isDefault) VALUES (3, 'HDFC ····1234', 'Bank Account', 0, 4950, 'b', '#fff', 'HDFC Bank', '1234', 1, 0)")
        raw.execSQL("INSERT INTO accounts (id, name, type, openingBalance, currentBalance, icon, color, orderIndex, isDefault) VALUES (4, 'Wallet', 'Wallet', 0, 1234.5, 'w', '#fff', 2, 0)")
        fun tx(id: Int, account: Int, amount: Double, date: String, balanceAfter: Double? = null, hash: String? = null) =
            raw.execSQL(
                "INSERT INTO transactions (id, amount, type, categoryId, accountId, note, transactionDate, createdAt, updatedAt, tags, source, currency, isDeleted, balanceAfter, transactionHash) " +
                    "VALUES ($id, $amount, 'expense', 1, $account, '', '$date', 'c', 'u', '', 'manual', 'INR', 0, ${balanceAfter ?: "NULL"}, ${hash?.let { "'$it'" } ?: "NULL"})"
            )
        tx(1, 1, 30.0, "2026-10-01T10:00:00")
        tx(2, 3, 200.0, "2026-10-02T10:00:00", balanceAfter = 5000.0, hash = "h1")
        tx(3, 3, 50.0, "2026-10-03T10:00:00")
        tx(4, 4, 99.0, "2026-10-04T10:00:00")
        raw.close()

        // 3. Upgrade.
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .addCallback(AppDatabase.callback)
            .allowMainThreadQueries()
            .build()
        val sql = db.openHelper.writableDatabase

        fun balance(id: Int) = sql.query("SELECT currentBalance FROM accounts WHERE id = $id")
            .use { it.moveToFirst(); it.getDouble(0) }
        assertEquals(70.0, balance(1), 0.001)
        assertEquals(4950.0, balance(3), 0.001)
        assertEquals(1234.5, balance(4), 0.001)

        fun syncId(query: String) = sql.query(query).use { it.moveToFirst(); it.getString(0) }
        assertEquals("cat:expense:food & dining", syncId("SELECT syncId FROM categories WHERE id = 1"))
        assertEquals("acct:seed:cash", syncId("SELECT syncId FROM accounts WHERE id = 1"))
        assertEquals("acct:hdfc bank:1234", syncId("SELECT syncId FROM accounts WHERE id = 3"))
        assertEquals("sms:h1", syncId("SELECT syncId FROM transactions WHERE id = 2"))
        assertEquals("settings", syncId("SELECT syncId FROM settings WHERE id = 1"))
        assertEquals("$", syncId("SELECT currency FROM settings WHERE id = 1"))
        // 10 to 11: navbar settings default to today's docked, labeled bar.
        assertEquals("0", syncId("SELECT floatingNavBar FROM settings WHERE id = 1"))
        assertEquals("1", syncId("SELECT navBarLabels FROM settings WHERE id = 1"))
        assertEquals(0, sql.query("SELECT COUNT(*) FROM transactions WHERE syncId IS NULL").use { it.moveToFirst(); it.getInt(0) })

        // Existing data isn't queued as fresh edits; the first sync sends a full copy anyway.
        assertFalse(SyncEngine.hasPendingChanges(sql))

        // Triggers are live after the upgrade.
        sql.execSQL("UPDATE accounts SET name = 'Pocket' WHERE id = 4")
        assertEquals(true, SyncEngine.hasPendingChanges(sql))
        db.close()
    }
}
