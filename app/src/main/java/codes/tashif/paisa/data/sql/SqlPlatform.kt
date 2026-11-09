package codes.tashif.paisa.data.sql

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

// androidx.sqlite steps and prepares synchronously here and asynchronously on
// the web, so its common API has neither. These bridge the two.

internal suspend fun SQLiteStatement.stepNow(): Boolean = step()

internal suspend fun SQLiteConnection.prepareNow(sql: String): SQLiteStatement = prepare(sql)
