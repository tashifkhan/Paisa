package codes.tashif.paisa.sync

import codes.tashif.paisa.data.sql.Sql
import codes.tashif.paisa.data.sql.nowMillis
import codes.tashif.paisa.data.sql.randomHexId
import kotlin.math.abs

/**
 * The database side of multi-device sync.
 *
 * Every synced table has two extra columns: `syncId`, a stable id that every
 * device uses for the same row, and `syncStamp`, a last-write-wins clock.
 * SQLite triggers keep both up to date and record every local change in
 * `sync_outbox`, so no write path in the app has to remember to call sync code.
 * Deletes leave a row in `sync_tombstones` so they reach other devices too.
 *
 * While `sync_state.applying` is 1 the triggers stand down. That is how
 * changes pulled from other devices get written without being sent back out.
 */
object SyncSchema {

    /**
     * A synced table.
     *
     * @param pk primary key column. `id` columns are local autoincrement ids and
     *   never leave the device; other keys (merchantName) are ordinary data.
     * @param fks local id columns that point at another synced table. Sync files
     *   carry the parent's syncId instead of the local id.
     * @param derived columns computed on each device and never synced.
     * @param only when set, the only columns that sync; the rest stay per device.
     * @param natural SQL for an id that two devices create independently for the
     *   same thing, written over a row alias such as `NEW.`. Rows without one get
     *   a random id.
     */
    class Table(
        val name: String,
        val pk: String = "id",
        val fks: Map<String, String> = emptyMap(),
        val derived: Set<String> = emptySet(),
        val natural: ((row: String) -> String)? = null,
        val only: Set<String>? = null
    ) {
        val localPk: Boolean get() = pk == "id"
    }

    /** Parents before children. Inserts run in this order, deletes in reverse. */
    val TABLES = listOf(
        // One row per device. Only the currency follows you; theme, app lock,
        // SMS scan state and the rest belong to the device they were set on.
        Table("settings", natural = { "'settings'" }, only = setOf("currency")),
        Table(
            "categories",
            natural = { r -> "'cat:' || ${r}type || ':' || lower(${r}name)" }
        ),
        Table(
            "accounts",
            derived = setOf("currentBalance"),
            natural = { r ->
                "CASE WHEN ${r}id = 1 THEN 'acct:seed:cash' " +
                    "WHEN ${r}id = 2 THEN 'acct:seed:bank' " +
                    "WHEN ${r}bankName IS NOT NULL AND ${r}accountLast4 IS NOT NULL " +
                    "THEN 'acct:' || lower(${r}bankName) || ':' || ${r}accountLast4 END"
            }
        ),
        Table(
            "transactions",
            fks = mapOf("categoryId" to "categories", "accountId" to "accounts"),
            natural = { r ->
                "CASE WHEN ${r}transactionHash IS NOT NULL THEN 'sms:' || ${r}transactionHash END"
            }
        ),
        Table("budgets", fks = mapOf("categoryId" to "categories")),
        Table("savings_goals"),
        Table(
            "recurring_transactions",
            fks = mapOf("categoryId" to "categories", "accountId" to "accounts")
        ),
        Table(
            "merchant_mappings",
            pk = "merchantName",
            natural = { r -> "'mm:' || lower(${r}merchantName)" }
        ),
        // Two devices that add the same name get one person, not two.
        Table("debt_people", natural = { r -> "'person:' || lower(${r}name)" }),
        Table("debt_entries", fks = mapOf("personId" to "debt_people"))
    )

    val TABLE_NAMES: Array<String> = TABLES.map { it.name }.toTypedArray()

    /**
     * Synced tables that exist in [db]. An old schema being migrated doesn't
     * have the tables later versions add, so loops over [TABLES] go through this.
     */
    suspend fun existingTables(db: Sql): List<Table> {
        val present = db.rows("SELECT name FROM sqlite_master WHERE type = 'table'")
            .mapTo(mutableSetOf()) { it.getString(0) }
        return TABLES.filter { it.name in present }
    }

