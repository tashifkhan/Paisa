package sh.taf.billi.data

import sh.taf.billi.data.sql.Sql

/**
 * Older versions seeded two catch-all expense categories, "Others" and "Other".
 * This leaves one, "Other", the same name income uses.
 *
 * "Other" is the row that stays, because its sync id (`cat:expense:other`) is
 * the one a fresh install seeds. Everything filed under "Others" moves to it.
 * With the sync triggers on, the moves and the delete reach other devices.
 */
internal suspend fun mergeOthersIntoOther(db: Sql) {
    val others = db.long("SELECT id FROM categories WHERE type = 'expense' AND name = 'Others'")
    if (others != null) {
        val other = db.long("SELECT id FROM categories WHERE type = 'expense' AND name = 'Other'")
        if (other == null) {
            db.exec("UPDATE categories SET name = 'Other' WHERE id = ?", others)
        } else {
            for (table in listOf("transactions", "budgets", "recurring_transactions")) {
                db.exec("UPDATE `$table` SET categoryId = ? WHERE categoryId = ?", other, others)
            }
            db.exec("DELETE FROM categories WHERE id = ?", others)
        }
    }
    db.exec(
        "UPDATE merchant_mappings SET categoryName = 'Other' " +
            "WHERE categoryName = 'Others' AND categoryType = 'expense'"
    )
}
