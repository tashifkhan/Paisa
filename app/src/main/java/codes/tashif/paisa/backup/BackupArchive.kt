package codes.tashif.paisa.backup

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import androidx.sqlite.db.SupportSQLiteDatabase
import codes.tashif.paisa.BuildConfig
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.sync.SyncEngine
import codes.tashif.paisa.sync.SyncSchema
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Logical snapshot of every Room table, written as gzip'd JSON.
 *
 * Tables are discovered from `sqlite_master`, so a new entity is backed up
 * without touching this file. Add it to [TABLE_ORDER] if it has foreign keys,
 * so restore inserts parents before children.
 */
object BackupArchive {

    const val FORMAT = 1

    /** Parents first. Restore inserts in this order and deletes in reverse. */
    private val TABLE_ORDER = listOf(
        "accounts",
        "categories",
        "transactions",
        "budgets",
        "savings_goals",
        "recurring_transactions",
        "settings",
        "unrecognized_sms",
        "merchant_mappings",
        SyncSchema.TOMBSTONES
    )

    private val INTERNAL_TABLES = setOf("android_metadata", "sqlite_sequence", "room_master_table")

    /** Hard cap on the decompressed payload, so a crafted file can't exhaust memory. */
    private const val MAX_PAYLOAD_BYTES = 256L * 1024 * 1024

    data class Snapshot(
        val dbVersion: Int,
        val createdAt: Long,
        val appVersion: String,
        val tables: Map<String, List<ContentValues>>
    ) {
        val transactionCount: Int get() = tables["transactions"]?.size ?: 0
    }

    fun userTables(db: SupportSQLiteDatabase): List<String> {
        val names = mutableListOf<String>()
        db.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'"
        ).use { c ->
            while (c.moveToNext()) {
                val name = c.getString(0)
                if (name !in INTERNAL_TABLES && name !in SyncSchema.LOCAL_ONLY_TABLES) names += name
            }
        }
        return names.sortedBy { TABLE_ORDER.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }

    /** Dumps the live database into gzip'd JSON. Call inside a transaction for a consistent read. */
    fun export(db: SupportSQLiteDatabase): ByteArray {
        val bytes = ByteArrayOutputStream()
        JsonWriter(OutputStreamWriter(GZIPOutputStream(bytes), Charsets.UTF_8)).use { w ->
            w.beginObject()
            w.name("format").value(FORMAT.toLong())
            w.name("dbVersion").value(db.version.toLong())
            w.name("createdAt").value(System.currentTimeMillis())
            w.name("appVersion").value(BuildConfig.VERSION_NAME)
            w.name("tables").beginObject()
            for (table in userTables(db)) {
                w.name(table).beginArray()
                db.query("SELECT * FROM `$table`").use { c -> writeRows(w, c) }
                w.endArray()
            }
            w.endObject()
            w.endObject()
        }
        return bytes.toByteArray()
    }

    private fun writeRows(w: JsonWriter, c: Cursor) {
        val cols = c.columnNames
        while (c.moveToNext()) {
            w.beginObject()
            for (i in cols.indices) {
                w.name(cols[i])
                when (c.getType(i)) {
                    Cursor.FIELD_TYPE_NULL -> w.nullValue()
                    Cursor.FIELD_TYPE_INTEGER -> w.value(c.getLong(i))
                    Cursor.FIELD_TYPE_FLOAT -> w.value(c.getDouble(i))
                    Cursor.FIELD_TYPE_BLOB ->
                        w.beginObject().name("b64")
                            .value(Base64.encodeToString(c.getBlob(i), Base64.NO_WRAP))
                            .endObject()
                    else -> w.value(c.getString(i))
                }
            }
            w.endObject()
        }
    }

    fun parse(gzipJson: ByteArray): Snapshot {
        var dbVersion = 0
        var createdAt = 0L
        var appVersion = ""
        var format = 0
        val tables = linkedMapOf<String, List<ContentValues>>()
        val stream = LimitedInputStream(GZIPInputStream(gzipJson.inputStream()), MAX_PAYLOAD_BYTES)
        try {
            JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { r ->
                r.beginObject()
                while (r.hasNext()) {
                    when (r.nextName()) {
                        "format" -> format = r.nextInt()
                        "dbVersion" -> dbVersion = r.nextInt()
                        "createdAt" -> createdAt = r.nextLong()
                        "appVersion" -> appVersion = r.nextString()
                        "tables" -> {
                            r.beginObject()
                            while (r.hasNext()) {
                                val name = r.nextName()
                                tables[name] = readRows(r)
                            }
                            r.endObject()
                        }
                        else -> r.skipValue()
                    }
                }
                r.endObject()
            }
        } catch (e: IOException) {
            throw e
        } catch (e: RuntimeException) {
            // JsonReader throws IllegalStateException/NumberFormatException on bad input.
            throw BackupCrypto.NotABackupException("Backup file is damaged")
        }
        if (format == 0 || dbVersion == 0) {
            throw BackupCrypto.NotABackupException("Backup file is damaged")
        }
        if (format > FORMAT || dbVersion > AppDatabase.SCHEMA_VERSION) {
            throw BackupCrypto.NotABackupException("This backup needs a newer version of Paisa")
        }
        return Snapshot(dbVersion, createdAt, appVersion, tables)
    }

