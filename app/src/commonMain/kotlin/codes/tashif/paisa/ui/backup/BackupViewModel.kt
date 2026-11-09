package codes.tashif.paisa.ui.backup

import codes.tashif.paisa.platform.formatFixed
import codes.tashif.paisa.platform.formatMediumDateTime
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.backup.BackupConfig
import codes.tashif.paisa.backup.BackupCrypto
import codes.tashif.paisa.backup.BackupFrequency
import codes.tashif.paisa.backup.BackupProvider
import codes.tashif.paisa.backup.NextcloudAccount
import codes.tashif.paisa.backup.PasswordRequiredException
import codes.tashif.paisa.backup.RemoteBackup
import codes.tashif.paisa.backup.RestoreResult
import codes.tashif.paisa.core.AppLockGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

/** Restore waiting on a password. Holds the downloaded bytes so we don't fetch twice. */
class PendingRestore(val label: String, val data: ByteArray)

class BackupViewModel(
    private val service: BackupService,
    private val appLockGuard: AppLockGuard
) : ViewModel() {

    /** Call right before opening the browser, Google sign-in or a file picker. */
    fun onExternalActivityLaunched() = appLockGuard.onExternalActivityLaunched()

    private val _config = MutableStateFlow(service.loadConfig())
    val config: StateFlow<BackupConfig> = _config.asStateFlow()

    /** null = not loaded yet. */
    private val _remote = MutableStateFlow<List<RemoteBackup>?>(null)
    val remote: StateFlow<List<RemoteBackup>?> = _remote.asStateFlow()

    /** Label of the running operation, e.g. "Backing up…". */
    private val _busy = MutableStateFlow<String?>(null)
    val busy: StateFlow<String?> = _busy.asStateFlow()

    private val _pendingRestore = MutableStateFlow<PendingRestore?>(null)
    val pendingRestore: StateFlow<PendingRestore?> = _pendingRestore.asStateFlow()

    /** Set while waiting for the user to approve Paisa in the Nextcloud web page. */
    private val _nextcloudLoginUrl = MutableStateFlow<String?>(null)
    val nextcloudLoginUrl: StateFlow<String?> = _nextcloudLoginUrl.asStateFlow()
    private var nextcloudPoll: Job? = null

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    /** Emits once after a restore commits, so the host can leave onboarding. */
    private val _restored = MutableSharedFlow<RestoreResult>(extraBufferCapacity = 1)
    val restored = _restored.asSharedFlow()

    /** Null where Google Drive and Nextcloud work, otherwise why they don't. */
    val cloudUnavailableMessage: String? get() = service.cloudUnavailableMessage

    /** Remote copies kept per provider. */
    val keptBackups: Int get() = service.keptBackups

    init {
        // BackupWorker writes status through its own BackupPrefs; pick those changes up.
        viewModelScope.launch {
            service.configChanges().collect { _config.value = service.loadConfig() }
        }
        refreshList()
    }

    private fun reload() {
        _config.value = service.loadConfig()
    }

    /** Shows [message] in the screen's snackbar. */
    fun showMessage(message: String) = say(message)

    fun nextcloudServerError(server: String): String? = service.nextcloudServerError(server)

    private fun say(message: String) {
        _messages.tryEmit(message)
    }

    /** Runs [block] with a busy label; IO errors become a message. */
    private fun run(label: String, block: suspend () -> Unit) {
        if (_busy.value != null) return
        viewModelScope.launch {
            _busy.value = label
            try {
                block()
            } catch (e: IOException) {
                say(e.message ?: "Something went wrong")
            } catch (e: RuntimeException) {
                say(e.message ?: "Something went wrong")
            } finally {
                _busy.value = null
                reload()
            }
        }
    }

    fun refreshList() {
        if (_config.value.provider == BackupProvider.NONE) {
            _remote.value = emptyList()
            return
        }
        viewModelScope.launch {
            _remote.value = try {
                service.list()
            } catch (e: IOException) {
                say(e.message ?: "Couldn't load backups")
                emptyList()
            }
        }
    }

    // --- Accounts ---

    fun onGoogleAuthorized(accessToken: String?) = run("Connecting…") {
        val token = accessToken ?: throw IOException("Google returned no access token")
        val email = service.connectGoogle(token)
        reload()
        service.schedule()
        refreshList()
        say(if (email != null) "Connected $email" else "Connected Google Drive")
    }

    fun onGoogleAuthFailed(message: String) = say(message)

    fun connectNextcloud(serverInput: String) {
        val server = service.normalizeNextcloudServer(serverInput)
        if (server == null) {
            say("Enter your Nextcloud address")
            return
        }
        service.nextcloudServerError(server)?.let {
            say(it)
            return
        }
        nextcloudPoll?.cancel()
        nextcloudPoll = viewModelScope.launch {
            try {
                val flow = service.startNextcloudLogin(server)
                _nextcloudLoginUrl.value = flow.loginUrl
                val account = service.pollNextcloudLogin(flow)
                finishNextcloud(account)
            } catch (e: IOException) {
                say(e.message ?: "Couldn't reach Nextcloud")
            } finally {
                _nextcloudLoginUrl.value = null
            }
        }
    }

    fun cancelNextcloudLogin() {
        nextcloudPoll?.cancel()
        _nextcloudLoginUrl.value = null
    }

    fun connectNextcloudManual(serverInput: String, user: String, appPassword: String) {
        val server = service.normalizeNextcloudServer(serverInput)
        if (server == null || user.isBlank() || appPassword.isBlank()) {
            say("Fill in the address, username and app password")
            return
        }
        service.nextcloudServerError(server)?.let {
            say(it)
            return
        }
        run("Connecting…") {
            finishNextcloud(service.verifyNextcloud(server, user.trim(), appPassword.trim()))
        }
    }

    private suspend fun finishNextcloud(account: NextcloudAccount) {
        service.setNextcloud(account)
        reload()
        service.schedule()
        refreshList()
        say("Connected ${account.loginName} on ${urlHost(account.serverUrl)}")
    }

    fun disconnect() = run("Disconnecting…") {
        service.disconnect(_config.value.provider)
        reload()
        _remote.value = emptyList()
        say("Disconnected. Backups already uploaded were left in place.")
    }

    // --- Settings ---

    fun setFrequency(frequency: BackupFrequency) {
        service.setFrequency(frequency)
        reload()
    }

    fun setAllowCellular(allow: Boolean) {
        service.setAllowCellular(allow)
        reload()
    }

    fun enableEncryption(password: String) = run("Setting password…") {
        val key = withContext(Dispatchers.Default) {
            BackupCrypto.deriveKey(password.toCharArray(), BackupCrypto.newSalt())
        }
        service.setEncryptionKey(key)
        say("Encryption on. New backups need this password to restore.")
    }

    fun disableEncryption() {
        service.setEncryptionKey(null)
        reload()
        say("Encryption off. New backups won't need a password.")
    }

    // --- Sync ---

    fun enableSync(password: String) {
        service.syncUnavailableMessage?.let {
            say(it)
            return
        }
        enableSyncNow(password)
    }

    private fun enableSyncNow(password: String) = run("Turning on sync…") {
        service.enableSync(password.toCharArray())
        reload()
        _busy.value = "Syncing…"
        val report = service.syncNow()
        say(
            if (report.devices == 0) "Sync is on. Turn it on with the same password on your other devices."
            else "Sync is on. Got ${plural(report.received, "change")} from ${plural(report.devices, "device")}."
        )
    }

    fun disableSync() {
        service.disableSync()
        reload()
        say("Sync is off. Data on this device and in the cloud stays.")
    }

    fun syncNow() = run("Syncing…") {
        val report = service.syncNow()
        say(
            when {
                report.received == 0 && report.sent == 0 -> "Already up to date"
                else -> "Sent ${plural(report.sent, "change")}, got ${plural(report.received, "change")}"
            }
        )
    }

    private fun plural(n: Int, word: String) = "$n $word" + if (n == 1) "" else "s"

    // --- Backup / restore ---

    fun backupNow() = run("Backing up…") {
        val uploaded = service.backupNow()
        refreshList()
        say("Backed up ${formatSize(uploaded.sizeBytes)}")
    }

    fun exportToFile(file: BackupFile) = run("Saving file…") {
        val bytes = service.createArchive()
        service.writeFile(file, bytes)
        say("Saved ${formatSize(bytes.size.toLong())} backup file")
    }

    fun restoreRemote(backup: RemoteBackup) = run("Downloading…") {
        val bytes = service.download(backup)
        attemptRestore(formatDate(backup.createdAt), bytes, null)
    }

    fun restoreFromFile(file: BackupFile) = run("Reading file…") {
        val bytes = service.readFile(file)
        attemptRestore("selected file", bytes, null)
    }

    fun restoreWithPassword(password: String) {
        val pending = _pendingRestore.value ?: return
        _pendingRestore.value = null
        run("Restoring…") { attemptRestore(pending.label, pending.data, password.toCharArray()) }
    }

    fun cancelPendingRestore() {
        _pendingRestore.value = null
    }

    private suspend fun attemptRestore(label: String, bytes: ByteArray, password: CharArray?) {
        _busy.value = "Restoring…"
        try {
            val result = service.restore(bytes, password)
            say(
                "Restored ${result.transactions} transaction" +
                    (if (result.transactions == 1) "" else "s") +
                    " from ${formatDate(result.backupCreatedAt)}"
            )
            _restored.tryEmit(result)
        } catch (e: PasswordRequiredException) {
            _pendingRestore.value = PendingRestore(label, bytes)
        } catch (e: BackupCrypto.WrongPasswordException) {
            say("Wrong password")
            _pendingRestore.value = PendingRestore(label, bytes)
        }
    }

    companion object {
        fun formatSize(bytes: Long): String = when {
            bytes >= 1024 * 1024 -> "${formatFixed(bytes / (1024.0 * 1024.0), 1)} MB"
            bytes >= 1024 -> "${formatFixed(bytes / 1024.0, 0)} KB"
            else -> "$bytes B"
        }

        fun formatDate(millis: Long): String =
            formatMediumDateTime(millis)
    }
}
