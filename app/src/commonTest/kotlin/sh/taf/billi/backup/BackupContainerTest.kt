package sh.taf.billi.backup

import sh.taf.billi.ai.json.JSONObject
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/** Runs on Android's JVM crypto and on WebCrypto, against the same bytes. */
class BackupContainerTest {

    private suspend fun openJson(data: ByteArray, password: String?): String {
        val header = BackupCrypto.readHeader(data)
        val key = if (header.encrypted) {
            BackupCrypto.deriveKey(password!!.toCharArray(), header.salt!!, header.iterations)
        } else {
            null
        }
        return Gzip.decompress(BackupCrypto.open(data, header, key)).decodeToString()
    }

    @Test
    fun opensASyncFileWrittenByAndroid() = runTest(timeout = 2.minutes) {
        val file = JSONObject(openJson(GoldenFiles.syncFull, GoldenFiles.SYNC_PASSWORD))
        assertEquals("0123456789abcdef", file.getString("deviceId"))
        assertEquals("Golden phone", file.getString("deviceName"))
        assertTrue(file.getBoolean("full"))
        assertEquals(1L, file.getLong("seq"))
        val rows = file.getJSONObject("rows")
        val accounts = rows.getJSONArray("accounts")
        // Sync rows wrap their columns in "v".
        val names = (0 until accounts.length()).map { accounts.getJSONObject(it).getJSONObject("v").getString("name") }
        assertTrue("Golden Bank" in names)
        assertTrue(rows.getJSONArray("categories").length() > 0, "seeded categories should sync")
    }

    @Test
    fun wrongPasswordFailsCleanly() = runTest(timeout = 2.minutes) {
        assertFailsWith<BackupCrypto.WrongPasswordException> {
            openJson(GoldenFiles.syncFull, "paisa sync")
        }
    }

    @Test
    fun aChangedByteFailsAuthentication() = runTest(timeout = 2.minutes) {
        val tampered = GoldenFiles.syncFull.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        assertFailsWith<BackupCrypto.WrongPasswordException> {
            openJson(tampered, GoldenFiles.SYNC_PASSWORD)
        }
    }

    @Test
    fun sealThenOpenRoundTrips() = runTest(timeout = 2.minutes) {
        val payload = Gzip.compress("""{"format":1,"tables":{}}""".encodeToByteArray())
        val key = BackupCrypto.deriveKey("hunter2".toCharArray(), BackupCrypto.newSalt(), iterations = 1_000)
        val sealed = BackupCrypto.seal(payload, key)
        val header = BackupCrypto.readHeader(sealed)
        assertTrue(header.encrypted)
        assertEquals(1_000, header.iterations)
        assertTrue(key.matches(header))
        assertContentEquals(payload, BackupCrypto.open(sealed, header, key))
    }

    @Test
    fun unencryptedFilesOpenWithoutAPassword() = runTest {
        val payload = Gzip.compress("""{"format":1,"tables":{"accounts":[]}}""".encodeToByteArray())
        val sealed = BackupCrypto.seal(payload, null)
        assertTrue(JSONObject(openJson(sealed, null)).has("tables"))
    }

    @Test
    fun rejectsFilesThatAreNotPaisa() {
        assertFailsWith<BackupCrypto.NotABackupException> { BackupCrypto.readHeader("hello".encodeToByteArray()) }
        assertFailsWith<BackupCrypto.NotABackupException> {
            BackupCrypto.readHeader(GoldenFiles.syncFull.copyOfRange(0, 20))
        }
    }
}
