package codes.tashif.paisa.shared

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

/**
 * What a decrypted `.paisa` file holds, read without touching a database.
 * Sync change files carry `rows` and `deleted` keyed by `syncId`; backups
 * carry `tables` keyed by local row id.
 */
sealed interface PaisaFile {
    val format: Int
    val dbVersion: Int
    val createdAt: Long

    /** Row count per table. */
    val tableCounts: Map<String, Int>

    /** A few rows per table, for a preview. */
    val samples: Map<String, List<JsonObject>>

    data class SyncChanges(
        override val format: Int,
        override val dbVersion: Int,
        override val createdAt: Long,
        val deviceId: String,
        val deviceName: String,
        val seq: Long,
        val full: Boolean,
        override val tableCounts: Map<String, Int>,
        override val samples: Map<String, List<JsonObject>>,
        val deletedCount: Int
    ) : PaisaFile

    data class Backup(
        override val format: Int,
        override val dbVersion: Int,
        override val createdAt: Long,
        val appVersion: String?,
        override val tableCounts: Map<String, Int>,
        override val samples: Map<String, List<JsonObject>>
    ) : PaisaFile

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String, samplesPerTable: Int = 3): PaisaFile {
            val root = json.parseToJsonElement(text).jsonObject
            val format = root["format"]?.jsonPrimitive?.int ?: 0
            val dbVersion = root["dbVersion"]?.jsonPrimitive?.int ?: 0
            val createdAt = root["createdAt"]?.jsonPrimitive?.longOrNull ?: 0L
            val rows = root["rows"]?.jsonObject
            if (rows != null) {
                return SyncChanges(
                    format = format,
                    dbVersion = dbVersion,
                    createdAt = createdAt,
                    deviceId = root["deviceId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    deviceName = root["deviceName"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    seq = root["seq"]?.jsonPrimitive?.long ?: 0L,
                    full = root["full"]?.jsonPrimitive?.booleanOrNull ?: false,
                    tableCounts = rows.mapValues { it.value.jsonArray.size },
                    samples = rows.mapValues { (_, list) ->
                        // Sync rows wrap their columns in "v".
                        list.jsonArray.take(samplesPerTable).map { it.jsonObject["v"]?.jsonObject ?: it.jsonObject }
                    },
                    deletedCount = (root["deleted"] as? JsonArray)?.size ?: 0
                )
            }
            val tables = root["tables"]?.jsonObject
                ?: throw NotAPaisaFileException("File has neither sync rows nor backup tables")
            return Backup(
                format = format,
                dbVersion = dbVersion,
                createdAt = createdAt,
                appVersion = root["appVersion"]?.jsonPrimitive?.contentOrNull,
                tableCounts = tables.mapValues { (it.value as? JsonArray)?.size ?: 0 },
                samples = tables.mapValues { (_, list) ->
                    (list as? JsonArray).orEmpty().take(samplesPerTable).mapNotNull { it as? JsonObject }
                }
            )
        }
    }
}
