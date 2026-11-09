package codes.tashif.paisa.shared

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class PaisaContainerTest {

    @Test
    fun opensASyncFileWrittenByAndroid() = runTest(timeout = 2.minutes) {
        val text = PaisaContainer.openJson(GoldenFiles.syncFull, GoldenFiles.SYNC_PASSWORD)
        val file = assertIs<PaisaFile.SyncChanges>(PaisaFile.parse(text))
        assertEquals("0123456789abcdef", file.deviceId)
        assertEquals("Golden phone", file.deviceName)
        assertTrue(file.full)
        assertEquals(1L, file.seq)
        val accounts = file.samples.getValue("accounts")
        assertTrue(accounts.any { it["name"]?.jsonPrimitive?.content == "Golden Bank" })
        assertTrue(file.tableCounts.getValue("categories") > 0, "seeded categories should sync")
    }

    @Test
    fun wrongPasswordFailsCleanly() = runTest(timeout = 2.minutes) {
        assertFailsWith<WrongPasswordException> {
            PaisaContainer.openJson(GoldenFiles.syncFull, "paisa sync")
        }
    }

    @Test
    fun aChangedByteFailsAuthentication() = runTest(timeout = 2.minutes) {
        val tampered = GoldenFiles.syncFull.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        assertFailsWith<WrongPasswordException> {
            PaisaContainer.openJson(tampered, GoldenFiles.SYNC_PASSWORD)
        }
    }

    @Test
    fun sealThenOpenRoundTrips() = runTest(timeout = 2.minutes) {
        val payload = Gzip.compress("""{"format":1,"tables":{}}""".encodeToByteArray())
        val key = PaisaContainer.deriveKey("hunter2", iterations = 1_000)
        val sealed = PaisaContainer.seal(payload, key)
        val header = PaisaContainer.readHeader(sealed)
        assertTrue(header.encrypted)
        assertEquals(1_000, header.iterations)
        assertTrue(key.matches(header))
        assertContentEquals(payload, PaisaContainer.open(sealed, header, key))
    }

    @Test
    fun unencryptedFilesOpenWithoutAPassword() = runTest {
        val payload = Gzip.compress("""{"format":1,"tables":{"accounts":[]}}""".encodeToByteArray())
        val sealed = PaisaContainer.seal(payload, null)
        assertIs<PaisaFile.Backup>(PaisaFile.parse(PaisaContainer.openJson(sealed, null)))
    }

    @Test
    fun rejectsFilesThatAreNotPaisa() {
        assertFailsWith<NotAPaisaFileException> { PaisaContainer.readHeader("hello".encodeToByteArray()) }
        assertFailsWith<NotAPaisaFileException> {
            PaisaContainer.readHeader(GoldenFiles.syncFull.copyOfRange(0, 20))
        }
    }
}
