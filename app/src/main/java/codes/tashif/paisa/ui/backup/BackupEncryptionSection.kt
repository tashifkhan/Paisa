package codes.tashif.paisa.ui.backup

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf

/** "Encryption": the backup password switch. */
@Composable
internal fun BackupEncryptionSection(
    encrypted: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit
) {
    SettingsSectionLabel("Encryption")
    SettingsGroup {
        SettingsItem(
            title = "Encrypted backup",
            subtitle = if (encrypted) {
                "On. Restoring on a new phone asks for your password."
            } else {
                "Off. Anyone with access to the file can read it."
            },
            icon = if (encrypted) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
            position = groupPositionOf(0, 1),
            iconContainerColor = if (encrypted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            iconContentColor = if (encrypted) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            onClick = {
                if (encrypted) onDisable() else onEnable()
            },
            trailing = {
                Switch(
                    checked = encrypted,
                    onCheckedChange = { on ->
                        if (on) onEnable() else onDisable()
                    }
                )
            }
        )
    }
}
