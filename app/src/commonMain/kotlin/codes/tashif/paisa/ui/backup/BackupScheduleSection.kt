package codes.tashif.paisa.ui.backup

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf

/** "Automatic backups": how often, and whether mobile data is allowed. */
@Composable
internal fun BackupScheduleSection(
    frequencyLabel: String,
    allowCellular: Boolean,
    onOpenFrequency: () -> Unit,
    onAllowCellularChange: (Boolean) -> Unit
) {
    SettingsSectionLabel("Automatic backups")
    SettingsGroup {
        SettingsItem(
            title = "Back up",
            subtitle = frequencyLabel,
            icon = Icons.Rounded.Schedule,
            position = groupPositionOf(0, 2),
            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onOpenFrequency
        )
        SettingsItem(
            title = "Back up using cellular",
            subtitle = if (allowCellular) "Wi-Fi or mobile data" else "Wi-Fi only",
            icon = Icons.Rounded.NetworkCell,
            position = groupPositionOf(1, 2),
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = { onAllowCellularChange(!allowCellular) },
            trailing = {
                Switch(
                    checked = allowCellular,
                    onCheckedChange = { onAllowCellularChange(it) }
                )
            }
        )
    }
}
