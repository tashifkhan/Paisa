package codes.tashif.paisa.backup

import java.io.IOException

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
