package codes.tashif.paisa.data.sql

import androidx.room3.PooledConnection
import androidx.room3.RoomDatabase
import androidx.room3.useReaderConnection
import androidx.room3.withWriteTransaction
import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

/**
 * Hand-written SQL for sync, backup, seeding and migrations, on top of
 * either a Room connection or a raw [SQLiteConnection]. Every call suspends,
 * because the web SQLite driver is asynchronous.
 *
 * Reads return [SqlRow]s with the values already copied out, so callers never
 * hold a statement open. Paisa's tables are small enough for that.
 */
class Sql private constructor(private val source: StatementSource) {

    private interface StatementSource {
        suspend fun <R> use(sql: String, block: suspend (SQLiteStatement) -> R): R
    }

    suspend fun exec(sql: String, vararg args: Any?) {
        source.use(sql) { stmt ->
            stmt.bindAll(args)
            while (stmt.stepNow()) Unit
        }
    }

    suspend fun rows(sql: String, vararg args: Any?): List<SqlRow> = source.use(sql) { stmt ->
        stmt.bindAll(args)
        val out = mutableListOf<SqlRow>()
        var columns: List<String>? = null
        while (stmt.stepNow()) {
            val names = columns ?: stmt.getColumnNames().also { columns = it }
            out += SqlRow(names, List(names.size) { stmt.valueAt(it) })
        }
        out
    }

    suspend fun first(sql: String, vararg args: Any?): SqlRow? = rows(sql, *args).firstOrNull()

    suspend fun long(sql: String, vararg args: Any?): Long? =
        first(sql, *args)?.takeUnless { it.isNull(0) }?.getLong(0)

    suspend fun double(sql: String, vararg args: Any?): Double? =
        first(sql, *args)?.takeUnless { it.isNull(0) }?.getDouble(0)

    suspend fun string(sql: String, vararg args: Any?): String? =
        first(sql, *args)?.takeUnless { it.isNull(0) }?.getString(0)

    /**
     * Inserts [values] and returns the new rowid, or -1 when `OR IGNORE` skipped
     * the row. [conflict] is ABORT, IGNORE or REPLACE.
     */
    suspend fun insert(table: String, values: Map<String, Any?>, conflict: String = "ABORT"): Long {
        val cols = values.keys.toList()
        exec(
            "INSERT OR $conflict INTO `$table` (${cols.joinToString { "`$it`" }}) " +
                "VALUES (${cols.joinToString { "?" }})",
            *values.values.toTypedArray()
        )
        val row = first("SELECT changes(), last_insert_rowid()")!!
        return if (row.getLong(0) == 0L) -1 else row.getLong(1)
    }

    suspend fun update(
        table: String,
        values: Map<String, Any?>,
        where: String,
        whereArgs: Array<out Any?>,
        conflict: String = "ABORT"
    ) {
        val set = values.keys.joinToString { "`$it` = ?" }
        exec(
            "UPDATE OR $conflict `$table` SET $set WHERE $where",
            *(values.values.toList() + whereArgs.toList()).toTypedArray()
        )
    }

    /** The schema version Room keeps in `PRAGMA user_version`. */
    suspend fun userVersion(): Int = long("PRAGMA user_version")?.toInt() ?: 0

    companion object {
        /** For migrations and Room callbacks, which hand over a raw connection. */
        fun of(connection: SQLiteConnection): Sql = Sql(object : StatementSource {
            override suspend fun <R> use(sql: String, block: suspend (SQLiteStatement) -> R): R {
                val stmt = connection.prepareNow(sql)
                try {
                    return block(stmt)
                } finally {
                    stmt.close()
                }
            }
        })

        /** For work inside a Room transaction or connection. */
        fun of(connection: PooledConnection): Sql = Sql(object : StatementSource {
            override suspend fun <R> use(sql: String, block: suspend (SQLiteStatement) -> R): R =
                connection.usePrepared(sql) { block(it) }
        })
    }
}

/** One result row, copied out of its statement. */
class SqlRow(val columns: List<String>, private val values: List<Any?>) {
    fun isNull(i: Int): Boolean = values[i] == null
    fun get(i: Int): Any? = values[i]
    fun getString(i: Int): String = values[i].let { if (it is ByteArray) it.decodeToString() else it.toString() }
    fun getLong(i: Int): Long = when (val v = values[i]) {
        is Long -> v
        is Double -> v.toLong()
        is String -> v.toLongOrNull() ?: v.toDoubleOrNull()?.toLong() ?: 0L
        else -> 0L
    }
    fun getInt(i: Int): Int = getLong(i).toInt()
    fun getDouble(i: Int): Double = when (val v = values[i]) {
        is Double -> v
        is Long -> v.toDouble()
        is String -> v.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
    fun getBlob(i: Int): ByteArray? = values[i] as? ByteArray
    fun index(name: String): Int = columns.indexOf(name).also {
        require(it >= 0) { "No column $name in ${columns.joinToString()}" }
    }
    fun toMap(): Map<String, Any?> = columns.indices.associate { columns[it] to values[it] }
}

private fun SQLiteStatement.bindAll(args: Array<out Any?>) {
    args.forEachIndexed { i, arg ->
        val index = i + 1
        when (arg) {
            null -> bindNull(index)
            is Boolean -> bindLong(index, if (arg) 1 else 0)
            is Int -> bindLong(index, arg.toLong())
            is Long -> bindLong(index, arg)
            is Float -> bindDouble(index, arg.toDouble())
            is Double -> bindDouble(index, arg)
            is ByteArray -> bindBlob(index, arg)
            else -> bindText(index, arg.toString())
        }
    }
}

private fun SQLiteStatement.valueAt(i: Int): Any? = when (getColumnType(i)) {
    SQLITE_DATA_NULL -> null
    SQLITE_DATA_INTEGER -> getLong(i)
    SQLITE_DATA_FLOAT -> getDouble(i)
    SQLITE_DATA_BLOB -> getBlob(i)
    else -> getText(i)
}

/** Runs [block] in a write transaction. Rolls back if it throws. */
suspend fun <R> RoomDatabase.sqlTransaction(block: suspend (Sql) -> R): R =
    withWriteTransaction { block(Sql.of(this)) }

/** Runs [block] on a read connection. */
suspend fun <R> RoomDatabase.sqlRead(block: suspend (Sql) -> R): R =
    useReaderConnection { block(Sql.of(it)) }
