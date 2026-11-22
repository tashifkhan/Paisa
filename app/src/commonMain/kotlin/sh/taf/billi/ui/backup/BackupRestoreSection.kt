package sh.taf.billi.ui.backup

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import sh.taf.billi.backup.RemoteBackup
import sh.taf.billi.ui.backup.BackupViewModel.Companion.formatDate
import sh.taf.billi.ui.backup.BackupViewModel.Companion.formatSize
import sh.taf.billi.ui.components.SettingsGroup
import sh.taf.billi.ui.components.SettingsItem
import sh.taf.billi.ui.components.SettingsSectionLabel
import sh.taf.billi.ui.components.groupPositionOf

/** "Restore": uploaded backups, then restore from a file and save to a file. */
@Composable
internal fun BackupRestoreSection(
    remote: List<RemoteBackup>,
    providerLabel: String,
    isBusy: Boolean,
    onRestoreRemote: (RemoteBackup) -> Unit,
    onDeleteRemote: (RemoteBackup) -> Unit,
    onRestoreFromFile: () -> Unit,
    onSaveToFile: () -> Unit
) {
    SettingsSectionLabel("Restore")
    SettingsGroup {
        val count = remote.size + 2
        remote.forEachIndexed { index, item ->
            SettingsItem(
                title = formatDate(item.createdAt),
                subtitle = "${formatSize(item.sizeBytes)} · $providerLabel",
                icon = Icons.Rounded.History,
                position = groupPositionOf(index, count),
                onClick = if (isBusy) null else ({ onRestoreRemote(item) }),
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Restore",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { onDeleteRemote(item) },
                            enabled = !isBusy
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete backup from ${formatDate(item.createdAt)}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
        SettingsItem(
            title = "Restore from file",
            subtitle = "Pick a .billi file, or a .paisa one from before the rename",
            icon = Icons.Rounded.FolderOpen,
            position = groupPositionOf(remote.size, count),
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = if (isBusy) null else onRestoreFromFile
        )
        SettingsItem(
            title = "Save backup to file",
            subtitle = "Export a .billi file you keep yourself",
            icon = Icons.Rounded.Save,
            position = groupPositionOf(remote.size + 1, count),
            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = if (isBusy) null else onSaveToFile
        )
    }
}
