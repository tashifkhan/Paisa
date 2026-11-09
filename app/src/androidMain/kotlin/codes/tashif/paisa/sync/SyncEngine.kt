package codes.tashif.paisa.sync

import codes.tashif.paisa.backup.BackupCrypto
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.sql.JsonOut
import codes.tashif.paisa.data.sql.Sql
import codes.tashif.paisa.data.sql.toScalar
import codes.tashif.paisa.shared.Gzip
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Turns local changes into a change file and merges other devices' change files.
 *
 * A change file is gzip'd JSON:
 * ```
 * { format, dbVersion, deviceId, deviceName, seq, full, createdAt,
 *   rows: { table: [ { id: syncId, s: syncStamp, v: { column: value } } ] },
 *   deleted: [ { t: table, id: syncId, s: stamp } ] }
 * ```
 * Foreign keys carry the parent's syncId, never a local id.
 *
 * Merging is last-write-wins per row on `syncStamp`, with deletes kept as
 * tombstones. Applying the same file twice, or files in a different order,
 * gives the same result, so a dropped or repeated upload can't corrupt data.
 */
object SyncEngine {

    const val FORMAT = 1

    data class Header(
        val deviceId: String,
        val deviceName: String,
        val seq: Long,
        val full: Boolean,
        val createdAt: Long
    )

    /** One queued local change, cleared from the outbox once its upload succeeds. */
    data class OutboxEntry(val table: String, val syncId: String, val stamp: Long)

    class Export(val payload: ByteArray, val changes: Int, val outbox: List<OutboxEntry>)

    data class ApplyResult(val applied: Int, val skipped: Int, val header: Header)

    private class Row(val syncId: String, val stamp: Long, val values: Map<String, Any?>)
    private class Tomb(val table: String, val syncId: String, val stamp: Long)

    // --- Export ---

    /**
     * Writes every row (when [full]) or only the rows queued in the outbox.
     * Call inside a transaction so the rows and the outbox snapshot agree.
     */
    suspend fun export(db: Sql, header: Header): Export {
        val outbox = readOutbox(db)
        val queued = outbox.groupBy { it.table }.mapValues { (_, v) -> v.map { it.syncId }.toSet() }
        val parentIds = SyncSchema.TABLES.filter { it.localPk }.associate { it.name to localToSyncIds(db, it.name) }
        var changes = 0

        val w = JsonOut()
        w.beginObject()
        w.name("format").value(FORMAT.toLong())
        w.name("dbVersion").value(db.userVersion().toLong())
        w.name("deviceId").value(header.deviceId)
        w.name("deviceName").value(header.deviceName)
        w.name("seq").value(header.seq)
        w.name("full").value(header.full)
        w.name("createdAt").value(header.createdAt)

        w.name("rows").beginObject()
        for (table in SyncSchema.TABLES) {
            val wanted = queued[table.name].orEmpty()
            if (!header.full && wanted.isEmpty()) continue
            val cols = SyncSchema.syncedColumns(db, table)
            w.name(table.name).beginArray()
            val select = cols.joinToString(", ") { "`$it`" }
            for (r in db.rows("SELECT syncId, syncStamp, $select FROM `${table.name}` WHERE syncId IS NOT NULL")) {
                val syncId = r.getString(0)
                if (!header.full && syncId !in wanted) continue
                w.beginObject()
                w.name("id").value(syncId)
                w.name("s").value(r.getLong(1))
                w.name("v").beginObject()
                cols.forEachIndexed { i, col ->
                    w.name(col)
                    val parent = table.fks[col]
                    if (parent != null && !r.isNull(i + 2)) {
                        val ref = parentIds[parent]?.get(r.getLong(i + 2))
                        if (ref == null) w.nullValue() else w.value(ref)
                    } else {
                        writeValue(w, r.get(i + 2))
                    }
                }
                w.endObject()
                w.endObject()
                changes++
            }
            w.endArray()
        }
        w.endObject()

        w.name("deleted").beginArray()
        val tombQuery = if (header.full) {
            "SELECT tableName, syncId, stamp FROM ${SyncSchema.TOMBSTONES}"
        } else {
            "SELECT t.tableName, t.syncId, t.stamp FROM ${SyncSchema.TOMBSTONES} t " +
                "JOIN ${SyncSchema.OUTBOX} o ON o.tableName = t.tableName AND o.syncId = t.syncId"
        }
        for (r in db.rows(tombQuery)) {
            w.beginObject()
            w.name("t").value(r.getString(0))
            w.name("id").value(r.getString(1))
            w.name("s").value(r.getLong(2))
            w.endObject()
            changes++
        }
        w.endArray()
        w.endObject()
        return Export(Gzip.compress(w.toString().encodeToByteArray()), changes, outbox)
    }

