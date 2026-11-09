package codes.tashif.paisa.sync

import android.content.Context
import codes.tashif.paisa.backup.BackupConfig
import codes.tashif.paisa.backup.BackupStore
import codes.tashif.paisa.backup.CloudPlatform
import codes.tashif.paisa.backup.testBackupPrefs
import codes.tashif.paisa.testing.inMemoryDatabase
import codes.tashif.paisa.testing.long
import codes.tashif.paisa.testing.sql
import androidx.test.core.app.ApplicationProvider
import codes.tashif.paisa.backup.BackupPrefs
import codes.tashif.paisa.backup.RemoteBackup
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.Transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import codes.tashif.paisa.data.repository.AccountRepository
import codes.tashif.paisa.data.repository.DebtRepository
import codes.tashif.paisa.data.repository.DebtShare
import codes.tashif.paisa.data.repository.SettingsRepository
import codes.tashif.paisa.data.repository.TransactionRepository

/** Full pull/push cycle between devices sharing one fake cloud folder. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncManagerTest {

    private class FakeCloud : BackupStore {
        val files = linkedMapOf<String, ByteArray>()
        override suspend fun list(): List<RemoteBackup> = emptyList()
        override suspend fun upload(name: String, bytes: ByteArray) = uploadSync(name, bytes)
        override suspend fun download(backup: RemoteBackup) = downloadSync(backup)
        override suspend fun delete(backup: RemoteBackup) = deleteSync(backup)
        override suspend fun listSync() = files.map { (n, b) -> RemoteBackup(n, n, b.size.toLong(), 0L) }
        override suspend fun uploadSync(name: String, bytes: ByteArray): RemoteBackup {
            files[name] = bytes
            return RemoteBackup(name, name, bytes.size.toLong(), 0L)
        }
        override suspend fun downloadSync(file: RemoteBackup) =
            files[file.name] ?: throw IOException("missing ${file.name}")
        override suspend fun deleteSync(file: RemoteBackup) {
            files.remove(file.name)
        }
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val cloud = FakeCloud()
    private val dbs = mutableListOf<AppDatabase>()

    private inner class Device(name: String) {
        val db: AppDatabase = inMemoryDatabase().also { dbs += it }
        val prefs = testBackupPrefs(context, "sync-test-$name")
        val sync = SyncManager(db, prefs, TestPlatform(name), cloud)
        val accountRepo = AccountRepository(db)
        val debtRepo = DebtRepository(db)
        val transactionRepo = TransactionRepository(db, accountRepo, debtRepo)
        val settingsRepo = SettingsRepository(db)

        fun add(amount: Double, note: String) = runBlocking {
            val cat = db.long("SELECT id FROM categories WHERE type = 'expense' LIMIT 1").toInt()
            transactionRepo.insertTransaction(
                Transaction(
                    amount = amount, type = "expense", categoryId = cat, accountId = 1, note = note,
                    transactionDate = "2026-11-08T10:00:00", createdAt = "c", updatedAt = "u"
                )
            )
        }

        fun notes(): Set<String> = db.sql { sql -> sql.rows("SELECT note FROM transactions").map { it.getString(0) }.toSet() }
    }

    @After
    fun tearDown() {
        dbs.forEach { it.close() }
    }

    @Test
    fun devicesExchangeChangesThroughTheCloud() = runBlocking {
        val phone = Device("phone")
        val tablet = Device("tablet")
        phone.add(10.0, "before sync")
        phone.sync.enable("correct horse".toCharArray())
        phone.sync.syncNow()

        tablet.sync.enable("correct horse".toCharArray())
        tablet.sync.syncNow()
        assertEquals(setOf("before sync"), tablet.notes())

        tablet.add(5.0, "from tablet")
        tablet.sync.syncNow()
        phone.sync.syncNow()
        assertEquals(setOf("before sync", "from tablet"), phone.notes())
        assertEquals(phone.accountRepo.getAccountById(1)!!.currentBalance, tablet.accountRepo.getAccountById(1)!!.currentBalance, 0.001)

        // Nothing new: a second sync uploads nothing.
        val before = cloud.files.size
        val report = phone.sync.syncNow()
        assertEquals(0, report.sent)
        assertEquals(before, cloud.files.size)
    }

    @Test
    fun pulledChangesRefreshOpenQueries() = runBlocking {
        val phone = Device("phone4")
        val tablet = Device("tablet4")
        // Like the app: turning sync on syncs right away, so the first device's
        // file (and salt) exists before the second joins.
        phone.sync.enable("correct horse".toCharArray())
        phone.sync.syncNow()
        tablet.sync.enable("correct horse".toCharArray())
        tablet.sync.syncNow()

        // Room re-runs Flows (the transaction list, balances) only when its
        // invalidation tracker hears about the write.
        val invalidated = kotlinx.coroutines.CompletableDeferred<Unit>()
        val watcher = launch(kotlinx.coroutines.Dispatchers.IO) {
            tablet.db.invalidationTracker.createFlow("transactions", emitInitialState = false)
                .collect { invalidated.complete(Unit) }
        }
        // Let the flow register its observer before the write.
        kotlinx.coroutines.delay(200)
        phone.add(12.0, "seen live")
        phone.sync.syncNow()
        tablet.sync.syncNow()
        val seen = kotlinx.coroutines.withTimeoutOrNull(5_000) { invalidated.await() } != null
        watcher.cancel()
        assertTrue("the UI would not see synced rows", seen)
    }

    @Test
    fun debtsSyncBetweenDevices() = runBlocking {
        val phone = Device("phone5")
        val tablet = Device("tablet5")
        phone.sync.enable("correct horse".toCharArray())
        phone.sync.syncNow()
        tablet.sync.enable("correct horse".toCharArray())
        tablet.sync.syncNow()

        val radhika = phone.debtRepo.addDebtPerson("Radhika", 5000.0)
        phone.debtRepo.addDebtEntry(radhika, -1000.0, "Paid back")
        phone.sync.syncNow()
        tablet.sync.syncNow()
        val onTablet = tablet.debtRepo.debtPeople.first()
        assertEquals(listOf("Radhika" to 4000.0), onTablet.map { it.person.name to it.balance })

        // Settling on the tablet reaches the phone.
        tablet.debtRepo.settleDebt(onTablet.single().person.id)
        tablet.sync.syncNow()
        phone.sync.syncNow()
        assertEquals(0.0, phone.debtRepo.debtPeople.first().single().balance, 0.001)
    }

    @Test
    fun aSplitSavedOnTwoOfflineDevicesEndsUpAsOneSet() = runBlocking {
        val phone = Device("phone6")
        val tablet = Device("tablet6")
        phone.sync.enable("correct horse".toCharArray())
        phone.sync.syncNow()
        tablet.sync.enable("correct horse".toCharArray())
        tablet.sync.syncNow()

        val dinnerOnPhone = phone.add(1200.0, "Dinner").toInt()
        val txSync = phone.db.transactionDao().getRawTransactionById(dinnerOnPhone)!!.syncId!!
        phone.sync.syncNow()
        tablet.sync.syncNow()
        val dinnerOnTablet = tablet.db.transactionDao().getIdBySyncId(txSync)!!

        // Both devices are offline and each saves its own split, adding the
        // same people by name.
        phone.debtRepo.saveTransactionSplit(
            dinnerOnPhone,
            listOf(DebtShare(null, "Radhika", 400.0), DebtShare(null, "Omar", 400.0))
        )
        // The tablet saves later. Stamps are milliseconds, so make "later" real;
        // two saves in the same millisecond fall to the tie-break instead.
        kotlinx.coroutines.delay(5)
        tablet.debtRepo.saveTransactionSplit(
            dinnerOnTablet,
            listOf(DebtShare(null, "Radhika", 500.0), DebtShare(null, "Omar", 300.0))
        )

        phone.sync.syncNow()
        tablet.sync.syncNow()
        phone.sync.syncNow()

        fun split(device: Device) = runBlocking {
            device.db.debtDao().getEntriesForTransaction(txSync)
                .map { device.db.debtDao().getPerson(it.personId)!!.name to it.amount }
                .sortedBy { it.first }
        }
        val expected = listOf("Omar" to 300.0, "Radhika" to 500.0)
        assertEquals(expected, split(tablet))
        assertEquals(expected, split(phone))
        assertEquals(
            mapOf("Omar" to 300.0, "Radhika" to 500.0),
            phone.debtRepo.debtPeople.first().associate { it.person.name to it.balance }
        )
    }

    @Test
    fun wrongPasswordIsRejectedWhenJoining() = runBlocking {
        val phone = Device("phone2")
        phone.sync.enable("correct horse".toCharArray())
        phone.sync.syncNow()
        val tablet = Device("tablet2")
        try {
            tablet.sync.enable("battery staple".toCharArray())
            fail("expected the wrong password to be rejected")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("password"))
        }
    }

    @Test
    fun compactionKeepsLateJoinersWhole() = runBlocking {
        val phone = Device("phone3")
        phone.sync.enable("correct horse".toCharArray())
        phone.sync.syncNow()
        repeat(45) { i ->
            phone.add(1.0, "tx $i")
            phone.sync.syncNow()
        }
        // History folded into a full copy; old per-change files are gone.
        assertTrue("files left: ${cloud.files.size}", cloud.files.size < 10)

        val late = Device("late3")
        late.sync.enable("correct horse".toCharArray())
        late.sync.syncNow()
        assertEquals(45, late.notes().size)
    }
}

/** No scheduling or widgets in tests; the store comes from the test itself. */
private class TestPlatform(override val deviceName: String) : CloudPlatform {
    override val appVersion = "test"
    override fun storeFor(config: BackupConfig): BackupStore? = null
    override fun reschedule() = Unit
    override suspend fun onDataReplaced() = Unit
}
