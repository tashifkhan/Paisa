package codes.tashif.paisa.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import codes.tashif.paisa.backup.BackupConfig
import codes.tashif.paisa.backup.BackupManager
import codes.tashif.paisa.backup.BackupNames
import codes.tashif.paisa.backup.BackupStore
import codes.tashif.paisa.backup.CloudPlatform
import codes.tashif.paisa.backup.NextcloudAccount
import codes.tashif.paisa.backup.NextcloudStore
import codes.tashif.paisa.backup.testBackupPrefs
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.Transaction
import codes.tashif.paisa.data.repository.AccountRepository
import codes.tashif.paisa.data.repository.DebtRepository
import codes.tashif.paisa.data.repository.TransactionRepository
import codes.tashif.paisa.testing.inMemoryDatabase
import codes.tashif.paisa.testing.long
import codes.tashif.paisa.testing.sql
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Backup and sync against a real Nextcloud server, through the same
 * NextcloudStore the app uses. Skipped unless PAISA_NEXTCLOUD_URL is set, for
 * example with the server from `docker run -p 8090:80 nextcloud`:
 *
 *     PAISA_NEXTCLOUD_URL=http://127.0.0.1:8090 PAISA_NEXTCLOUD_USER=paisa \
 *     PAISA_NEXTCLOUD_PASSWORD=… ./gradlew :app:testDebugUnitTest --tests '*NextcloudLiveSyncTest*'
 *
 * It deletes everything in the account's /Paisa folder first.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NextcloudLiveSyncTest {
    private val server = System.getenv("PAISA_NEXTCLOUD_URL")
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbs = mutableListOf<AppDatabase>()
    private lateinit var account: NextcloudAccount

    private class Platform(override val deviceName: String, private val store: BackupStore) : CloudPlatform {
        override val appVersion = "test"
        override fun storeFor(config: BackupConfig): BackupStore = store
        override fun reschedule() = Unit
        override suspend fun onDataReplaced() = Unit
    }

    private inner class Device(name: String) {
        val db = inMemoryDatabase().also { dbs += it }
        val prefs = testBackupPrefs(context, "nc-live-$name-${System.nanoTime()}").also { it.setNextcloud(account) }
        val store = NextcloudStore(account)
        val sync = SyncManager(db, prefs, Platform(name, store))
        val backups = BackupManager(prefs, db, Platform(name, store))
        private val accounts = AccountRepository(db)
        private val transactions = TransactionRepository(db, accounts, DebtRepository(db))

        fun add(amount: Double, note: String) = runBlocking {
            val cat = db.long("SELECT id FROM categories WHERE type = 'expense' LIMIT 1").toInt()
            transactions.insertTransaction(
                Transaction(
                    amount = amount, type = "expense", categoryId = cat, accountId = 1, note = note,
                    transactionDate = "2026-11-09T10:00:00", createdAt = "c", updatedAt = "u"
                )
            )
        }

        fun notes(): Set<String> = db.sql { s -> s.rows("SELECT note FROM transactions").map { it.getString(0) }.toSet() }
    }

    @Before
    fun setUp() = runBlocking {
        assumeTrue("set PAISA_NEXTCLOUD_URL to run against a real server", !server.isNullOrBlank())
        account = NextcloudStore.verify(
            server!!.trimEnd('/'),
            System.getenv("PAISA_NEXTCLOUD_USER") ?: "paisa",
            System.getenv("PAISA_NEXTCLOUD_PASSWORD") ?: ""
        )
        val store = NextcloudStore(account)
        store.listSync().forEach { store.deleteSync(it) }
        store.list().forEach { store.delete(it) }
    }

    @After
    fun tearDown() = dbs.forEach { it.close() }

    @Test
    fun twoDevicesSyncThroughNextcloud() = runBlocking {
        val phone = Device("phone")
        val laptop = Device("laptop")

        phone.add(250.0, "groceries on the phone")
        phone.sync.enable("same password".toCharArray())
        phone.sync.syncNow()

        laptop.sync.enable("same password".toCharArray())
        val first = laptop.sync.syncNow()
        assertTrue(first.received > 0)
        assertTrue("groceries on the phone" in laptop.notes())

        laptop.add(90.0, "coffee on the laptop")
        laptop.sync.syncNow()
        phone.sync.syncNow()
        assertTrue("coffee on the laptop" in phone.notes())
        assertEquals(phone.notes(), laptop.notes())

        // Files really are on the server, one or more per device, all encrypted.
        val files = phone.store.listSync()
        assertEquals(2, files.mapNotNull { SyncFileNames.parse(it.name)?.deviceId }.toSet().size)
    }

    @Test
    fun aWrongPasswordIsRefused() = runBlocking {
        val phone = Device("phone2")
        phone.add(10.0, "x")
        phone.sync.enable("right".toCharArray())
        phone.sync.syncNow()
        val other = Device("other2")
        try {
            other.sync.enable("wrong".toCharArray())
            throw AssertionError("enabled with the wrong password")
        } catch (e: kotlinx.io.IOException) {
            assertTrue(e.message!!.contains("sync password"))
        }
    }

    @Test
    fun backupUploadListAndRestore() = runBlocking {
        val phone = Device("phone3")
        phone.add(42.0, "backed up")
        val uploaded = phone.backups.backupNow()
        val listed = phone.backups.list()
        assertTrue(listed.any { it.name == uploaded.name && BackupNames.isBackup(it.name) })

        val fresh = Device("fresh3")
        fresh.backups.restore(fresh.backups.download(listed.first { it.name == uploaded.name }))
        assertTrue("backed up" in fresh.notes())
    }
}
