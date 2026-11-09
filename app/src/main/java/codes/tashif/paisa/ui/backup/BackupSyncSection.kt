package codes.tashif.paisa.ui.backup

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import codes.tashif.paisa.backup.SyncStatus
import codes.tashif.paisa.ui.backup.BackupViewModel.Companion.formatDate
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf

/** "Sync between devices": the on/off switch, then Sync now while it's on. */
@Composable
internal fun BackupSyncSection(
    sync: SyncStatus,
    connected: Boolean,
    isBusy: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onSyncNow: () -> Unit
) {
    SettingsSectionLabel("Sync between devices")
    SettingsGroup {
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
                if (sync.enabled) onDisable() else onEnable()
            }),
            trailing = {
                Switch(
                    checked = sync.enabled,
                    enabled = connected && !isBusy,
                    onCheckedChange = { on ->
                        if (on) onEnable() else onDisable()
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
                onClick = if (isBusy) null else onSyncNow
            )
        }
    }
}
