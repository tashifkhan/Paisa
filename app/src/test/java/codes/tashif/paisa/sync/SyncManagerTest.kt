package codes.tashif.paisa.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import codes.tashif.paisa.backup.BackupPrefs
import codes.tashif.paisa.backup.BackupStore
import codes.tashif.paisa.backup.RemoteBackup
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.Repository
import codes.tashif.paisa.data.Transaction
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
        val db: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.callback)
            .allowMainThreadQueries()
            .build()
            .also { dbs += it; it.openHelper.writableDatabase }
        val prefs = BackupPrefs(context, "sync-test-$name")
        val sync = SyncManager(context, db, prefs, cloud)
        val repo = Repository(db)

        fun add(amount: Double, note: String) = runBlocking {
            val cat = db.openHelper.writableDatabase
                .query("SELECT id FROM categories WHERE type = 'expense' LIMIT 1")
                .use { it.moveToFirst(); it.getInt(0) }
            repo.insertTransaction(
                Transaction(
                    amount = amount, type = "expense", categoryId = cat, accountId = 1, note = note,
                    transactionDate = "2026-11-08T10:00:00", createdAt = "c", updatedAt = "u"
                )
            )
        }

        fun notes(): Set<String> = db.openHelper.writableDatabase
            .query("SELECT note FROM transactions").use { c ->
                buildSet { while (c.moveToNext()) add(c.getString(0)) }
            }
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
        assertEquals(phone.repo.getAccountById(1)!!.currentBalance, tablet.repo.getAccountById(1)!!.currentBalance, 0.001)

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
        val invalidated = java.util.concurrent.CountDownLatch(1)
        tablet.db.invalidationTracker.addObserver(object :
            androidx.room.InvalidationTracker.Observer(arrayOf("transactions")) {
            override fun onInvalidated(tables: Set<String>) = invalidated.countDown()
        })
        phone.add(12.0, "seen live")
        phone.sync.syncNow()
        tablet.sync.syncNow()
        assertTrue(
            "the UI would not see synced rows",
            invalidated.await(5, java.util.concurrent.TimeUnit.SECONDS)
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