    suspend fun hasPendingChanges(db: Sql): Boolean =
        db.long("SELECT EXISTS (SELECT 1 FROM ${SyncSchema.OUTBOX})") == 1L

    /** Drops uploaded entries. Rows changed again since keep their newer entry. */
    suspend fun clearOutbox(db: Sql, entries: List<OutboxEntry>) {
        for (e in entries) {
            db.exec(
                "DELETE FROM ${SyncSchema.OUTBOX} WHERE tableName = ? AND syncId = ? AND stamp <= ?",
                e.table, e.syncId, e.stamp
            )
        }
    }

    /** After a restore the whole database is new to the cloud. */
    suspend fun clearAllOutbox(db: Sql) {
        db.exec("DELETE FROM ${SyncSchema.OUTBOX}")
    }

    private suspend fun readOutbox(db: Sql): List<OutboxEntry> =
        db.rows("SELECT tableName, syncId, stamp FROM ${SyncSchema.OUTBOX}")
            .map { OutboxEntry(it.getString(0), it.getString(1), it.getLong(2)) }

    private fun writeValue(w: JsonOut, value: Any?) {
        when (value) {
            null -> w.nullValue()
            is Long -> w.value(value)
            is Double -> w.value(value)
            is ByteArray -> w.value(value.decodeToString())
            else -> w.value(value.toString())
        }
    }

    // --- Apply ---

    /** Reads only the header fields, to decide whether and in what order to apply. */
    suspend fun readHeader(gzipJson: ByteArray): Header = parse(gzipJson).header

    /**
     * Merges a change file from another device. Call inside one transaction, so
     * a bad file changes nothing. Local edits made while this runs wait for it.
     */
    suspend fun apply(db: Sql, gzipJson: ByteArray): ApplyResult {
        val file = parse(gzipJson)
        var applied = 0
        var skipped = 0
        SyncSchema.setApplying(db, true)
        val toLocal = SyncSchema.TABLES.filter { it.localPk }
            .associate { it.name to syncToLocalIds(db, it.name) }
        val toSync = SyncSchema.TABLES.filter { it.localPk }
            .associate { it.name to localToSyncIds(db, it.name) }
        var maxStamp = 0L

        for (table in SyncSchema.TABLES) {
            val rows = file.rows[table.name] ?: continue
            val cols = SyncSchema.syncedColumns(db, table).toSet()
            for (row in rows) {
                maxStamp = maxOf(maxStamp, row.stamp)
                when (applyRow(db, table, cols, row, toLocal, toSync)) {
                    Outcome.APPLIED -> applied++
                    Outcome.SKIPPED -> skipped++
                    Outcome.UNCHANGED -> Unit
                }
            }
        }

        // Children before parents, so a deleted account's transactions go first.
        val byTable = file.deleted.groupBy { it.table }
        for (table in SyncSchema.TABLES.asReversed()) {
            for (tomb in byTable[table.name].orEmpty()) {
                maxStamp = maxOf(maxStamp, tomb.stamp)
                if (applyTombstone(db, table, tomb)) applied++
            }
        }

        SyncSchema.observeClock(db, maxStamp)
        SyncSchema.setApplying(db, false)
        SyncSchema.recomputeBalances(db)
        return ApplyResult(applied, skipped, file.header)
    }

    private enum class Outcome { APPLIED, UNCHANGED, SKIPPED }

    private suspend fun applyRow(
        db: Sql,
        table: Table,
        localCols: Set<String>,
        row: Row,
        toLocal: Map<String, MutableMap<String, Long>>,
        toSync: Map<String, MutableMap<Long, String>>
    ): Outcome {
        val tombStamp = tombstoneStamp(db, table.name, row.syncId)
        if (tombStamp != null) {
            if (tombStamp >= row.stamp) return Outcome.UNCHANGED
            // Edited on another device after we deleted it: the edit wins.
            db.exec(
                "DELETE FROM ${SyncSchema.TOMBSTONES} WHERE tableName = ? AND syncId = ?",
                table.name, row.syncId
            )
        }

        val values = linkedMapOf<String, Any?>()
        for ((col, value) in row.values) {
            if (col !in localCols) continue
            val parent = table.fks[col]
            if (parent != null && value != null) {
                val localId = toLocal[parent]?.get(value.toString())
                if (localId == null) {
                    // The parent was deleted here or never arrived. Required links
                    // can't be satisfied, so leave the row out.
                    if (isRequired(db, table.name, col)) return Outcome.SKIPPED
                    values[col] = null
                } else {
                    values[col] = localId
                }
            } else {
                values[col] = sqlValue(value)
            }
        }

        val local = db.first("SELECT rowid, syncStamp FROM `${table.name}` WHERE syncId = ?", row.syncId)
            ?.let { it.getLong(0) to it.getLong(1) }

        if (local != null) {
            val (rowid, localStamp) = local
            val wins = row.stamp > localStamp ||
                (row.stamp == localStamp && canonical(row.values) > canonical(localValues(db, table, rowid, localCols, toSync)))
            if (!wins) return Outcome.UNCHANGED
            values["syncStamp"] = row.stamp
            db.update(table.name, values, "rowid = ?", arrayOf(rowid), conflict = "IGNORE")
            return Outcome.APPLIED
        }

        values["syncId"] = row.syncId
        values["syncStamp"] = row.stamp
        // Derived columns (currentBalance) have no default; the recompute after
        // this file is applied fills in the real value.
        for (col in table.derived) values[col] = 0L
        // Merchant rules are keyed by name, so the same merchant from another
        // device replaces ours instead of failing on the primary key.
        val conflict = if (table.localPk) "IGNORE" else "REPLACE"
        val rowid = db.insert(table.name, values, conflict)
        if (rowid == -1L) return Outcome.SKIPPED
        if (table.localPk) {
            toLocal[table.name]?.put(row.syncId, rowid)
            toSync[table.name]?.put(rowid, row.syncId)
        }
        return Outcome.APPLIED
    }

