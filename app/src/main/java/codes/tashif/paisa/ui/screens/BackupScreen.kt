package codes.tashif.paisa.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import codes.tashif.paisa.backup.BackupFrequency
import codes.tashif.paisa.backup.BackupManager
import codes.tashif.paisa.backup.BackupNames
import codes.tashif.paisa.backup.BackupProvider
import codes.tashif.paisa.backup.BackupViewModel
import codes.tashif.paisa.backup.BackupViewModel.Companion.formatDate
import codes.tashif.paisa.backup.BackupViewModel.Companion.formatSize
import codes.tashif.paisa.backup.GoogleDriveAuth
import codes.tashif.paisa.backup.RemoteBackup
import codes.tashif.paisa.data.PaisaViewModel
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.theme.spacing
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

/**
 * Backups, laid out like WhatsApp's chat backup screen: last backup and a big
 * "Back up" button on top, then the account, schedule, encryption and restore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: PaisaViewModel,
    onBack: () -> Unit,
    onRestored: () -> Unit = {},
    backup: BackupViewModel = viewModel()
) {
    val context = LocalContext.current
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

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) { backup.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(Unit) { backup.restored.collect { onRestored() } }

    // Nextcloud Login Flow v2: approve Paisa in the browser while we poll.
    LaunchedEffect(nextcloudLoginUrl) {
        val url = nextcloudLoginUrl ?: return@LaunchedEffect
        showNextcloud = false
        viewModel.onExternalActivityLaunched()
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        try {
            val result = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(res.data)
            backup.onGoogleAuthorized(result)
        } catch (e: ApiException) {
            backup.onGoogleAuthFailed("Google sign-in was cancelled")
        }
    }

    fun connectGoogle() {
        if (!GoogleDriveAuth.isAvailable(context)) {
            backup.onGoogleAuthFailed("Google Drive needs Google Play services. Use Nextcloud instead.")
            return
        }
        Identity.getAuthorizationClient(context)
            .authorize(GoogleDriveAuth.request())
            .addOnSuccessListener { result ->
                val pending = result.pendingIntent
                if (result.hasResolution() && pending != null) {
                    viewModel.onExternalActivityLaunched()
                    googleLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                } else {
                    backup.onGoogleAuthorized(result)
                }
            }
            .addOnFailureListener { e ->
                val code = (e as? ApiException)?.statusCode
                backup.onGoogleAuthFailed(
                    if (code == CommonStatusCodes.DEVELOPER_ERROR) {
                        "Google sign-in isn't set up for this build of Paisa"
                    } else {
                        "Couldn't reach Google: ${e.message ?: "unknown error"}"
                    }
                )
            }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupNames.MIME)
    ) { uri -> if (uri != null) backup.exportToFile(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) backup.restoreFromFile(uri) }

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
                StatusCard(
                    lastBackupAt = config.lastBackupAt,
                    lastBackupSize = config.lastBackupSize,
                    lastError = config.lastError,
                    providerLabel = if (connected) config.provider.label else null,
                    busy = busy,
                    canBackUp = connected && !isBusy,
                    onBackUp = { backup.backupNow() }
                )

                SettingsSectionLabel("Back up to")
                SettingsGroup {
                    val rows = if (connected) 3 else 2
                    SettingsItem(
                        title = "Google Drive",
                        subtitle = when {
                            config.provider == BackupProvider.GOOGLE_DRIVE ->
                                config.googleEmail ?: "Connected"
                            else -> "Hidden app folder in your Google account"
                        },
                        icon = Icons.Rounded.Cloud,
                        position = groupPositionOf(0, rows),
                        onClick = if (isBusy) null else ({ connectGoogle() }),
                        trailing = {
                            RadioButton(
                                selected = config.provider == BackupProvider.GOOGLE_DRIVE,
                                onClick = null
                            )
                        }
                    )
                    SettingsItem(
                        title = "Nextcloud",
                        subtitle = when {
                            nextcloudLoginUrl != null -> "Waiting for you to approve in the browser…"
                            config.provider == BackupProvider.NEXTCLOUD && config.nextcloud != null ->
                                "${config.nextcloud!!.loginName} · ${Uri.parse(config.nextcloud!!.serverUrl).host}"
                            else -> "Your own server, saved in a Paisa folder"
                        },
                        icon = Icons.Rounded.Storage,
                        position = groupPositionOf(1, rows),
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = if (isBusy) null else ({ showNextcloud = true }),
                        trailing = {
                            RadioButton(
                                selected = config.provider == BackupProvider.NEXTCLOUD,
                                onClick = null
                            )
                        }
                    )
                    if (connected) {
                        SettingsItem(
                            title = "Disconnect",
                            subtitle = "Stop backing up. Uploaded backups stay put.",
                            icon = Icons.AutoMirrored.Rounded.Logout,
                            position = groupPositionOf(2, rows),
                            iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                            iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                            onClick = if (isBusy) null else ({ showDisconnect = true })
                        )
                    }
                }

                SettingsSectionLabel("Sync between devices")
                SettingsGroup {
                    val sync = config.sync
                    val rows = if (sync.enabled) 2 else 1
                    SettingsItem(
                        title = "Sync between devices",
                        subtitle = when {
                            !connected -> "Connect Google Drive or Nextcloud first"
                            sync.enabled && sync.error != null -> sync.error
                            sync.enabled && sync.lastSyncAt > 0 -> "On · last synced ${formatDate(sync.lastSyncAt)}"
                            sync.enabled -> "On"
                            else -> "Use the same Paisa data on your phone, tablet or a second phone"
                        },
                        icon = Icons.Rounded.Sync,
                        position = groupPositionOf(0, rows),
                        iconContainerColor = if (sync.enabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        iconContentColor = if (sync.enabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        onClick = if (!connected || isBusy) null else ({
                            if (sync.enabled) showDisableSync = true else showSyncPassword = true
                        }),
                        trailing = {
                            Switch(
                                checked = sync.enabled,
                                enabled = connected && !isBusy,
                                onCheckedChange = { on ->
                                    if (on) showSyncPassword = true else showDisableSync = true
                                }
                            )
                        }
                    )
                    if (sync.enabled) {
                        SettingsItem(
                            title = "Sync now",
                            subtitle = if (sync.peers.isEmpty()) {
                                "No other devices yet"
                            } else {
                                "With ${sync.peers.joinToString(", ")}"
                            },
                            icon = Icons.Rounded.Devices,
                            position = groupPositionOf(1, rows),
                            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = if (isBusy) null else ({ backup.syncNow() })
                        )
                    }
                }

                SettingsSectionLabel("Automatic backups")
                SettingsGroup {
                    SettingsItem(
                        title = "Back up",
                        subtitle = config.frequency.label,
                        icon = Icons.Rounded.Schedule,
                        position = groupPositionOf(0, 2),
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = { showFrequency = true }
                    )
                    SettingsItem(
                        title = "Back up using cellular",
                        subtitle = if (config.allowCellular) "Wi-Fi or mobile data" else "Wi-Fi only",
                        icon = Icons.Rounded.NetworkCell,
                        position = groupPositionOf(1, 2),
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { backup.setAllowCellular(!config.allowCellular) },
                        trailing = {
                            Switch(
                                checked = config.allowCellular,
                                onCheckedChange = { backup.setAllowCellular(it) }
                            )
                        }
                    )
                }

                SettingsSectionLabel("Encryption")
                SettingsGroup {
                    SettingsItem(
                        title = "Encrypted backup",
                        subtitle = if (config.encrypted) {
                            "On. Restoring on a new phone asks for your password."
                        } else {
                            "Off. Anyone with access to the file can read it."
                        },
                        icon = if (config.encrypted) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        position = groupPositionOf(0, 1),
                        iconContainerColor = if (config.encrypted) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        iconContentColor = if (config.encrypted) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        onClick = {
                            if (config.encrypted) showDisableEncryption = true else showSetPassword = true
                        },
                        trailing = {
                            Switch(
                                checked = config.encrypted,
                                onCheckedChange = { on ->
                                    if (on) showSetPassword = true else showDisableEncryption = true
                                }
                            )
                        }
                    )
                }

                SettingsSectionLabel("Restore")
                SettingsGroup {
                    val list = remote.orEmpty()
                    val count = list.size + 2
                    list.forEachIndexed { index, item ->
                        SettingsItem(
                            title = formatDate(item.createdAt),
                            subtitle = "${formatSize(item.sizeBytes)} · ${config.provider.label}",
                            icon = Icons.Rounded.History,
                            position = groupPositionOf(index, count),
                            onClick = if (isBusy) null else ({ confirmRestore = item }),
                            trailing = {
                                Text(
                                    "Restore",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                    SettingsItem(
                        title = "Restore from file",
                        subtitle = "Pick a .paisa file from any app or drive",
                        icon = Icons.Rounded.FolderOpen,
                        position = groupPositionOf(list.size, count),
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = if (isBusy) null else ({
                            viewModel.onExternalActivityLaunched()
                            importLauncher.launch(arrayOf("*/*"))
                        })
                    )
                    SettingsItem(
                        title = "Save backup to file",
                        subtitle = "Export a .paisa file you keep yourself",
                        icon = Icons.Rounded.Save,
                        position = groupPositionOf(list.size + 1, count),
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = if (isBusy) null else ({
                            viewModel.onExternalActivityLaunched()
                            exportLauncher.launch(BackupNames.forTime(System.currentTimeMillis()))
                        })
                    )
                }

                Spacer(Modifier.height(MaterialTheme.spacing.medium))
                Text(
                    "A backup holds every account, category, transaction (including the SMS " +
                        "text it came from), budget, goal and merchant rule. AI keys and the " +
                        "Nextcloud password are never included. Paisa keeps the newest " +
                        "${BackupManager.KEEP} backups and deletes older ones. Restoring " +
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
        AlertDialog(
            onDismissRequest = { showFrequency = false },
            title = { Text("Back up") },
            text = {
                Column {
                    BackupFrequency.entries.forEach { f ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    backup.setFrequency(f)
                                    showFrequency = false
                                }
                                .padding(vertical = MaterialTheme.spacing.smaller),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = config.frequency == f, onClick = null)
                            Spacer(Modifier.size(MaterialTheme.spacing.small))
                            Text(f.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if (!connected) {
                        Text(
                            "Connect Google Drive or Nextcloud for this to take effect.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = MaterialTheme.spacing.smaller)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFrequency = false }) { Text("Done") } }
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
        AlertDialog(
            onDismissRequest = { showDisableEncryption = false },
            title = { Text("Turn off encryption?") },
            text = {
                Text(
                    "New backups will be readable by anyone who can open the file, " +
                        "including your cloud provider. Backups you already made still " +
                        "need the old password."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDisableEncryption = false
                    backup.disableEncryption()
                }) { Text("Turn off") }
            },
            dismissButton = {
                TextButton(onClick = { showDisableEncryption = false }) { Text("Cancel") }
            }
        )
    }

    if (showDisconnect) {
        AlertDialog(
            onDismissRequest = { showDisconnect = false },
            title = { Text("Disconnect ${config.provider.label}?") },
            text = {
                Text(
                    "Automatic backups stop. Backups already uploaded stay in your " +
                        "account, and you can restore them after connecting again."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDisconnect = false
                    backup.disconnect()
                }) { Text("Disconnect") }
            },
            dismissButton = { TextButton(onClick = { showDisconnect = false }) { Text("Cancel") } }
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
        AlertDialog(
            onDismissRequest = { showDisableSync = false },
            title = { Text("Turn off sync?") },
            text = {
                Text(
                    "This device stops sending and receiving changes. Your data stays " +
                        "on this device, and your other devices keep what they have."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDisableSync = false
                    backup.disableSync()
                }) { Text("Turn off") }
            },
            dismissButton = { TextButton(onClick = { showDisableSync = false }) { Text("Cancel") } }
        )
    }

    confirmRestore?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            icon = { androidx.compose.material3.Icon(Icons.Rounded.Restore, null) },
            title = { Text("Restore this backup?") },
            text = {
                Text(
                    "Everything in Paisa on this phone is replaced with the backup from " +
                        "${formatDate(item.createdAt)}. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = null
                    backup.restoreRemote(item)
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text("Cancel") } }
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
            onDismiss = { showNextcloud = false },
            onBrowserLogin = { backup.connectNextcloud(it) },
            onManualLogin = { server, user, pw ->
                showNextcloud = false
                backup.connectNextcloudManual(server, user, pw)
            }
        )
    }

    if (nextcloudLoginUrl != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Approve in Nextcloud") },
            text = {
                Text(
                    "Log in and tap \"Grant access\" in the browser, then come back. " +
                        "Paisa connects as soon as you approve."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onExternalActivityLaunched()
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(nextcloudLoginUrl)))
                }) { Text("Open again") }
            },
            dismissButton = {
                TextButton(onClick = { backup.cancelNextcloudLogin() }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatusCard(
    lastBackupAt: Long,
    lastBackupSize: Long,
    lastError: String?,
    providerLabel: String?,
    busy: String?,
    canBackUp: Boolean,
    onBackUp: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                androidx.compose.material3.Icon(
                    Icons.Rounded.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Last backup",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (lastBackupAt > 0) {
                            "${formatDate(lastBackupAt)} · ${formatSize(lastBackupSize)}"
                        } else {
                            "Never"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text(
                text = if (providerLabel != null) {
                    "Backs up your Paisa data to $providerLabel so you can restore it " +
                        "on a new phone or after reinstalling."
                } else {
                    "Pick Google Drive or Nextcloud below to start backing up."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (lastError != null) {
                Text(
                    text = lastError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (busy != null) {
                Text(
                    busy,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                Button(onClick = onBackUp, enabled = canBackUp) {
                    androidx.compose.material3.Icon(
                        Icons.Rounded.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(MaterialTheme.spacing.smaller))
                    Text("Back up", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SetPasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val tooShort = password.length < MIN_PASSWORD
    val mismatch = confirm.isNotEmpty() && confirm != password
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { androidx.compose.material3.Icon(Icons.Rounded.Lock, null) },
        title = { Text("Create a backup password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
                Text(
                    "Paisa can't reset this password. If you forget it, your encrypted " +
                        "backups can't be restored.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = { Text("At least $MIN_PASSWORD characters") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("Confirm password") },
                    singleLine = true,
                    isError = mismatch,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = if (mismatch) ({ Text("Passwords don't match") }) else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !tooShort && confirm == password,
                onClick = { onConfirm(password) }
            ) { Text("Turn on") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SyncPasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val tooShort = password.length < MIN_SYNC_PASSWORD
    val mismatch = confirm.isNotEmpty() && confirm != password
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { androidx.compose.material3.Icon(Icons.Rounded.Sync, null) },
        title = { Text("Sync password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
                Text(
                    "Use the same password on every device. It encrypts your data before " +
                        "it leaves this phone. Paisa can't reset it.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = { Text("At least $MIN_SYNC_PASSWORD characters") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("Confirm password") },
                    singleLine = true,
                    isError = mismatch,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = if (mismatch) ({ Text("Passwords don't match") }) else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !tooShort && confirm == password,
                onClick = { onConfirm(password) }
            ) { Text("Turn on") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EnterPasswordDialog(label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { androidx.compose.material3.Icon(Icons.Rounded.Lock, null) },
        title = { Text("Enter backup password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
                Text(
                    "The backup from $label is encrypted. Restoring replaces everything " +
                        "in Paisa on this phone.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = password.isNotEmpty(), onClick = { onConfirm(password) }) {
                Text("Restore")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NextcloudSheet(
    initialServer: String,
    onDismiss: () -> Unit,
    onBrowserLogin: (server: String) -> Unit,
    onManualLogin: (server: String, user: String, appPassword: String) -> Unit
) {
    var server by remember { mutableStateOf(initialServer) }
    var manual by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf("") }
    var appPassword by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.larger),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text("Connect Nextcloud", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Backups go to a Paisa folder in your Nextcloud files.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = server,
                onValueChange = { server = it },
                label = { Text("Server address") },
                placeholder = { Text("cloud.example.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            if (!manual) {
                Button(
                    onClick = { onBrowserLogin(server) },
                    enabled = server.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Log in with browser") }
                TextButton(onClick = { manual = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Use an app password instead")
                }
            } else {
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = appPassword,
                    onValueChange = { appPassword = it },
                    label = { Text("App password") },
                    supportingText = { Text("Nextcloud → Settings → Security → Create new app password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                FilledTonalButton(
                    onClick = { onManualLogin(server, user, appPassword) },
                    enabled = server.isNotBlank() && user.isNotBlank() && appPassword.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Connect") }
            }
        }
    }
}

private const val MIN_PASSWORD = 6

/** Longer than the backup minimum: sync files sit in the cloud indefinitely. */
private const val MIN_SYNC_PASSWORD = 8
