package sh.taf.billi.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test

class BackupCryptoTest {

    private val payload = "gzip-json-stand-in ₹ 🙂".toByteArray()

    // Low iteration count keeps the test fast; the format stores it per file.
    private fun key(pw: String, salt: ByteArray = BackupCrypto.newSalt()) = runBlocking {
        BackupCrypto.deriveKey(pw.toCharArray(), salt, iterations = 1_000)
    }

    @Test
    fun plainRoundTrip(): Unit = runBlocking {
        val sealed = BackupCrypto.seal(payload, null)
        val header = BackupCrypto.readHeader(sealed)
        assertFalse(header.encrypted)
        assertArrayEquals(payload, BackupCrypto.open(sealed, header, null))
    }

    @Test
    fun encryptedRoundTripWithPasswordOnAnotherDevice(): Unit = runBlocking {
        val sealed = BackupCrypto.seal(payload, key("hunter22"))
        val header = BackupCrypto.readHeader(sealed)
        assertTrue(header.encrypted)
        // New phone: derive again from the password and the salt stored in the header.
        val again = BackupCrypto.deriveKey("hunter22".toCharArray(), header.salt!!, header.iterations)
        assertTrue(again.matches(header))
        assertArrayEquals(payload, BackupCrypto.open(sealed, header, again))
    }

    @Test(expected = BackupCrypto.WrongPasswordException::class)
    fun wrongPasswordFails(): Unit = runBlocking {
        val sealed = BackupCrypto.seal(payload, key("hunter22"))
        val header = BackupCrypto.readHeader(sealed)
        val wrong = BackupCrypto.deriveKey("hunter23".toCharArray(), header.salt!!, header.iterations)
        BackupCrypto.open(sealed, header, wrong)
    }

    @Test(expected = BackupCrypto.WrongPasswordException::class)
    fun tamperedBodyFails(): Unit = runBlocking {
        val k = key("pw123456")
        val sealed = BackupCrypto.seal(payload, k)
        sealed[sealed.size - 1] = (sealed[sealed.size - 1].toInt() xor 1).toByte()
        BackupCrypto.open(sealed, BackupCrypto.readHeader(sealed), k)
    }

    @Test(expected = BackupCrypto.WrongPasswordException::class)
    fun tamperedHeaderFails(): Unit = runBlocking {
        val k = key("pw123456")
        val sealed = BackupCrypto.seal(payload, k)
        // Flip a bit in the IV, which is authenticated as associated data.
        val ivOffset = 8 + 2 + BackupCrypto.SALT_BYTES + 4
        sealed[ivOffset] = (sealed[ivOffset].toInt() xor 1).toByte()
        BackupCrypto.open(sealed, BackupCrypto.readHeader(sealed), k)
    }

    @Test
    fun freshIvPerBackup(): Unit = runBlocking {
        val k = key("pw123456")
        val a = BackupCrypto.seal(payload, k)
        val b = BackupCrypto.seal(payload, k)
        assertFalse(a.contentEquals(b))
    }

    @Test(expected = BackupCrypto.NotABackupException::class)
    fun rejectsOtherFiles(): Unit = runBlocking {
        BackupCrypto.readHeader("%PDF-1.7 hello".toByteArray())
    }

    @Test(expected = BackupCrypto.NotABackupException::class)
    fun rejectsTruncatedEncryptedHeader(): Unit = runBlocking {
        val sealed = BackupCrypto.seal(payload, key("pw123456"))
        BackupCrypto.readHeader(sealed.copyOfRange(0, 20))
    }

    @Test(expected = BackupCrypto.NotABackupException::class)
    fun rejectsFutureVersion(): Unit = runBlocking {
        val sealed = BackupCrypto.seal(payload, null)
        sealed[8] = 99
        BackupCrypto.readHeader(sealed)
    }

    @Test
    fun namesEncodeTimeAndSort(): Unit = runBlocking {
        val t = 1_780_000_000_000L
        val name = BackupNames.forTime(t)
        assertTrue(BackupNames.isBackup(name))
        assertEquals(t / 1000 * 1000, BackupNames.timeOf(name))
        assertFalse(BackupNames.isBackup("notes.txt"))
        assertNotNull(BackupNames.timeOf(BackupNames.forTime(0)))
    }

    @Test
    fun paisaBackupsStillRestoreButAreNeverPruned(): Unit = runBlocking {
        val t = 1_780_000_000_000L
        val current = BackupNames.forTime(t)
        assertTrue(current.startsWith("billi-backup-") && current.endsWith(".billi"))
        assertTrue(BackupNames.isCurrent(current))

        val legacy = "paisa-backup-" + current.removePrefix("billi-backup-").removeSuffix(".billi") + ".paisa"
        assertTrue(BackupNames.isBackup(legacy))
        assertTrue(BackupNames.isLegacy(legacy))
        assertFalse(BackupNames.isCurrent(legacy))
        assertEquals(t / 1000 * 1000, BackupNames.timeOf(legacy))
    }
}