    fun table(name: String): Table? = TABLES.firstOrNull { it.name == name }

    const val STATE = "sync_state"
    const val OUTBOX = "sync_outbox"
    const val TOMBSTONES = "sync_tombstones"

    /** Local bookkeeping that never goes into a backup file. */
    val LOCAL_ONLY_TABLES = setOf(STATE, OUTBOX)

    val META_COLUMNS = setOf("syncId", "syncStamp")

    /** Wall clock in ms, from inside SQLite. */
    private const val NOW_MS = "CAST((julianday('now') - 2440587.5) * 86400000.0 AS INTEGER)"
    private const val APPLYING_OFF = "(SELECT applying FROM $STATE WHERE id = 1) = 0"
    private const val CLOCK = "(SELECT clock FROM $STATE WHERE id = 1)"
    private const val TICK = "UPDATE $STATE SET clock = max(clock + 1, $NOW_MS) WHERE id = 1"
    private const val RANDOM_ID = "lower(hex(randomblob(16)))"

    /**
     * Sets [Account.currentBalance] from its anchor. Runs on every device after
     * any change, so balances agree no matter which order changes arrive in.
     */
    const val RECOMPUTE_BALANCES = """
        UPDATE accounts SET currentBalance = balanceAnchor + COALESCE((
            SELECT SUM(CASE WHEN t.type = 'income' THEN t.amount ELSE -t.amount END)
            FROM transactions t
            WHERE t.accountId = accounts.id AND t.isDeleted = 0
              AND t.transactionDate > accounts.balanceAnchorAt
        ), 0)
    """

    /** Creates the bookkeeping tables and (re)creates the triggers. Safe on every open. */
    suspend fun install(db: Sql) {
        db.exec(
            "CREATE TABLE IF NOT EXISTS $STATE (id INTEGER PRIMARY KEY, " +
                "applying INTEGER NOT NULL DEFAULT 0, clock INTEGER NOT NULL DEFAULT 0)"
        )
        db.exec("INSERT OR IGNORE INTO $STATE (id, applying, clock) VALUES (1, 0, 0)")
        // A crash mid-apply rolls back with its transaction, but be sure.
        db.exec("UPDATE $STATE SET applying = 0 WHERE id = 1")
        for (t in listOf(OUTBOX, TOMBSTONES)) {
            db.exec(
                "CREATE TABLE IF NOT EXISTS $t (tableName TEXT NOT NULL, syncId TEXT NOT NULL, " +
                    "stamp INTEGER NOT NULL, PRIMARY KEY (tableName, syncId))"
            )
        }
        for (table in TABLES) installTriggers(db, table)
    }

    suspend fun isInstalled(db: Sql): Boolean =
        db.first("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = '$STATE'") != null

    /** Columns whose values sync, in table order. */
    suspend fun syncedColumns(db: Sql, table: Table): List<String> =
        columnsOf(db, table.name).filter { col ->
            col !in META_COLUMNS && col !in table.derived && !(table.localPk && col == table.pk) &&
                (table.only == null || col in table.only)
        }

    suspend fun columnsOf(db: Sql, table: String): List<String> {
        val rows = db.rows("PRAGMA table_info(`$table`)")
        return rows.map { it.getString(it.index("name")) }
    }

