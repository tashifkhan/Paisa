package codes.tashif.paisa.data.sql

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

// androidx.sqlite steps and prepares synchronously on Android and
// asynchronously on the web, so its common API has neither. These bridge the two.

internal expect suspend fun SQLiteStatement.stepNow(): Boolean

internal expect suspend fun SQLiteConnection.prepareNow(sql: String): SQLiteStatement
