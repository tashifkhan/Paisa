package codes.tashif.paisa.ui.backup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import codes.tashif.paisa.backup.BackupFrequency
import codes.tashif.paisa.backup.RemoteBackup
import codes.tashif.paisa.ui.backup.BackupViewModel.Companion.formatDate
import codes.tashif.paisa.ui.theme.spacing

/** Picks how often automatic backups run. Picking one closes the dialog. */
@Composable
internal fun BackupFrequencyDialog(
    selected: BackupFrequency,
    connected: Boolean,
    onSelect: (BackupFrequency) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Back up") },
        text = {
            Column {
                BackupFrequency.entries.forEach { f ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(f)
                                onDismiss()
                            }
                            .padding(vertical = MaterialTheme.spacing.smaller),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == f, onClick = null)
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
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
internal fun DisableEncryptionDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Turn off encryption?") },
        text = {
            Text(
                "New backups will be readable by anyone who can open the file, " +
                    "including your cloud provider. Backups you already made still " +
                    "need the old password."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Turn off") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun DisconnectDialog(providerLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Disconnect $providerLabel?") },
        text = {
            Text(
                "Automatic backups stop. Backups already uploaded stay in your " +
                    "account, and you can restore them after connecting again."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Disconnect") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun DisableSyncDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Turn off sync?") },
        text = {
            Text(
                "This device stops sending and receiving changes. Your data stays " +
                    "on this device, and your other devices keep what they have."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Turn off") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun ConfirmRestoreDialog(item: RemoteBackup, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Restore, null) },
        title = { Text("Restore this backup?") },
        text = {
            Text(
                "Everything in Paisa on this phone is replaced with the backup from " +
                    "${formatDate(item.createdAt)}. This can't be undone."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Restore") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun ConfirmDeleteBackupDialog(
    item: RemoteBackup,
    providerLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DeleteOutline, null) },
        title = { Text("Delete this backup?") },
        text = {
            Text(
                "The backup from ${formatDate(item.createdAt)} is removed from $providerLabel. " +
                    "Your data in Paisa stays as it is."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Shown while Paisa polls Nextcloud for the browser approval. Can't be dismissed by tapping outside. */
@Composable
internal fun NextcloudApproveDialog(onOpenAgain: () -> Unit, onCancel: () -> Unit) {
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
            TextButton(onClick = onOpenAgain) { Text("Open again") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}
