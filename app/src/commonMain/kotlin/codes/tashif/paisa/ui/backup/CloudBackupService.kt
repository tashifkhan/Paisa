package codes.tashif.paisa.ui.backup

import codes.tashif.paisa.backup.BackupConfig
import codes.tashif.paisa.backup.BackupCrypto
import codes.tashif.paisa.backup.BackupFrequency
import codes.tashif.paisa.backup.BackupManager
import codes.tashif.paisa.backup.BackupProvider
import codes.tashif.paisa.backup.GoogleDriveStore
import codes.tashif.paisa.backup.NextcloudAccount
import codes.tashif.paisa.backup.NextcloudStore
import codes.tashif.paisa.backup.NextcloudTransport
import codes.tashif.paisa.backup.RemoteBackup
import codes.tashif.paisa.backup.RestoreResult
import codes.tashif.paisa.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.io.IOException

/** The parts of [CloudBackupService] that differ per platform. */
interface BackupServiceHooks {
    suspend fun writeFile(file: BackupFile, bytes: ByteArray)

    /** The picked file's bytes. Size is checked by the caller. */
    suspend fun readFile(file: BackupFile): ByteArray

    /** A Google token that is still valid, to revoke on disconnect. Null when there is none. */
    suspend fun googleTokenForRevoke(): String?

    /** A token from the sign-in screen. The web keeps it, since browsers get no refresh token. */
    fun rememberGoogleToken(token: String) = Unit

    /** Forgets anything kept about Google after a disconnect. */
    fun forgetGoogle() = Unit

    /** How Nextcloud requests travel. */
    fun nextcloudTransport(): NextcloudTransport = NextcloudTransport.Direct

    /** Web only: whether to ask for a CORS proxy, and where it's stored. */
    val nextcloudNeedsProxy: Boolean get() = false
    fun nextcloudProxy(): String? = null
    fun setNextcloudProxy(url: String?) = Unit
}

/** [BackupService] for every platform, on [BackupManager] and [SyncManager]. */
class CloudBackupService(
    private val manager: BackupManager,
    private val sync: SyncManager,
    private val hooks: BackupServiceHooks
) : BackupService {

    override val cloudUnavailableMessage: String? = null
    override val syncUnavailableMessage: String? = null
    override val keptBackups: Int = BackupManager.KEEP

    override fun loadConfig(): BackupConfig = manager.prefs.load()

    override fun configChanges(): Flow<Unit> = manager.prefs.store.changes()

    override fun schedule() = manager.schedule()

    override suspend fun list(): List<RemoteBackup> = manager.list()

    override suspend fun download(backup: RemoteBackup): ByteArray = manager.download(backup)

    override suspend fun delete(backup: RemoteBackup) = manager.delete(backup)

    override suspend fun backupNow(): RemoteBackup = manager.backupNow()

    override suspend fun createArchive(): ByteArray = manager.createArchive()

    override suspend fun restore(data: ByteArray, password: CharArray?): RestoreResult =
        manager.restore(data, password)

    override suspend fun writeFile(file: BackupFile, bytes: ByteArray) = hooks.writeFile(file, bytes)

    override suspend fun readFile(file: BackupFile): ByteArray = manager.checkSize(hooks.readFile(file))

    override suspend fun connectGoogle(accessToken: String): String? {
        hooks.rememberGoogleToken(accessToken)
        val email = GoogleDriveStore.email(accessToken)
        manager.prefs.setGoogle(email)
        return email
    }

    override fun normalizeNextcloudServer(input: String): String? = NextcloudStore.normalizeServer(input)

    override fun nextcloudServerError(server: String): String? = NextcloudStore.serverError(server)

    override suspend fun startNextcloudLogin(server: String): NextcloudLogin {
        val flow = NextcloudStore.startLogin(server, hooks.nextcloudTransport())
        return NextcloudLogin(flow.loginUrl, flow)
    }

    override suspend fun pollNextcloudLogin(login: NextcloudLogin): NextcloudAccount =
        NextcloudStore.pollLogin(login.handle as NextcloudStore.Companion.LoginFlow, hooks.nextcloudTransport())

    override suspend fun verifyNextcloud(server: String, loginName: String, appPassword: String): NextcloudAccount =
        NextcloudStore.verify(server, loginName, appPassword, hooks.nextcloudTransport())

    override fun setNextcloud(account: NextcloudAccount) = manager.prefs.setNextcloud(account)

    override val nextcloudNeedsProxy: Boolean get() = hooks.nextcloudNeedsProxy
    override fun nextcloudProxy(): String? = hooks.nextcloudProxy()
    override fun setNextcloudProxy(url: String?) = hooks.setNextcloudProxy(url)

    override suspend fun disconnect(provider: BackupProvider) {
        if (provider == BackupProvider.GOOGLE_DRIVE) {
            try {
                hooks.googleTokenForRevoke()?.let { GoogleDriveStore.revoke(it) }
            } catch (e: IOException) {
                // Not signed in any more; nothing to revoke.
            }
            hooks.forgetGoogle()
        }
        manager.prefs.disconnect()
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
