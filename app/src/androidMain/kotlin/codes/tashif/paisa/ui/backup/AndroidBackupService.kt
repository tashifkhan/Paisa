package codes.tashif.paisa.ui.backup

import android.content.Context
import android.content.SharedPreferences
import codes.tashif.paisa.backup.BackupConfig
import codes.tashif.paisa.backup.BackupCrypto
import codes.tashif.paisa.backup.BackupFrequency
import codes.tashif.paisa.backup.BackupManager
import codes.tashif.paisa.backup.BackupProvider
import codes.tashif.paisa.backup.GoogleDriveAuth
import codes.tashif.paisa.backup.NextcloudAccount
import codes.tashif.paisa.backup.NextcloudStore
import codes.tashif.paisa.backup.RemoteBackup
import codes.tashif.paisa.backup.RestoreResult
import codes.tashif.paisa.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.IOException

/** [BackupService] on top of [BackupManager], its prefs, [SyncManager] and the cloud stores. */
class AndroidBackupService(
    private val context: Context,
    private val manager: BackupManager,
    private val sync: SyncManager
) : BackupService {

    override val cloudUnavailableMessage: String? = null
    override val syncUnavailableMessage: String? = null
    override val keptBackups: Int = BackupManager.KEEP

    override fun loadConfig(): BackupConfig = manager.prefs.load()

    override fun configChanges(): Flow<Unit> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(Unit) }
        manager.prefs.prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { manager.prefs.prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override fun schedule() = manager.schedule()

    override suspend fun list(): List<RemoteBackup> = manager.list()

    override suspend fun download(backup: RemoteBackup): ByteArray = manager.download(backup)

    override suspend fun backupNow(): RemoteBackup = manager.backupNow()

    override suspend fun createArchive(): ByteArray = manager.createArchive()

    override suspend fun restore(data: ByteArray, password: CharArray?): RestoreResult =
        manager.restore(data, password)

    override suspend fun writeFile(file: BackupFile, bytes: ByteArray) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(file.uri)?.use { it.write(bytes) }
            ?: throw IOException("Couldn't open that file")
    }

    override suspend fun readFile(file: BackupFile): ByteArray = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(file.uri)
            ?: throw IOException("Couldn't open that file")
        manager.readLimited(input)
    }

    override suspend fun connectGoogle(accessToken: String): String? {
        val email = GoogleDriveAuth.email(accessToken)
        manager.prefs.setGoogle(email)
        return email
    }

    override fun normalizeNextcloudServer(input: String): String? = NextcloudStore.normalizeServer(input)

    override fun nextcloudServerError(server: String): String? = NextcloudStore.serverError(server)

    override suspend fun startNextcloudLogin(server: String): NextcloudLogin {
        val flow = NextcloudStore.startLogin(server)
        return NextcloudLogin(flow.loginUrl, flow)
    }

    override suspend fun pollNextcloudLogin(login: NextcloudLogin): NextcloudAccount =
        NextcloudStore.pollLogin(login.handle as NextcloudStore.Companion.LoginFlow)

    override suspend fun verifyNextcloud(
        server: String,
        loginName: String,
        appPassword: String
    ): NextcloudAccount = NextcloudStore.verify(server, loginName, appPassword)

    override fun setNextcloud(account: NextcloudAccount) = manager.prefs.setNextcloud(account)

    override suspend fun disconnect(provider: BackupProvider) {
        if (provider == BackupProvider.GOOGLE_DRIVE) {
            try {
                GoogleDriveAuth.revoke(GoogleDriveAuth.silentToken(context))
            } catch (e: IOException) {
                // Not signed in any more; nothing to revoke.
            }
        }
        manager.prefs.disconnect()
        SyncManager.invalidateCache()
        SyncManager.schedule(context)
        manager.schedule()
    }

    override fun setFrequency(frequency: BackupFrequency) {
        manager.prefs.setFrequency(frequency)
        manager.schedule()
    }

    override fun setAllowCellular(allow: Boolean) {
        manager.prefs.setAllowCellular(allow)
        manager.schedule()
    }

    override fun setEncryptionKey(key: BackupCrypto.DerivedKey?) = manager.prefs.setEncryptionKey(key)

    override suspend fun enableSync(password: CharArray) = sync.enable(password)

    override fun disableSync() = sync.disable()

    override suspend fun syncNow(): SyncSummary {
        val report = sync.syncNow()
        return SyncSummary(report.received, report.sent, report.devices)
    }
}