    /** Returns true if a local row was deleted. */
    private suspend fun applyTombstone(db: Sql, table: Table, tomb: Tomb): Boolean {
        val local = db.first("SELECT rowid, syncStamp FROM `${table.name}` WHERE syncId = ?", tomb.syncId)
            ?.let { it.getLong(0) to it.getLong(1) }

        var deleted = false
        if (local != null) {
            val (rowid, localStamp) = local
            if (localStamp > tomb.stamp) return false // edited here after the delete
            try {
                db.exec("DELETE FROM `${table.name}` WHERE rowid = ?", rowid)
                deleted = true
            } catch (e: Exception) {
                if (!e.isConstraintFailure()) throw e
                // Something here still points at it, e.g. a transaction added on this
                // device to an account the other device deleted. Keep the row and send
                // it back out with a newer stamp so the other device restores it.
                val stamp = SyncSchema.tick(db)
                db.exec("UPDATE `${table.name}` SET syncStamp = ? WHERE rowid = ?", stamp, rowid)
                db.exec(
                    "INSERT OR REPLACE INTO ${SyncSchema.OUTBOX} (tableName, syncId, stamp) VALUES (?, ?, ?)",
                    table.name, tomb.syncId, stamp
                )
                return false
            }
        }
        // Remember the delete so a full export carries it to devices that join later.
        // (No upsert syntax: Android 7 to 10 ship SQLite older than 3.24.)
        val known = tombstoneStamp(db, table.name, tomb.syncId)
        if (known == null || known < tomb.stamp) {
            db.exec(
                "INSERT OR REPLACE INTO ${SyncSchema.TOMBSTONES} (tableName, syncId, stamp) VALUES (?, ?, ?)",
                table.name, tomb.syncId, tomb.stamp
            )
        }
        return deleted
    }

    /** SQLite reports a foreign key or other constraint failure. */
    private fun Exception.isConstraintFailure(): Boolean =
        generateSequence<Throwable>(this) { it.cause }.any {
            it::class.simpleName == "SQLiteConstraintException" ||
                it.message.orEmpty().contains("constraint", ignoreCase = true)
        }

    private suspend fun tombstoneStamp(db: Sql, table: String, syncId: String): Long? =
        db.long("SELECT stamp FROM ${SyncSchema.TOMBSTONES} WHERE tableName = ? AND syncId = ?", table, syncId)

    private val requiredCache = mutableMapOf<String, Boolean>()

    private suspend fun isRequired(db: Sql, table: String, col: String): Boolean {
        requiredCache["$table.$col"]?.let { return it }
        val required = db.rows("PRAGMA table_info(`$table`)").any {
            it.getString(it.index("name")) == col && it.getInt(it.index("notnull")) == 1
        }
        requiredCache["$table.$col"] = required
        return required
    }

    /** Local values in sync-file form, for the tie-break when two stamps are equal. */
    private suspend fun localValues(
        db: Sql,
        table: Table,
        rowid: Long,
        cols: Set<String>,
        toSync: Map<String, MutableMap<Long, String>>
    ): Map<String, Any?> {
        val ordered = cols.toList()
        val out = mutableMapOf<String, Any?>()
        val r = db.first(
            "SELECT ${ordered.joinToString(", ") { "`$it`" }} FROM `${table.name}` WHERE rowid = ?",
            rowid
        ) ?: return out
        ordered.forEachIndexed { i, col ->
            val parent = table.fks[col]
            val v = r.get(i)
            out[col] = when {
                v == null -> null
                parent != null -> toSync[parent]?.get(r.getLong(i))
                v is ByteArray -> v.decodeToString()
                else -> v
            }
        }
        return out
    }