    private suspend fun installTriggers(db: Sql, table: Table) {
        val t = table.name
        val cols = syncedColumns(db, table)
        val changed = if (cols.isEmpty()) "0" else cols.joinToString(" OR ") { "OLD.`$it` IS NOT NEW.`$it`" }
        val natural = table.natural?.invoke("NEW.")
        val newId = if (natural == null) {
            RANDOM_ID
        } else {
            // Fall back to a random id if another row already holds the natural one,
            // e.g. a renamed category still owns its original name's id.
            "CASE WHEN ($natural) IS NOT NULL AND NOT EXISTS " +
                "(SELECT 1 FROM `$t` WHERE syncId = ($natural)) THEN ($natural) ELSE $RANDOM_ID END"
        }

        db.exec("DROP TRIGGER IF EXISTS sync_ins_$t")
        db.exec("DROP TRIGGER IF EXISTS sync_upd_$t")
        db.exec("DROP TRIGGER IF EXISTS sync_del_$t")

        db.exec(
            """
            CREATE TRIGGER sync_ins_$t AFTER INSERT ON `$t` WHEN $APPLYING_OFF
            BEGIN
                $TICK;
                UPDATE `$t` SET syncId = COALESCE(NEW.syncId, $newId), syncStamp = $CLOCK
                    WHERE rowid = NEW.rowid;
                DELETE FROM $TOMBSTONES WHERE tableName = '$t'
                    AND syncId = (SELECT syncId FROM `$t` WHERE rowid = NEW.rowid);
                DELETE FROM $OUTBOX WHERE tableName = '$t'
                    AND syncId = (SELECT syncId FROM `$t` WHERE rowid = NEW.rowid);
                INSERT INTO $OUTBOX (tableName, syncId, stamp)
                    SELECT '$t', syncId, syncStamp FROM `$t` WHERE rowid = NEW.rowid;
            END
            """.trimIndent()
        )

        // Room's @Update writes every column, including stale syncId/syncStamp from
        // an old object. Keep the id, never let the stamp go backwards, and only
        // bump it (and queue the row) when a synced value actually changed.
        //
        // The WHEN clause also ends recursion: some SQLite builds turn on
        // recursive_triggers, and this trigger's own UPDATE changes neither a
        // synced value nor anything it would fix.
        // No INSERT OR REPLACE in trigger bodies: the firing statement's conflict
        // clause overrides it, and Room's @Update runs UPDATE OR ABORT.
        db.exec(
            """
            CREATE TRIGGER sync_upd_$t AFTER UPDATE ON `$t` WHEN $APPLYING_OFF AND (
                ($changed) OR NEW.syncId IS NULL OR NEW.syncStamp < OLD.syncStamp)
            BEGIN
                UPDATE $STATE SET clock = max(clock + 1, $NOW_MS) WHERE id = 1 AND ($changed);
                UPDATE `$t` SET
                    syncId = COALESCE(NEW.syncId, OLD.syncId, $RANDOM_ID),
                    syncStamp = CASE WHEN ($changed) THEN $CLOCK
                        ELSE max(NEW.syncStamp, OLD.syncStamp) END
                    WHERE rowid = NEW.rowid;
                DELETE FROM $OUTBOX WHERE tableName = '$t' AND ($changed)
                    AND syncId = (SELECT syncId FROM `$t` WHERE rowid = NEW.rowid);
                INSERT INTO $OUTBOX (tableName, syncId, stamp)
                    SELECT '$t', syncId, syncStamp FROM `$t`
                    WHERE rowid = NEW.rowid AND syncId IS NOT NULL AND ($changed);
            END
            """.trimIndent()
        )

        db.exec(
            """
            CREATE TRIGGER sync_del_$t AFTER DELETE ON `$t`
                WHEN $APPLYING_OFF AND OLD.syncId IS NOT NULL
            BEGIN
                $TICK;
                DELETE FROM $TOMBSTONES WHERE tableName = '$t' AND syncId = OLD.syncId;
                INSERT INTO $TOMBSTONES (tableName, syncId, stamp) VALUES ('$t', OLD.syncId, $CLOCK);
                DELETE FROM $OUTBOX WHERE tableName = '$t' AND syncId = OLD.syncId;
                INSERT INTO $OUTBOX (tableName, syncId, stamp) VALUES ('$t', OLD.syncId, $CLOCK);
            END
            """.trimIndent()
        )
    }

    suspend fun setApplying(db: Sql, applying: Boolean) {
        if (!isInstalled(db)) return
        db.exec("UPDATE $STATE SET applying = ${if (applying) 1 else 0} WHERE id = 1")
    }

