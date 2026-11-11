package codes.tashif.paisa.data.demo

import codes.tashif.paisa.testing.double
import codes.tashif.paisa.testing.inMemoryDatabase
import codes.tashif.paisa.testing.long
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DemoDataTest {
    @Test
    fun seedsAFullConsistentDatabase() {
        val db = inMemoryDatabase()
        assertTrue(runBlocking { DemoData.isEmpty(db) })
        runBlocking { DemoData.seed(db) }

        assertTrue(db.long("SELECT COUNT(*) FROM transactions") > 100)
        assertEquals(4L, db.long("SELECT COUNT(*) FROM accounts"))
        assertEquals(5L, db.long("SELECT COUNT(*) FROM budgets"))
        assertEquals(2L, db.long("SELECT COUNT(*) FROM debt_people"))
        assertEquals(0L, db.long("SELECT COUNT(*) FROM transactions WHERE syncId IS NULL"))
        // Balances follow the anchor formula, not the placeholder column values.
        val expected = db.double(
            "SELECT 86500 + SUM(CASE WHEN type = 'income' THEN amount ELSE -amount END) " +
                "FROM transactions WHERE accountId = 2"
        )
        assertEquals(expected, db.double("SELECT currentBalance FROM accounts WHERE id = 2"), 0.001)
        assertEquals(1L, db.long("SELECT onboardingCompleted FROM settings"))
        db.close()
    }
}
