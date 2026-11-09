package codes.tashif.paisa.backup

import codes.tashif.paisa.shared.Gzip
import codes.tashif.paisa.shared.PaisaContainer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The web app reads files with the shared PaisaContainer and the phone with
 * BackupCrypto. Each has to open what the other seals.
 */
class SharedContainerParityTest {
    private val password = "पैसा 🔐 sync"
    private val json = """{"format":1,"tables":{"accounts":[]}}"""

    @Test
    fun sharedOpensWhatThePhoneSeals() = runBlocking {
        val key = BackupCrypto.deriveKey(password.toCharArray(), BackupCrypto.newSalt(), 2_000)
        val sealed = BackupCrypto.seal(Gzip.compress(json.encodeToByteArray()), key)
        assertEquals(json, PaisaContainer.openJson(sealed, password))
    }

    @Test
    fun thePhoneOpensWhatSharedSeals() = runBlocking {
        val payload = Gzip.compress(json.encodeToByteArray())
        val sealed = PaisaContainer.seal(payload, PaisaContainer.deriveKey(password, iterations = 2_000))
        val header = BackupCrypto.readHeader(sealed)
        val key = BackupCrypto.deriveKey(password.toCharArray(), header.salt!!, header.iterations)
        assertArrayEquals(payload, BackupCrypto.open(sealed, header, key))
    }
}
