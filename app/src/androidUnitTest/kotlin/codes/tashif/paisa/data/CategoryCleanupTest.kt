package codes.tashif.paisa.data

import codes.tashif.paisa.testing.exec
import codes.tashif.paisa.testing.inMemoryDatabase
import codes.tashif.paisa.testing.long
import codes.tashif.paisa.testing.sql
import codes.tashif.paisa.testing.string
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The 13 to 14 cleanup that leaves one catch-all expense category. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CategoryCleanupTest {

    private val db: AppDatabase = inMemoryDatabase()

    @After
    fun tearDown() = db.close()

    private fun addOthers(): Long {
        db.exec(
            "INSERT INTO categories (name, type, icon, color, isDefault, orderIndex) " +
                "VALUES ('Others', 'expense', 'more_horiz', '#E8AEB2', 0, 21)"
        )
        return db.long("SELECT id FROM categories WHERE type = 'expense' AND name = 'Others'")
    }

    private fun count(where: String) = db.long("SELECT COUNT(*) FROM categories WHERE $where")

    @Test
    fun aFreshDatabaseSeedsOneOther() {
        assertEquals(1, count("type = 'expense' AND name IN ('Other', 'Others')"))
        assertEquals(1, count("type = 'income' AND name = 'Other'"))
    }

    @Test
    fun othersMovesIntoOther() {
        val others = addOthers()
        val other = db.long("SELECT id FROM categories WHERE type = 'expense' AND name = 'Other'")
        db.exec(
            "INSERT INTO transactions (amount, type, categoryId, accountId, note, transactionDate, createdAt, updatedAt, tags, source, currency, isDeleted) " +
                "VALUES (50, 'expense', ?, 1, '', '2026-11-01T10:00:00', 'c', 'u', '', 'manual', 'INR', 0)", others
        )
        db.exec("INSERT INTO budgets (categoryId, budgetAmount, month, year, budgetType) VALUES (?, 500, 11, 2026, 'MONTHLY')", others)
        db.exec(
            "INSERT INTO merchant_mappings (merchantName, categoryName, categoryType, createdAt, updatedAt) " +
                "VALUES ('Corner Shop', 'Others', 'expense', 'c', 'u')"
        )

        db.sql { mergeOthersIntoOther(it) }

        assertEquals(0, count("name = 'Others'"))
        assertEquals(other, db.long("SELECT categoryId FROM transactions"))
        assertEquals(other, db.long("SELECT categoryId FROM budgets"))
        assertEquals("Other", db.string("SELECT categoryName FROM merchant_mappings"))
        // The delete has to reach other devices.
        assertEquals(1, db.long("SELECT COUNT(*) FROM sync_tombstones WHERE syncId = 'cat:expense:others'"))
    }

    @Test
    fun othersAloneIsRenamed() {
        db.exec("DELETE FROM categories WHERE type = 'expense' AND name = 'Other'")
        val others = addOthers()

        db.sql { mergeOthersIntoOther(it) }

        assertEquals("Other", db.string("SELECT name FROM categories WHERE id = ?", others))
        assertEquals(1, count("type = 'expense' AND name = 'Other'"))
    }
}
