package codes.tashif.paisa.backup

import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.platform.parseDate
import kotlinx.io.IOException

// Plain backup types shared by the backup screen on every platform. The
// Android stores, prefs and workers that produce them stay in androidMain.

enum class BackupProvider(val id: String, val label: String) {
    NONE("none", "Not set up"),
    GOOGLE_DRIVE("gdrive", "Google Drive"),
    NEXTCLOUD("nextcloud", "Nextcloud");

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: NONE
    }
}

enum class BackupFrequency(val id: String, val label: String, val days: Long) {
    MANUAL("manual", "Only when I tap \"Back up\"", 0),
    DAILY("daily", "Daily", 1),
    WEEKLY("weekly", "Weekly", 7),
    MONTHLY("monthly", "Monthly", 30);

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: MANUAL
    }
}

data class NextcloudAccount(
    val serverUrl: String,
    val loginName: String,
    val userId: String,
    val appPassword: String
)

data class BackupConfig(
    val provider: BackupProvider,
    val frequency: BackupFrequency,
    val allowCellular: Boolean,
    val encrypted: Boolean,
    val googleEmail: String?,
    val nextcloud: NextcloudAccount?,
    val lastBackupAt: Long,
    val lastBackupSize: Long,
    val lastError: String?,
    val sync: SyncStatus
)

/** What the backup screen shows about multi-device sync. */
data class SyncStatus(
    val enabled: Boolean,
    val lastSyncAt: Long,
    val error: String?,
    /** Names of the other devices this one has received changes from. */
    val peers: List<String>
)

data class RemoteBackup(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val createdAt: Long
)

/** The backup is encrypted with a key this device doesn't have. Ask for the password. */
class PasswordRequiredException : IOException("This backup is encrypted. Enter its password.")

data class RestoreResult(val transactions: Int, val backupCreatedAt: Long)

object BackupNames {
    const val EXTENSION = ".paisa"
    const val PREFIX = "paisa-backup-"
    const val MIME = "application/octet-stream"

    private const val PATTERN = "yyyyMMdd'T'HHmmss'Z'"

    fun forTime(millis: Long): String =
        PREFIX + formatDate(millis, PATTERN, utc = true, machine = true) + EXTENSION

    fun isBackup(name: String) = name.startsWith(PREFIX) && name.endsWith(EXTENSION)

    /** Time encoded in the file name, so every provider sorts the same way. */
    fun timeOf(name: String): Long? =
        parseDate(name.removePrefix(PREFIX).removeSuffix(EXTENSION), PATTERN, utc = true, machine = true)
}
