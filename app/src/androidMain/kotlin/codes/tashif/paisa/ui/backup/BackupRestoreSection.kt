package codes.tashif.paisa.ui.backup

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import codes.tashif.paisa.backup.RemoteBackup
import codes.tashif.paisa.ui.backup.BackupViewModel.Companion.formatDate
import codes.tashif.paisa.ui.backup.BackupViewModel.Companion.formatSize
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf

/** "Restore": uploaded backups, then restore from a file and save to a file. */
@Composable
internal fun BackupRestoreSection(
    remote: List<RemoteBackup>,
    providerLabel: String,
    isBusy: Boolean,
    onRestoreRemote: (RemoteBackup) -> Unit,
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
            position = groupPositionOf(remote.size, count),
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = if (isBusy) null else onRestoreFromFile
        )
        SettingsItem(
            title = "Save backup to file",
            subtitle = "Export a .paisa file you keep yourself",
            icon = Icons.Rounded.Save,
            position = groupPositionOf(remote.size + 1, count),
            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = if (isBusy) null else onSaveToFile
        )
    }
}