    /** Moves the local clock past [stamp] so new local edits win over what we just saw. */
    suspend fun observeClock(db: Sql, stamp: Long) {
        db.exec("UPDATE $STATE SET clock = max(clock, ?) WHERE id = 1", stamp)
    }

    /** Next clock value, for writes made while the triggers are off. */
    suspend fun tick(db: Sql): Long {
        db.exec(TICK)
        return db.long("SELECT clock FROM $STATE WHERE id = 1") ?: nowMillis()
    }

    suspend fun recomputeBalances(db: Sql) {
        db.exec(RECOMPUTE_BALANCES)
    }

    /**
     * Gives every row without a syncId one, natural where possible, stamped
     * [stamp]. Used by the 9 to 10 migration (stamp 1), the first-run seed
     * (stamp 0, so any real edit on another device wins), and old backups.
     */
    suspend fun backfillIds(db: Sql, stamp: Long) {
        for (table in existingTables(db)) {
            val used = db.rows("SELECT syncId FROM `${table.name}` WHERE syncId IS NOT NULL")
                .mapTo(mutableSetOf()) { it.getString(0) }
            val natural = table.natural?.invoke("") ?: "NULL"
            val pending = db.rows(
                "SELECT rowid, ($natural) FROM `${table.name}` WHERE syncId IS NULL ORDER BY rowid"
            ).map { it.getLong(0) to (if (it.isNull(1)) null else it.getString(1)) }
            for ((rowid, candidate) in pending) {
                val id = candidate?.takeIf { it !in used } ?: randomId()
                used += id
                db.exec(
                    "UPDATE `${table.name}` SET syncId = ?, syncStamp = ? WHERE rowid = ?",
                    id, stamp, rowid
                )
            }
        }
    }

    fun randomId(): String = randomHexId()

    /**
     * Picks an anchor for each account so the new balance formula reproduces
     * the balance it already shows. Tries, in order: the opening balance plus
     * every transaction, the last SMS-reported balance plus what came after it,
     * and finally the current balance pinned after the newest transaction.
     */
    suspend fun deriveAnchors(db: Sql) {
        data class Acc(val id: Long, val opening: Double, val current: Double)
        val accounts = db.rows("SELECT id, openingBalance, currentBalance FROM accounts")
            .map { Acc(it.getLong(0), it.getDouble(1), it.getDouble(2)) }
        for (a in accounts) {
            val (anchor, at) = anchorFor(db, a.id, a.opening, a.current)
            db.exec(
                "UPDATE accounts SET balanceAnchor = ?, balanceAnchorAt = ? WHERE id = ?",
                anchor, at, a.id
            )
        }
        recomputeBalances(db)
    }

    private suspend fun anchorFor(
        db: Sql,
        accountId: Long,
        opening: Double,
        current: Double
    ): Pair<Double, String> {
        if (close(opening + sumAfter(db, accountId, ""), current)) return opening to ""
        val last = db.first(
            "SELECT balanceAfter, transactionDate FROM transactions WHERE accountId = ? " +
                "AND balanceAfter IS NOT NULL AND isDeleted = 0 ORDER BY transactionDate DESC LIMIT 1",
            accountId
        )
        if (last != null) {
            val reported = last.getDouble(0)
            val date = last.getString(1)
            if (close(reported + sumAfter(db, accountId, date), current)) return reported to date
        }
        val newest = db.string(
            "SELECT MAX(transactionDate) FROM transactions WHERE accountId = ?",
            accountId
        ) ?: ""
        return current to newest
    }

    private suspend fun sumAfter(db: Sql, accountId: Long, after: String): Double =
        db.double(
            "SELECT COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE -amount END), 0) " +
                "FROM transactions WHERE accountId = ? AND isDeleted = 0 AND transactionDate > ?",
            accountId, after
        ) ?: 0.0

    private fun close(a: Double, b: Double) = abs(a - b) < 0.005
}
