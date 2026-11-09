package codes.tashif.paisa.backup

import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.sql.JsonOut
import codes.tashif.paisa.data.sql.Sql
import codes.tashif.paisa.platform.nowMillis
import codes.tashif.paisa.data.sql.toScalar
import codes.tashif.paisa.shared.Gzip
import codes.tashif.paisa.sync.SyncEngine
import codes.tashif.paisa.sync.SyncSchema
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

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
        "debt_people",
        "debt_entries",
        SyncSchema.TOMBSTONES
    )

    private val INTERNAL_TABLES = setOf("android_metadata", "sqlite_sequence", "room_master_table")

    /** Hard cap on the decompressed payload, so a crafted file can't exhaust memory. */
    private const val MAX_PAYLOAD_BYTES = 256L * 1024 * 1024

    /** One table row: column name to Long, Double, String, ByteArray or null. */
    typealias Values = Map<String, Any?>

    data class Snapshot(
        val dbVersion: Int,
        val createdAt: Long,
        val appVersion: String,
        val tables: Map<String, List<Values>>
    ) {
        val transactionCount: Int get() = tables["transactions"]?.size ?: 0
    }

    suspend fun userTables(db: Sql): List<String> {
        val names = db.rows("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'")
            .map { it.getString(0) }
            .filter { it !in INTERNAL_TABLES && it !in SyncSchema.LOCAL_ONLY_TABLES && !it.startsWith("room_") }
        return names.sortedBy { TABLE_ORDER.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }

    /**
     * Dumps the live database into gzip'd JSON. Call inside a transaction for a
     * consistent read. [appVersion] is the app's version name.
     */
    suspend fun export(db: Sql, appVersion: String): ByteArray {
        val w = JsonOut()
        w.beginObject()
        w.name("format").value(FORMAT.toLong())
        w.name("dbVersion").value(db.userVersion().toLong())
        w.name("createdAt").value(nowMillis())
        w.name("appVersion").value(appVersion)
        w.name("tables").beginObject()
        for (table in userTables(db)) {
            w.name(table).beginArray()
            for (row in db.rows("SELECT * FROM `$table`")) {
                w.beginObject()
                row.columns.forEachIndexed { i, col -> w.name(col).sqlValue(row.get(i)) }
                w.endObject()
            }
            w.endArray()
        }
        w.endObject()
        w.endObject()
        return Gzip.compress(w.toString().encodeToByteArray())
    }

    private val json = Json { isLenient = false }

    suspend fun parse(gzipJson: ByteArray): Snapshot {
        val text = try {
            Gzip.decompress(gzipJson)
        } catch (e: Exception) {
            throw BackupCrypto.NotABackupException("Backup file is damaged")
        }
        if (text.size > MAX_PAYLOAD_BYTES) throw IOException("Backup is too large")
        val root = try {
            json.parseToJsonElement(text.decodeToString()).jsonObject
        } catch (e: SerializationException) {
            throw BackupCrypto.NotABackupException("Backup file is damaged")
        } catch (e: IllegalArgumentException) {
            throw BackupCrypto.NotABackupException("Backup file is damaged")
        }
        val format: Int
        val dbVersion: Int
        val snapshot = try {
            format = (root["format"]?.toScalar() as? Long)?.toInt() ?: 0
            dbVersion = (root["dbVersion"]?.toScalar() as? Long)?.toInt() ?: 0
            val createdAt = root["createdAt"]?.toScalar() as? Long ?: 0L
            val appVersion = root["appVersion"]?.toScalar() as? String ?: ""
            val tables = linkedMapOf<String, List<Values>>()
            (root["tables"] as? JsonObject)?.forEach { (name, rows) ->
                tables[name] = (rows as? JsonArray).orEmpty().map { row ->
                    // Booleans from other writers become 0 or 1, as SQLite stores them.
                    row.jsonObject.mapValues { (_, v) ->
                        when (val x = v.toScalar()) {
                            is Boolean -> if (x) 1L else 0L
                            else -> x
                        }
                    }
                }
            }
            Snapshot(dbVersion, createdAt, appVersion, tables)
        } catch (e: RuntimeException) {
            throw BackupCrypto.NotABackupException("Backup file is damaged")
        }
        if (format == 0 || dbVersion == 0) {
            throw BackupCrypto.NotABackupException("Backup file is damaged")
        }
        if (format > FORMAT || dbVersion > AppDatabase.SCHEMA_VERSION) {
            throw BackupCrypto.NotABackupException("This backup needs a newer version of Paisa")
        }
        return snapshot
    }

    /**
     * Replaces every table with the snapshot's rows. Must run inside a transaction;
     * any exception rolls the whole restore back.
     *
     * Columns the current schema no longer has are dropped, and columns added
     * since the backup was made take their SQL defaults.
     */
    suspend fun restore(db: Sql, snapshot: Snapshot) {
        val live = userTables(db)
        // Keep each row's sync id and stamp as backed up. With the triggers on,
        // the deletes below would turn into tombstones and the inserts into
        // brand-new edits that overwrite newer changes on other devices.
        SyncSchema.setApplying(db, true)
        db.exec("PRAGMA defer_foreign_keys = ON")
        for (table in live.asReversed()) db.exec("DELETE FROM `$table`")
        for (table in live) {
            val rows = snapshot.tables[table] ?: continue
            val columns = SyncSchema.columnsOf(db, table).toSet()
            for (row in rows) {
                val values = row.filterKeys { it in columns }
                if (values.isEmpty()) continue
                db.insert(table, values)
            }
        }
        val broken = db.first("PRAGMA foreign_key_check")
        if (broken != null) {
            throw BackupCrypto.NotABackupException("Backup has broken references (${broken.getString(0)})")
        }
        if (SyncSchema.isInstalled(db)) {
            // Backups from before sync have no sync ids or balance anchors.
            SyncSchema.backfillIds(db, stamp = 1)
            if (snapshot.dbVersion < 10) SyncSchema.deriveAnchors(db) else SyncSchema.recomputeBalances(db)
            SyncEngine.clearAllOutbox(db)
        }
        SyncSchema.setApplying(db, false)
    }
}
