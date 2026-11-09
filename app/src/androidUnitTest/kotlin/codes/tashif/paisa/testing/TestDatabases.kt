package codes.tashif.paisa.testing

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.sql.Sql
import codes.tashif.paisa.data.sql.sqlTransaction
import kotlinx.coroutines.runBlocking

/** A fresh in-memory database with the real seed and sync triggers. */
fun inMemoryDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder<AppDatabase>(ApplicationProvider.getApplicationContext<Context>())
        .addCallback(AppDatabase.callback)
        .build()

/** Runs [block] in a write transaction, blocking the test thread. */
fun <T> AppDatabase.sql(block: suspend (Sql) -> T): T = runBlocking { sqlTransaction(block) }

fun AppDatabase.long(query: String, vararg args: Any?): Long = sql { it.long(query, *args) ?: 0L }

fun AppDatabase.double(query: String, vararg args: Any?): Double = sql { it.double(query, *args) ?: 0.0 }

fun AppDatabase.string(query: String, vararg args: Any?): String? = sql { it.string(query, *args) }

fun AppDatabase.exec(query: String, vararg args: Any?) = sql { it.exec(query, *args) }
