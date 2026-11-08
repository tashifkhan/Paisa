package codes.tashif.paisa.backup

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.AuthorizationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import codes.tashif.paisa.sync.SyncManager
import java.io.IOException
import java.text.DateFormat
import java.util.Date

/** Restore waiting on a password. Holds the downloaded bytes so we don't fetch twice. */
class PendingRestore(val label: String, val data: ByteArray)

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = BackupManager(application)

    private val _config = MutableStateFlow(manager.prefs.load())
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

    // BackupWorker writes status through its own BackupPrefs; pick those changes up.
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _config.value = manager.prefs.load()
    }

    init {
        manager.prefs.prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        refreshList()
    }

    override fun onCleared() {
        manager.prefs.prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun reload() {
        _config.value = manager.prefs.load()
    }

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
                manager.list()
            } catch (e: IOException) {
                say(e.message ?: "Couldn't load backups")
                emptyList()
            }
        }
    }

    // --- Accounts ---

    fun onGoogleAuthorized(result: AuthorizationResult) = run("Connecting…") {
        val token = result.accessToken ?: throw IOException("Google returned no access token")
        val email = GoogleDriveAuth.email(token)
        manager.prefs.setGoogle(email)
        reload()
        manager.schedule()
        refreshList()
        say(if (email != null) "Connected $email" else "Connected Google Drive")
    }

    fun onGoogleAuthFailed(message: String) = say(message)

    fun connectNextcloud(serverInput: String) {
        val server = NextcloudStore.normalizeServer(serverInput)
        if (server == null) {
            say("Enter your Nextcloud address")
            return
        }
        nextcloudPoll?.cancel()
        nextcloudPoll = viewModelScope.launch {
            try {
                val flow = NextcloudStore.startLogin(server)
                _nextcloudLoginUrl.value = flow.loginUrl
                val account = NextcloudStore.pollLogin(flow)
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
        val server = NextcloudStore.normalizeServer(serverInput)
        if (server == null || user.isBlank() || appPassword.isBlank()) {
            say("Fill in the address, username and app password")
            return
        }
        run("Connecting…") {
            finishNextcloud(NextcloudStore.verify(server, user.trim(), appPassword.trim()))
        }
    }

    private suspend fun finishNextcloud(account: NextcloudAccount) {
        manager.prefs.setNextcloud(account)
        reload()
        manager.schedule()
        refreshList()
        say("Connected ${account.loginName} on ${Uri.parse(account.serverUrl).host}")
    }

    fun disconnect() = run("Disconnecting…") {
        if (_config.value.provider == BackupProvider.GOOGLE_DRIVE) {
            try {
                GoogleDriveAuth.revoke(GoogleDriveAuth.silentToken(getApplication()))
            } catch (e: IOException) {
                // Not signed in any more; nothing to revoke.
            }
        }
        manager.prefs.disconnect()
        SyncManager.invalidateCache()
        SyncManager.schedule(getApplication())
        reload()
        manager.schedule()
        _remote.value = emptyList()
        say("Disconnected. Backups already uploaded were left in place.")
    }

    // --- Settings ---

    fun setFrequency(frequency: BackupFrequency) {
        manager.prefs.setFrequency(frequency)
        reload()
        manager.schedule()
    }

    fun setAllowCellular(allow: Boolean) {
        manager.prefs.setAllowCellular(allow)
        reload()
        manager.schedule()
    }

    fun enableEncryption(password: String) = run("Setting password…") {
        val key = withContext(Dispatchers.Default) {
            BackupCrypto.deriveKey(password.toCharArray(), BackupCrypto.newSalt())
        }
        manager.prefs.setEncryptionKey(key)
        say("Encryption on. New backups need this password to restore.")
    }

    fun disableEncryption() {
        manager.prefs.setEncryptionKey(null)
        reload()
        say("Encryption off. New backups won't need a password.")
    }

    // --- Sync ---

    private val sync = SyncManager(application)

    fun enableSync(password: String) = run("Turning on sync…") {
        sync.enable(password.toCharArray())
        reload()
        _busy.value = "Syncing…"
        val report = sync.syncNow()
        say(
            if (report.devices == 0) "Sync is on. Turn it on with the same password on your other devices."
            else "Sync is on. Got ${plural(report.received, "change")} from ${plural(report.devices, "device")}."
        )
    }

    fun disableSync() {
        sync.disable()
        reload()
        say("Sync is off. Data on this device and in the cloud stays.")
    }

    fun syncNow() = run("Syncing…") {
        val report = sync.syncNow()
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
        val uploaded = manager.backupNow()
        refreshList()
        say("Backed up ${formatSize(uploaded.sizeBytes)}")
    }

    fun exportToFile(uri: Uri) = run("Saving file…") {
        val bytes = manager.createArchive()
        withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw IOException("Couldn't open that file")
        }
        say("Saved ${formatSize(bytes.size.toLong())} backup file")
    }

    fun restoreRemote(backup: RemoteBackup) = run("Downloading…") {
        val bytes = manager.download(backup)
        attemptRestore(formatDate(backup.createdAt), bytes, null)
    }

    fun restoreFromFile(uri: Uri) = run("Reading file…") {
        val bytes = withContext(Dispatchers.IO) {
            val input = getApplication<Application>().contentResolver.openInputStream(uri)
                ?: throw IOException("Couldn't open that file")
            manager.readLimited(input)
        }
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
            val result = manager.restore(bytes, password)
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
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }

        fun formatDate(millis: Long): String =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
    }
}
