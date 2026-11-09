package codes.tashif.paisa.ui.backup

import codes.tashif.paisa.ui.platform.PlatformBackHandler
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import codes.tashif.paisa.backup.BackupProvider
import codes.tashif.paisa.backup.RemoteBackup
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.theme.spacing

/**
 * Backups, laid out like WhatsApp's chat backup screen: last backup and a big
 * "Back up" button on top, then the account, schedule, encryption and restore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    onRestored: () -> Unit = {},
    viewModel: BackupViewModel = koinViewModel()
) {
    val backup = viewModel
    val config by backup.config.collectAsState()
    val remote by backup.remote.collectAsState()
    val busy by backup.busy.collectAsState()
    val pendingRestore by backup.pendingRestore.collectAsState()
    val nextcloudLoginUrl by backup.nextcloudLoginUrl.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var showFrequency by remember { mutableStateOf(false) }
    var showSetPassword by remember { mutableStateOf(false) }
    var showDisableEncryption by remember { mutableStateOf(false) }
    var showNextcloud by remember { mutableStateOf(false) }
    var showDisconnect by remember { mutableStateOf(false) }
    var showSyncPassword by remember { mutableStateOf(false) }
    var showDisableSync by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf<RemoteBackup?>(null) }

    PlatformBackHandler(onBack = onBack)

    val launchers = rememberBackupLaunchers(backup)

    LaunchedEffect(Unit) { backup.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(Unit) { backup.restored.collect { onRestored() } }

    // Nextcloud Login Flow v2: approve Paisa in the browser while we poll.
    LaunchedEffect(nextcloudLoginUrl) {
        val url = nextcloudLoginUrl ?: return@LaunchedEffect
        showNextcloud = false
        backup.onExternalActivityLaunched()
        launchers.openUrl(url)
    }

    val cloudUnavailable = backup.cloudUnavailableMessage

    val connected = config.provider != BackupProvider.NONE
    val isBusy = busy != null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            DetailHeader(title = "Backup", onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = MaterialTheme.spacing.medium)
                    .padding(bottom = MaterialTheme.spacing.extraLarge)
            ) {
                BackupStatusCard(
                    lastBackupAt = config.lastBackupAt,
                    lastBackupSize = config.lastBackupSize,
                    lastError = config.lastError,
                    providerLabel = if (connected) config.provider.label else null,
                    busy = busy,
                    canBackUp = connected && !isBusy,
                    onBackUp = { backup.backupNow() }
                )

                BackupDestinationSection(
                    config = config,
                    nextcloudLoginUrl = nextcloudLoginUrl,
                    isBusy = isBusy,
                    onConnectGoogle = {
                        if (cloudUnavailable != null) {
                            backup.showMessage(cloudUnavailable)
                        } else {
                            launchers.connectGoogle()
                        }
                    },
                    onOpenNextcloud = {
                        if (cloudUnavailable != null) {
                            backup.showMessage(cloudUnavailable)
                        } else {
                            showNextcloud = true
                        }
                    },
                    onDisconnect = { showDisconnect = true }
                )

                BackupSyncSection(
                    sync = config.sync,
                    connected = connected,
                    isBusy = isBusy,
                    onEnable = { showSyncPassword = true },
                    onDisable = { showDisableSync = true },
                    onSyncNow = { backup.syncNow() }
                )

                BackupScheduleSection(
                    frequencyLabel = config.frequency.label,
                    allowCellular = config.allowCellular,
                    onOpenFrequency = { showFrequency = true },
                    onAllowCellularChange = { backup.setAllowCellular(it) }
                )

                BackupEncryptionSection(
                    encrypted = config.encrypted,
                    onEnable = { showSetPassword = true },
                    onDisable = { showDisableEncryption = true }
                )

                BackupRestoreSection(
                    remote = remote.orEmpty(),
                    providerLabel = config.provider.label,
                    isBusy = isBusy,
                    onRestoreRemote = { confirmRestore = it },
                    onRestoreFromFile = {
                        backup.onExternalActivityLaunched()
                        launchers.restoreFromFile()
                    },
                    onSaveToFile = {
                        backup.onExternalActivityLaunched()
                        launchers.saveToFile()
                    }
                )

                Spacer(Modifier.height(MaterialTheme.spacing.medium))
                if (cloudUnavailable != null) {
                    Text(
                        cloudUnavailable,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small)
                    )
                    Spacer(Modifier.height(MaterialTheme.spacing.small))
                }
                Text(
                    "A backup holds every account, category, transaction (including the SMS " +
                        "text it came from), budget, goal and merchant rule. AI keys and the " +
                        "Nextcloud password are never included. Paisa keeps the newest " +
                        "${backup.keptBackups} backups and deletes older ones. Restoring " +
                        "replaces everything on this phone.\n\nSync works offline: every " +
                        "change is saved on this device first and sent the next time there's " +
                        "a connection. Sync files are encrypted with your sync password before " +
                        "they're uploaded. SMS are read on each phone that has them, and the " +
                        "same SMS on two phones counts once. Your currency follows you; " +
                        "theme and app lock stay on each device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small)
                )
            }
        }
    }

    if (showFrequency) {
        BackupFrequencyDialog(
            selected = config.frequency,
            connected = connected,
            onSelect = { backup.setFrequency(it) },
            onDismiss = { showFrequency = false }
        )
    }

    if (showSetPassword) {
        SetPasswordDialog(
            onDismiss = { showSetPassword = false },
            onConfirm = { pw ->
                showSetPassword = false
                backup.enableEncryption(pw)
            }
        )
    }

    if (showDisableEncryption) {
        DisableEncryptionDialog(
            onDismiss = { showDisableEncryption = false },
            onConfirm = {
                showDisableEncryption = false
                backup.disableEncryption()
            }
        )
    }

    if (showDisconnect) {
        DisconnectDialog(
            providerLabel = config.provider.label,
            onDismiss = { showDisconnect = false },
            onConfirm = {
                showDisconnect = false
                backup.disconnect()
            }
        )
    }

    if (showSyncPassword) {
        SyncPasswordDialog(
            onDismiss = { showSyncPassword = false },
            onConfirm = { pw ->
                showSyncPassword = false
                backup.enableSync(pw)
            }
        )
    }

    if (showDisableSync) {
        DisableSyncDialog(
            onDismiss = { showDisableSync = false },
            onConfirm = {
                showDisableSync = false
                backup.disableSync()
            }
        )
    }

    confirmRestore?.let { item ->
        ConfirmRestoreDialog(
            item = item,
            onDismiss = { confirmRestore = null },
            onConfirm = {
                confirmRestore = null
                backup.restoreRemote(item)
            }
        )
    }

    pendingRestore?.let { pending ->
        EnterPasswordDialog(
            label = pending.label,
            onDismiss = { backup.cancelPendingRestore() },
            onConfirm = { backup.restoreWithPassword(it) }
        )
    }

    if (showNextcloud) {
        NextcloudSheet(
            initialServer = config.nextcloud?.serverUrl.orEmpty(),
            serverError = backup::nextcloudServerError,
            onDismiss = { showNextcloud = false },
            onBrowserLogin = { backup.connectNextcloud(it) },
            onManualLogin = { server, user, pw ->
                showNextcloud = false
                backup.connectNextcloudManual(server, user, pw)
            }
        )
    }

    nextcloudLoginUrl?.let { url ->
        NextcloudApproveDialog(
            onOpenAgain = {
                backup.onExternalActivityLaunched()
                launchers.openUrl(url)
            },
            onCancel = { backup.cancelNextcloudLogin() }
        )
    }
}
