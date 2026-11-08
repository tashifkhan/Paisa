package codes.tashif.paisa.sync

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import androidx.sqlite.db.SupportSQLiteDatabase
import codes.tashif.paisa.backup.BackupCrypto
import codes.tashif.paisa.data.AppDatabase
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

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
    fun export(db: SupportSQLiteDatabase, header: Header): Export {
        val outbox = readOutbox(db)
        val queued = outbox.groupBy { it.table }.mapValues { (_, v) -> v.map { it.syncId }.toSet() }
        val parentIds = SyncSchema.TABLES.filter { it.localPk }.associate { it.name to localToSyncIds(db, it.name) }
        var changes = 0

        val bytes = ByteArrayOutputStream()
        JsonWriter(OutputStreamWriter(GZIPOutputStream(bytes), Charsets.UTF_8)).use { w ->
            w.beginObject()
            w.name("format").value(FORMAT.toLong())
            w.name("dbVersion").value(db.version.toLong())
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
                db.query("SELECT syncId, syncStamp, $select FROM `${table.name}` WHERE syncId IS NOT NULL")
                    .use { c ->
                        while (c.moveToNext()) {
                            val syncId = c.getString(0)
                            if (!header.full && syncId !in wanted) continue
                            w.beginObject()
                            w.name("id").value(syncId)
                            w.name("s").value(c.getLong(1))
                            w.name("v").beginObject()
                            cols.forEachIndexed { i, col ->
                                w.name(col)
                                val parent = table.fks[col]
                                if (parent != null && !c.isNull(i + 2)) {
                                    val ref = parentIds[parent]?.get(c.getLong(i + 2))
                                    if (ref == null) w.nullValue() else w.value(ref)
                                } else {
                                    writeValue(w, c, i + 2)
                                }
                            }
                            w.endObject()
                            w.endObject()
                            changes++
                        }
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
            db.query(tombQuery).use { c ->
                while (c.moveToNext()) {
                    w.beginObject()
                    w.name("t").value(c.getString(0))
                    w.name("id").value(c.getString(1))
                    w.name("s").value(c.getLong(2))
                    w.endObject()
                    changes++
                }
            }
            w.endArray()
            w.endObject()
        }
        return Export(bytes.toByteArray(), changes, outbox)
    }

    fun hasPendingChanges(db: SupportSQLiteDatabase): Boolean =
        db.query("SELECT EXISTS (SELECT 1 FROM ${SyncSchema.OUTBOX})").use { c ->
            c.moveToFirst() && c.getInt(0) == 1
        }

    /** Drops uploaded entries. Rows changed again since keep their newer entry. */
    fun clearOutbox(db: SupportSQLiteDatabase, entries: List<OutboxEntry>) {
        for (e in entries) {
            db.execSQL(
                "DELETE FROM ${SyncSchema.OUTBOX} WHERE tableName = ? AND syncId = ? AND stamp <= ?",
                arrayOf<Any>(e.table, e.syncId, e.stamp)
            )
        }
    }

    /** After a restore the whole database is new to the cloud. */
    fun clearAllOutbox(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM ${SyncSchema.OUTBOX}")
    }

    private fun readOutbox(db: SupportSQLiteDatabase): List<OutboxEntry> {
        val out = mutableListOf<OutboxEntry>()
        db.query("SELECT tableName, syncId, stamp FROM ${SyncSchema.OUTBOX}").use { c ->
            while (c.moveToNext()) out += OutboxEntry(c.getString(0), c.getString(1), c.getLong(2))
        }
        return out
    }

    private fun writeValue(w: JsonWriter, c: Cursor, i: Int) {
        when (c.getType(i)) {
            Cursor.FIELD_TYPE_NULL -> w.nullValue()
            Cursor.FIELD_TYPE_INTEGER -> w.value(c.getLong(i))
            Cursor.FIELD_TYPE_FLOAT -> w.value(c.getDouble(i))
            else -> w.value(c.getString(i))
        }
    }

    // --- Apply ---

    /** Reads only the header fields, to decide whether and in what order to apply. */
    fun readHeader(gzipJson: ByteArray): Header = parse(gzipJson).header

    /**
     * Merges a change file from another device. Runs in one transaction; a bad
     * file changes nothing. Local edits made while this runs wait for it.
     */
    fun apply(db: SupportSQLiteDatabase, gzipJson: ByteArray): ApplyResult {
        val file = parse(gzipJson)
        var applied = 0
        var skipped = 0
        db.beginTransaction()
        try {
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
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return ApplyResult(applied, skipped, file.header)
    }

    private enum class Outcome { APPLIED, UNCHANGED, SKIPPED }

    private fun applyRow(
        db: SupportSQLiteDatabase,
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
            db.execSQL(
                "DELETE FROM ${SyncSchema.TOMBSTONES} WHERE tableName = ? AND syncId = ?",
                arrayOf<Any>(table.name, row.syncId)
            )
        }

        val values = ContentValues()
        for ((col, value) in row.values) {
            if (col !in localCols) continue
            val parent = table.fks[col]
            if (parent != null && value != null) {
                val localId = toLocal[parent]?.get(value.toString())
                if (localId == null) {
                    // The parent was deleted here or never arrived. Required links
                    // can't be satisfied, so leave the row out.
                    if (isRequired(db, table.name, col)) return Outcome.SKIPPED
                    values.putNull(col)
                } else {
                    values.put(col, localId)
                }
            } else {
                putValue(values, col, value)
            }
        }

        val local = db.query(
            "SELECT rowid, syncStamp FROM `${table.name}` WHERE syncId = ?",
            arrayOf<Any>(row.syncId)
        ).use { c -> if (c.moveToFirst()) c.getLong(0) to c.getLong(1) else null }

        if (local != null) {
            val (rowid, localStamp) = local
            val wins = row.stamp > localStamp ||
                (row.stamp == localStamp && canonical(row.values) > canonical(localValues(db, table, rowid, localCols, toSync)))
            if (!wins) return Outcome.UNCHANGED
            values.put("syncStamp", row.stamp)
            db.update(table.name, SQLiteDatabase.CONFLICT_IGNORE, values, "rowid = ?", arrayOf<Any>(rowid))
            return Outcome.APPLIED
        }

        values.put("syncId", row.syncId)
        values.put("syncStamp", row.stamp)
        // Derived columns (currentBalance) have no default; the recompute after
        // this file is applied fills in the real value.
        for (col in table.derived) values.put(col, 0)
        // Merchant rules are keyed by name, so the same merchant from another
        // device replaces ours instead of failing on the primary key.
        val conflict = if (table.localPk) SQLiteDatabase.CONFLICT_IGNORE else SQLiteDatabase.CONFLICT_REPLACE
        val rowid = db.insert(table.name, conflict, values)
        if (rowid == -1L) return Outcome.SKIPPED
        if (table.localPk) {
            toLocal[table.name]?.put(row.syncId, rowid)
            toSync[table.name]?.put(rowid, row.syncId)
        }
        return Outcome.APPLIED
    }

    /** Returns true if a local row was deleted. */
    private fun applyTombstone(db: SupportSQLiteDatabase, table: Table, tomb: Tomb): Boolean {
        val local = db.query(
            "SELECT rowid, syncStamp FROM `${table.name}` WHERE syncId = ?",
            arrayOf<Any>(tomb.syncId)
        ).use { c -> if (c.moveToFirst()) c.getLong(0) to c.getLong(1) else null }

        var deleted = false
        if (local != null) {
            val (rowid, localStamp) = local
            if (localStamp > tomb.stamp) return false // edited here after the delete
            try {
                db.delete(table.name, "rowid = ?", arrayOf<Any>(rowid))
                deleted = true
            } catch (e: SQLiteConstraintException) {
                // Something here still points at it, e.g. a transaction added on this
                // device to an account the other device deleted. Keep the row and send
                // it back out with a newer stamp so the other device restores it.
                val stamp = SyncSchema.tick(db)
                db.execSQL(
                    "UPDATE `${table.name}` SET syncStamp = ? WHERE rowid = ?",
                    arrayOf<Any>(stamp, rowid)
                )
                db.execSQL(
                    "INSERT OR REPLACE INTO ${SyncSchema.OUTBOX} (tableName, syncId, stamp) VALUES (?, ?, ?)",
                    arrayOf<Any>(table.name, tomb.syncId, stamp)
                )
                return false
            }
        }
        // Remember the delete so a full export carries it to devices that join later.
        // (No upsert syntax: Android 7 to 10 ship SQLite older than 3.24.)
        val known = tombstoneStamp(db, table.name, tomb.syncId)
        if (known == null || known < tomb.stamp) {
            db.execSQL(
                "INSERT OR REPLACE INTO ${SyncSchema.TOMBSTONES} (tableName, syncId, stamp) VALUES (?, ?, ?)",
                arrayOf<Any>(table.name, tomb.syncId, tomb.stamp)
            )
        }
        return deleted
    }

    private fun tombstoneStamp(db: SupportSQLiteDatabase, table: String, syncId: String): Long? =
        db.query(
            "SELECT stamp FROM ${SyncSchema.TOMBSTONES} WHERE tableName = ? AND syncId = ?",
            arrayOf<Any>(table, syncId)
        ).use { c -> if (c.moveToFirst()) c.getLong(0) else null }

    private val requiredCache = mutableMapOf<String, Boolean>()

    private fun isRequired(db: SupportSQLiteDatabase, table: String, col: String): Boolean =
        requiredCache.getOrPut("$table.$col") {
            db.query("PRAGMA table_info(`$table`)").use { c ->
                val name = c.getColumnIndexOrThrow("name")
                val notNull = c.getColumnIndexOrThrow("notnull")
                while (c.moveToNext()) {
                    if (c.getString(name) == col) return@use c.getInt(notNull) == 1
                }
                false
            }
        }

    /** Local values in sync-file form, for the tie-break when two stamps are equal. */
    private fun localValues(
        db: SupportSQLiteDatabase,
        table: Table,
        rowid: Long,
        cols: Set<String>,
        toSync: Map<String, MutableMap<Long, String>>
    ): Map<String, Any?> {
        val ordered = cols.toList()
        val out = mutableMapOf<String, Any?>()
        db.query(
            "SELECT ${ordered.joinToString(", ") { "`$it`" }} FROM `${table.name}` WHERE rowid = ?",
            arrayOf<Any>(rowid)
        ).use { c ->
            if (!c.moveToFirst()) return out
            ordered.forEachIndexed { i, col ->
                val parent = table.fks[col]
                out[col] = when {
                    c.isNull(i) -> null
                    parent != null -> toSync[parent]?.get(c.getLong(i))
                    c.getType(i) == Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                    c.getType(i) == Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                    else -> c.getString(i)
                }
            }
        }
        return out
    }

    /** Same order and formatting on every device, so ties resolve the same way everywhere. */
    private fun canonical(values: Map<String, Any?>): String =
        values.toSortedMap().entries.joinToString("\u0001") { (k, v) -> "$k=${v ?: "\u0000"}" }

    private fun syncToLocalIds(db: SupportSQLiteDatabase, table: String): MutableMap<String, Long> {
        val out = mutableMapOf<String, Long>()
        db.query("SELECT syncId, id FROM `$table` WHERE syncId IS NOT NULL").use { c ->
            while (c.moveToNext()) out[c.getString(0)] = c.getLong(1)
        }
        return out
    }

    private fun localToSyncIds(db: SupportSQLiteDatabase, table: String): MutableMap<Long, String> {
        val out = mutableMapOf<Long, String>()
        db.query("SELECT id, syncId FROM `$table` WHERE syncId IS NOT NULL").use { c ->
            while (c.moveToNext()) out[c.getLong(0)] = c.getString(1)
        }
        return out
    }

    private fun putValue(cv: ContentValues, key: String, value: Any?) {
        when (value) {
            null -> cv.putNull(key)
            is Long -> cv.put(key, value)
            is Double -> cv.put(key, value)
            is Boolean -> cv.put(key, if (value) 1 else 0)
            else -> cv.put(key, value.toString())
        }
    }

    // --- Parsing ---

    private class File(
        val header: Header,
        val rows: Map<String, List<Row>>,
        val deleted: List<Tomb>
    )

    private const val MAX_PAYLOAD_BYTES = 256L * 1024 * 1024

    private fun parse(gzipJson: ByteArray): File {
        var format = 0
        var dbVersion = 0
        var deviceId = ""
        var deviceName = ""
        var seq = 0L
        var full = false
        var createdAt = 0L
        val rows = linkedMapOf<String, List<Row>>()
        val deleted = mutableListOf<Tomb>()
        try {
            val input = LimitedInputStream(GZIPInputStream(gzipJson.inputStream()), MAX_PAYLOAD_BYTES)
            JsonReader(InputStreamReader(input, Charsets.UTF_8)).use { r ->
                r.beginObject()
                while (r.hasNext()) {
                    when (r.nextName()) {
                        "format" -> format = r.nextInt()
                        "dbVersion" -> dbVersion = r.nextInt()
                        "deviceId" -> deviceId = r.nextString()
                        "deviceName" -> deviceName = r.nextString()
                        "seq" -> seq = r.nextLong()
                        "full" -> full = r.nextBoolean()
                        "createdAt" -> createdAt = r.nextLong()
                        "rows" -> {
                            r.beginObject()
                            while (r.hasNext()) rows[r.nextName()] = readRows(r)
                            r.endObject()
                        }
                        "deleted" -> {
                            r.beginArray()
                            while (r.hasNext()) deleted += readTomb(r)
                            r.endArray()
                        }
                        else -> r.skipValue()
                    }
                }
                r.endObject()
            }
        } catch (e: IOException) {
            throw e
        } catch (e: RuntimeException) {
            throw BackupCrypto.NotABackupException("Sync file is damaged")
        }
        if (format == 0 || deviceId.isEmpty()) throw BackupCrypto.NotABackupException("Sync file is damaged")
        if (format > FORMAT || dbVersion > AppDatabase.SCHEMA_VERSION) {
            throw NewerVersionException()
        }
        return File(Header(deviceId, deviceName, seq, full, createdAt), rows, deleted)
    }

    /** Another device runs a newer Paisa. Its changes wait until this one updates. */
    class NewerVersionException : IOException("Another device runs a newer Paisa. Update this one to keep syncing.")

    private fun readRows(r: JsonReader): List<Row> {
        val out = mutableListOf<Row>()
        r.beginArray()
        while (r.hasNext()) {
            var id = ""
            var stamp = 0L
            val values = linkedMapOf<String, Any?>()
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "id" -> id = r.nextString()
                    "s" -> stamp = r.nextLong()
                    "v" -> {
                        r.beginObject()
                        while (r.hasNext()) values[r.nextName()] = readScalar(r)
                        r.endObject()
                    }
                    else -> r.skipValue()
                }
            }
            r.endObject()
            if (id.isNotEmpty()) out += Row(id, stamp, values)
        }
        r.endArray()
        return out
    }

    private fun readTomb(r: JsonReader): Tomb {
        var table = ""
        var id = ""
        var stamp = 0L
        r.beginObject()
        while (r.hasNext()) {
            when (r.nextName()) {
                "t" -> table = r.nextString()
                "id" -> id = r.nextString()
                "s" -> stamp = r.nextLong()
                else -> r.skipValue()
            }
        }
        r.endObject()
        return Tomb(table, id, stamp)
    }

    private fun readScalar(r: JsonReader): Any? = when (r.peek()) {
        JsonToken.NULL -> { r.nextNull(); null }
        JsonToken.BOOLEAN -> r.nextBoolean()
        JsonToken.NUMBER -> {
            val raw = r.nextString()
            raw.toLongOrNull() ?: raw.toDouble()
        }
        JsonToken.STRING -> r.nextString()
        else -> { r.skipValue(); null }
    }

    private class LimitedInputStream(inner: java.io.InputStream, private val limit: Long) :
        java.io.FilterInputStream(inner) {
        private var count = 0L

        override fun read(): Int {
            val b = super.read()
            if (b >= 0) bump(1)
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = super.read(b, off, len)
            if (n > 0) bump(n.toLong())
            return n
        }

        private fun bump(n: Long) {
            count += n
            if (count > limit) throw IOException("Sync file is too large")
        }
    }
}

private typealias Table = SyncSchema.Table