    private fun readRows(r: JsonReader): List<ContentValues> {
        val rows = mutableListOf<ContentValues>()
        r.beginArray()
        while (r.hasNext()) {
            val cv = ContentValues()
            r.beginObject()
            while (r.hasNext()) {
                val col = r.nextName()
                when (r.peek()) {
                    JsonToken.NULL -> { r.nextNull(); cv.putNull(col) }
                    JsonToken.BOOLEAN -> cv.put(col, if (r.nextBoolean()) 1 else 0)
                    JsonToken.STRING -> cv.put(col, r.nextString())
                    JsonToken.NUMBER -> {
                        val raw = r.nextString()
                        val asLong = raw.toLongOrNull()
                        if (asLong != null) cv.put(col, asLong) else cv.put(col, raw.toDouble())
                    }
                    JsonToken.BEGIN_OBJECT -> {
                        r.beginObject()
                        var blob: ByteArray? = null
                        while (r.hasNext()) {
                            if (r.nextName() == "b64") blob = Base64.decode(r.nextString(), Base64.NO_WRAP)
                            else r.skipValue()
                        }
                        r.endObject()
                        if (blob != null) cv.put(col, blob) else cv.putNull(col)
                    }
                    else -> r.skipValue()
                }
            }
            r.endObject()
            rows += cv
        }
        r.endArray()
        return rows
    }

    /**
     * Replaces every table with the snapshot's rows. Must run inside a transaction;
     * any exception rolls the whole restore back.
     *
     * Columns the current schema no longer has are dropped, and columns added
     * since the backup was made take their SQL defaults.
     */
    fun restore(db: SupportSQLiteDatabase, snapshot: Snapshot) {
        val live = userTables(db)
        // Keep each row's sync id and stamp as backed up. With the triggers on,
        // the deletes below would turn into tombstones and the inserts into
        // brand-new edits that overwrite newer changes on other devices.
        SyncSchema.setApplying(db, true)
        db.execSQL("PRAGMA defer_foreign_keys = ON")
        for (table in live.asReversed()) db.execSQL("DELETE FROM `$table`")
        for (table in live) {
            val rows = snapshot.tables[table] ?: continue
            val columns = columnsOf(db, table)
            for (row in rows) {
                val cv = ContentValues()
                for (key in row.keySet()) {
                    if (key in columns) putValue(cv, key, row.get(key))
                }
                if (cv.size() == 0) continue
                db.insert(table, SQLiteDatabase.CONFLICT_ABORT, cv)
            }
        }
        db.query("PRAGMA foreign_key_check").use { c ->
            if (c.moveToFirst()) {
                throw BackupCrypto.NotABackupException("Backup has broken references (${c.getString(0)})")
            }
        }
        if (SyncSchema.isInstalled(db)) {
            // Backups from before sync have no sync ids or balance anchors.
            SyncSchema.backfillIds(db, stamp = 1)
            if (snapshot.dbVersion < 10) SyncSchema.deriveAnchors(db) else SyncSchema.recomputeBalances(db)
            SyncEngine.clearAllOutbox(db)
        }
        SyncSchema.setApplying(db, false)
    }

    private fun putValue(cv: ContentValues, key: String, value: Any?) {
        when (value) {
            null -> cv.putNull(key)
            is Long -> cv.put(key, value)
            is Int -> cv.put(key, value)
            is Double -> cv.put(key, value)
            is String -> cv.put(key, value)
            is ByteArray -> cv.put(key, value)
            else -> cv.put(key, value.toString())
        }
    }

    private fun columnsOf(db: SupportSQLiteDatabase, table: String): Set<String> {
        val cols = mutableSetOf<String>()
        db.query("PRAGMA table_info(`$table`)").use { c ->
            val nameIdx = c.getColumnIndexOrThrow("name")
            while (c.moveToNext()) cols += c.getString(nameIdx)
        }
        return cols
    }

    private class LimitedInputStream(
        private val inner: java.io.InputStream,
        private val limit: Long
    ) : java.io.FilterInputStream(inner) {
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
            if (count > limit) throw IOException("Backup is too large")
        }
    }
}
