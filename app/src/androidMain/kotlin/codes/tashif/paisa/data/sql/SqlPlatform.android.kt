package codes.tashif.paisa.data.sql

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

internal actual suspend fun SQLiteStatement.stepNow(): Boolean = step()

internal actual suspend fun SQLiteConnection.prepareNow(sql: String): SQLiteStatement = prepare(sql)
