package codes.tashif.paisa.ui.backup

import android.content.Context
import codes.tashif.paisa.backup.GoogleDriveAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

/** Files through the content resolver; Google tokens from Play services. */
class AndroidBackupHooks(context: Context) : BackupServiceHooks {
    private val app = context.applicationContext

    override suspend fun writeFile(file: BackupFile, bytes: ByteArray) = withContext(Dispatchers.IO) {
        app.contentResolver.openOutputStream(file.uri)?.use { it.write(bytes) }
            ?: throw IOException("Couldn't open that file")
    }

    override suspend fun readFile(file: BackupFile): ByteArray = withContext(Dispatchers.IO) {
        val input = app.contentResolver.openInputStream(file.uri)
            ?: throw IOException("Couldn't open that file")
        input.use { it.readBytes() }
    }

    override suspend fun googleTokenForRevoke(): String? = GoogleDriveAuth.silentToken(app)
}
