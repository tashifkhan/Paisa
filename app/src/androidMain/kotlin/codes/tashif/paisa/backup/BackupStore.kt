package codes.tashif.paisa.backup

import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class RemoteBackup(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val createdAt: Long
)

/** The provider rejected our credentials; the user has to sign in again in the app. */
class BackupAuthException(message: String) : IOException(message)

interface BackupStore {
    /** Newest first. */
    suspend fun list(): List<RemoteBackup>
    suspend fun upload(name: String, bytes: ByteArray): RemoteBackup
    suspend fun download(backup: RemoteBackup): ByteArray
    suspend fun delete(backup: RemoteBackup)

    /** Every sync change file, from all devices. Order is not guaranteed. */
    suspend fun listSync(): List<RemoteBackup>
    suspend fun uploadSync(name: String, bytes: ByteArray): RemoteBackup
    suspend fun downloadSync(file: RemoteBackup): ByteArray
    suspend fun deleteSync(file: RemoteBackup)
}

object BackupNames {
    const val EXTENSION = ".paisa"
    const val PREFIX = "paisa-backup-"
    const val MIME = "application/octet-stream"

    private fun format() = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun forTime(millis: Long): String = PREFIX + format().format(Date(millis)) + EXTENSION

    fun isBackup(name: String) = name.startsWith(PREFIX) && name.endsWith(EXTENSION)

    /** Time encoded in the file name, so every provider sorts the same way. */
    fun timeOf(name: String): Long? = try {
        format().parse(name.removePrefix(PREFIX).removeSuffix(EXTENSION))?.time
    } catch (e: Exception) {
        null
    }
}
