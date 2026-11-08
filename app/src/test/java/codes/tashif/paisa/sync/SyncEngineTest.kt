package codes.tashif.paisa.sync

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.Repository
import codes.tashif.paisa.data.Transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Two in-memory databases stand in for two phones swapping change files. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncEngineTest {

    private class Device(val name: String, val db: AppDatabase) {
        val repo = Repository(db)
        val sql: SupportSQLiteDatabase get() = db.openHelper.writableDatabase
        var seq = 0L

        /** Exports pending changes (or everything) and clears them, like a successful upload. */
        fun push(full: Boolean = false): ByteArray {
            seq++
            var export: SyncEngine.Export? = null
            db.runInTransaction {
                export = SyncEngine.export(
                    sql,
                    SyncEngine.Header(name, name, seq, full, System.currentTimeMillis())
                )
                SyncEngine.clearOutbox(sql, export!!.outbox)
            }
            return export!!.payload
        }

        fun pull(file: ByteArray) = SyncEngine.apply(sql, file)

        fun count(sql: String): Int = this.sql.query(sql).use { c -> c.moveToFirst(); c.getInt(0) }

        fun balance(accountId: Int): Double = runBlocking { repo.getAccountById(accountId)!!.currentBalance }

        fun categoryId(name: String, type: String = "expense"): Int =
            sql.query("SELECT id FROM categories WHERE name = ? AND type = ?", arrayOf<Any>(name, type))
                .use { c -> c.moveToFirst(); c.getInt(0) }
    }

    private lateinit var a: Device
    private lateinit var b: Device

    private fun newDevice(name: String): Device {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.callback)
            .allowMainThreadQueries()
            .build()
        db.openHelper.writableDatabase // runs onCreate + onOpen
        return Device(name, db)
    }

    @Before
    fun setUp() {
        a = newDevice("phone")
        b = newDevice("tablet")
    }

    @After
    fun tearDown() {
        a.db.close()
        b.db.close()
    }

    private fun tx(
        device: Device,
        amount: Double,
        type: String = "expense",
        accountId: Int = 1,
        date: String = "2026-11-08T10:00:00",
        note: String = "",
        hash: String? = null
    ): Long = runBlocking {
        val t = Transaction(
            amount = amount,
            type = type,
            categoryId = device.categoryId(if (type == "income") "Salary" else "Food & Dining", type),
            accountId = accountId,
            note = note,
            transactionDate = date,
            createdAt = date,
            updatedAt = date,
            transactionHash = hash,
            source = if (hash != null) "sms" else "manual"
        )
        if (hash != null) device.repo.insertSmsTransaction(t, null) else device.repo.insertTransaction(t)
    }

    @Test
    fun seededDefaultsDoNotDuplicateOnASecondDevice() {
        val categories = a.count("SELECT COUNT(*) FROM categories")
        b.pull(a.push(full = true))
        assertEquals(categories, b.count("SELECT COUNT(*) FROM categories"))
        assertEquals(2, b.count("SELECT COUNT(*) FROM accounts"))
    }

    @Test
    fun newDevicePullsEverythingAndBalancesMatch() {
        runBlocking { a.repo.updateCurrentBalance(1, 500.0) }
        tx(a, 120.0, date = "2099-01-01T09:00:00")
        tx(a, 1000.0, type = "income", date = "2099-01-02T09:00:00")
        assertEquals(1380.0, a.balance(1), 0.001)

        b.pull(a.push(full = true))

        assertEquals(2, b.count("SELECT COUNT(*) FROM transactions"))
        assertEquals(1380.0, b.balance(1), 0.001)
    }

    @Test
    fun concurrentTransactionsOnBothDevicesAddUp() {
        b.pull(a.push(full = true))

        tx(a, 100.0, note = "chai")
        tx(b, 40.0, note = "auto")
        val fromA = a.push()
        val fromB = b.push()
        a.pull(fromB)
        b.pull(fromA)

        assertEquals(2, a.count("SELECT COUNT(*) FROM transactions"))
        assertEquals(2, b.count("SELECT COUNT(*) FROM transactions"))
        assertEquals(-140.0, a.balance(1), 0.001)
        assertEquals(-140.0, b.balance(1), 0.001)
    }

    @Test
    fun appliedChangesAreNotSentBack() {
        tx(a, 10.0)
        b.pull(a.push(full = true))
        assertFalse(SyncEngine.hasPendingChanges(b.sql))
    }

    @Test
    fun deletesPropagate() {
        val id = tx(a, 75.0)
        b.pull(a.push(full = true))
        assertEquals(-75.0, b.balance(1), 0.001)

        runBlocking { a.repo.deleteTransaction(id.toInt()) }
        b.pull(a.push())

        assertEquals(0, b.count("SELECT COUNT(*) FROM transactions"))
        assertEquals(0.0, b.balance(1), 0.001)
    }

    @Test
    fun laterEditWinsOnBothDevices() {
        val id = tx(a, 50.0, note = "original")
        b.pull(a.push(full = true))
        val bId = b.count("SELECT id FROM transactions")

        runBlocking {
            val ta = a.repo.getRawTransactionById(id.toInt())!!
            a.repo.updateTransaction(ta.copy(note = "from phone"))
            Thread.sleep(5)
            val tb = b.repo.getRawTransactionById(bId)!!
            b.repo.updateTransaction(tb.copy(note = "from tablet", amount = 60.0))
        }
        val fromA = a.push()
        val fromB = b.push()
        a.pull(fromB)
        b.pull(fromA)

        val noteA = runBlocking { a.repo.getRawTransactionById(id.toInt())!!.note }
        val noteB = runBlocking { b.repo.getRawTransactionById(bId)!!.note }
        assertEquals("from tablet", noteA)
        assertEquals("from tablet", noteB)
        assertEquals(-60.0, a.balance(1), 0.001)
        assertEquals(-60.0, b.balance(1), 0.001)
    }

    @Test
    fun sameSmsOnTwoPhonesIsOneTransaction() {
        b.pull(a.push(full = true))
        tx(a, 250.0, hash = "sms-abc")
        tx(b, 250.0, hash = "sms-abc")
        val fromA = a.push()
        val fromB = b.push()
        a.pull(fromB)
        b.pull(fromA)
        assertEquals(1, a.count("SELECT COUNT(*) FROM transactions"))
        assertEquals(1, b.count("SELECT COUNT(*) FROM transactions"))
    }

    @Test
    fun applyingTheSameFileTwiceChangesNothing() {
        tx(a, 30.0)
        val file = a.push(full = true)
        b.pull(file)
        val second = b.pull(file)
        assertEquals(0, second.applied)
        assertEquals(1, b.count("SELECT COUNT(*) FROM transactions"))
    }

    @Test
    fun deletedAccountWithNewLocalTransactionsComesBack() {
        val accountId = runBlocking {
            a.repo.insertAccount(
                Account(name = "Wallet", type = "Wallet", openingBalance = 0.0, currentBalance = 0.0, icon = "w", color = "#fff")
            ).toInt()
        }
        b.pull(a.push(full = true))
        val bAccount = b.count("SELECT id FROM accounts WHERE name = 'Wallet'")

        // Phone deletes the account while the tablet logs a purchase on it.
        runBlocking { a.repo.deleteAccountCascade(a.repo.getAccountById(accountId)!!) }
        tx(b, 20.0, accountId = bAccount)

        val fromA = a.push()
        b.pull(fromA)
        // The tablet keeps it and sends it back out.
        assertNotNull(runBlocking { b.repo.getAccountById(bAccount) })
        a.pull(b.push())
        assertEquals(1, a.count("SELECT COUNT(*) FROM accounts WHERE name = 'Wallet'"))
        assertEquals(1, a.count("SELECT COUNT(*) FROM transactions"))
    }

    @Test
    fun recurringRuleRunOnBothPhonesMakesOneTransaction() {
        runBlocking {
            a.repo.insertRecurringTransaction(
                codes.tashif.paisa.data.RecurringTransaction(
                    amount = 999.0,
                    type = "expense",
                    categoryId = a.categoryId("Subscriptions"),
                    accountId = 1,
                    note = "Rent",
                    frequency = "monthly",
                    nextExecutionDate = "2026-01-01"
                )
            )
        }
        b.pull(a.push(full = true))
        runBlocking {
            a.repo.processRecurringTransactions()
            b.repo.processRecurringTransactions()
        }
        val generated = a.count("SELECT COUNT(*) FROM transactions")
        assertTrue(generated > 0)
        val fromA = a.push()
        val fromB = b.push()
        a.pull(fromB)
        b.pull(fromA)
        assertEquals(generated, a.count("SELECT COUNT(*) FROM transactions"))
        assertEquals(generated, b.count("SELECT COUNT(*) FROM transactions"))
        assertEquals(a.balance(1), b.balance(1), 0.001)
    }

    @Test
    fun currencyFollowsButOtherSettingsStayPerDevice() {
        b.pull(a.push(full = true))
        runBlocking {
            val sa = a.repo.getSettingsDirect()!!
            a.repo.updateSettings(sa.copy(currency = "$", themeMode = "dark"))
        }
        b.pull(a.push())
        val sb = runBlocking { b.repo.getSettingsDirect()!! }
        assertEquals("$", sb.currency)
        assertEquals("system", sb.themeMode)

        // Device-only settings don't queue an upload.
        runBlocking { b.repo.updateSettings(sb.copy(themeMode = "light", lastSmsScanAt = 123L)) }
        assertFalse(SyncEngine.hasPendingChanges(b.sql))

        // And currency changes flow back the other way.
        runBlocking { b.repo.updateSettings(b.repo.getSettingsDirect()!!.copy(currency = "€")) }
        a.pull(b.push())
        assertEquals("€", runBlocking { a.repo.getSettingsDirect()!!.currency })
        assertEquals("dark", runBlocking { a.repo.getSettingsDirect()!!.themeMode })
    }

    @Test
    fun freshDeviceDefaultCurrencyDoesNotOverwriteAChosenOne() {
        runBlocking { a.repo.updateSettings(a.repo.getSettingsDirect()!!.copy(currency = "$")) }
        val fromA = a.push(full = true)
        val fromB = b.push(full = true) // fresh install still on the default ₹
        a.pull(fromB)
        b.pull(fromA)
        assertEquals("$", runBlocking { a.repo.getSettingsDirect()!!.currency })
        assertEquals("$", runBlocking { b.repo.getSettingsDirect()!!.currency })
    }

    @Test
    fun staleObjectUpdateKeepsSyncId() {
        val id = tx(a, 5.0)
        val before = runBlocking { a.repo.getRawTransactionById(id.toInt())!! }
        assertNotNull(before.syncId)
        runBlocking { a.repo.updateTransaction(before.copy(syncId = null, note = "x")) }
        val after = runBlocking { a.repo.getRawTransactionById(id.toInt())!! }
        assertEquals(before.syncId, after.syncId)
        assertTrue(after.syncStamp > before.syncStamp)
    }

    @Test
    fun reportedBalanceAnchorsAndOlderSmsCannotRollItBack() {
        runBlocking {
            a.repo.applyReportedBalance(1, 1000.0, "2026-11-05T10:00:00")
            a.repo.applyReportedBalance(1, 10.0, "2026-10-01T10:00:00")
        }
        tx(a, 100.0, date = "2026-11-06T10:00:00")
        tx(a, 999.0, date = "2026-11-01T10:00:00") // before the anchor, already counted
        assertEquals(900.0, a.balance(1), 0.001)
        val accounts = runBlocking { a.repo.allAccounts.first() }
        assertNull(accounts.firstOrNull { it.syncId == null })
    }
}