    /** Same order and formatting on every device, so ties resolve the same way everywhere. */
    private fun canonical(values: Map<String, Any?>): String =
        values.entries.sortedBy { it.key }.joinToString("\u0001") { (k, v) -> "$k=${v ?: "\u0000"}" }

    private suspend fun syncToLocalIds(db: Sql, table: String): MutableMap<String, Long> =
        db.rows("SELECT syncId, id FROM `$table` WHERE syncId IS NOT NULL")
            .associateTo(mutableMapOf()) { it.getString(0) to it.getLong(1) }

    private suspend fun localToSyncIds(db: Sql, table: String): MutableMap<Long, String> =
        db.rows("SELECT id, syncId FROM `$table` WHERE syncId IS NOT NULL")
            .associateTo(mutableMapOf()) { it.getLong(0) to it.getString(1) }

    /** A value from a sync file as SQLite takes it. Booleans are 0 or 1. */
    private fun sqlValue(value: Any?): Any? = when (value) {
        null, is Long, is Double -> value
        is Boolean -> if (value) 1L else 0L
        else -> value.toString()
    }

    // --- Parsing ---

    private class File(
        val header: Header,
        val rows: Map<String, List<Row>>,
        val deleted: List<Tomb>
    )

    private const val MAX_PAYLOAD_BYTES = 256L * 1024 * 1024

    private val json = Json { isLenient = false }

    private suspend fun parse(gzipJson: ByteArray): File {
        val text = try {
            Gzip.decompress(gzipJson)
        } catch (e: Exception) {
            throw BackupCrypto.NotABackupException("Sync file is damaged")
        }
        if (text.size > MAX_PAYLOAD_BYTES) throw IOException("Sync file is too large")
        val root = try {
            json.parseToJsonElement(text.decodeToString()).jsonObject
        } catch (e: SerializationException) {
            throw BackupCrypto.NotABackupException("Sync file is damaged")
        } catch (e: IllegalArgumentException) {
            throw BackupCrypto.NotABackupException("Sync file is damaged")
        }
        val file = try {
            val format = (root["format"]?.toScalar() as? Long)?.toInt() ?: 0
            val dbVersion = (root["dbVersion"]?.toScalar() as? Long)?.toInt() ?: 0
            val deviceId = root["deviceId"]?.toScalar() as? String ?: ""
            val deviceName = root["deviceName"]?.toScalar() as? String ?: ""
            val seq = root["seq"]?.toScalar() as? Long ?: 0L
            val full = root["full"]?.toScalar() as? Boolean ?: false
            val createdAt = root["createdAt"]?.toScalar() as? Long ?: 0L
            val rows = linkedMapOf<String, List<Row>>()
            (root["rows"] as? JsonObject)?.forEach { (table, list) -> rows[table] = readRows(list.jsonArray) }
            val deleted = (root["deleted"] as? JsonArray).orEmpty().map { readTomb(it.jsonObject) }
            if (format == 0 || deviceId.isEmpty()) throw BackupCrypto.NotABackupException("Sync file is damaged")
            if (format > FORMAT || dbVersion > AppDatabase.SCHEMA_VERSION) throw NewerVersionException()
            File(Header(deviceId, deviceName, seq, full, createdAt), rows, deleted)
        } catch (e: IOException) {
            throw e
        } catch (e: RuntimeException) {
            throw BackupCrypto.NotABackupException("Sync file is damaged")
        }
        return file
    }

    /** Another device runs a newer Paisa. Its changes wait until this one updates. */
    class NewerVersionException : IOException("Another device runs a newer Paisa. Update this one to keep syncing.")

    private fun readRows(list: JsonArray): List<Row> = list.mapNotNull { el ->
        val o = el.jsonObject
        val id = (o["id"] as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()
        val stamp = o["s"]?.toScalar() as? Long ?: 0L
        val values = linkedMapOf<String, Any?>()
        (o["v"] as? JsonObject)?.forEach { (k, v) -> values[k] = if (v is JsonPrimitive || v is kotlinx.serialization.json.JsonNull) v.toScalar() else null }
        if (id.isNotEmpty()) Row(id, stamp, values) else null
    }

    private fun readTomb(o: JsonObject): Tomb = Tomb(
        table = o["t"]?.toScalar() as? String ?: "",
        syncId = o["id"]?.toScalar() as? String ?: "",
        stamp = o["s"]?.toScalar() as? Long ?: 0L
    )
}

private typealias Table = SyncSchema.Table
