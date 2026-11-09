package codes.tashif.paisa.ui.backup

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import codes.tashif.paisa.backup.BackupConfig
import codes.tashif.paisa.backup.BackupProvider
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf

/** "Back up to": Google Drive, Nextcloud, and Disconnect once one is set up. */
@Composable
internal fun BackupDestinationSection(
    config: BackupConfig,
    nextcloudLoginUrl: String?,
    isBusy: Boolean,
    onConnectGoogle: () -> Unit,
    onOpenNextcloud: () -> Unit,
    onDisconnect: () -> Unit
) {
    val connected = config.provider != BackupProvider.NONE
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
            onClick = if (isBusy) null else onConnectGoogle,
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
                    "${config.nextcloud!!.loginName} · ${urlHost(config.nextcloud!!.serverUrl)}"
                else -> "Your own server, saved in a Paisa folder"
            },
            icon = Icons.Rounded.Storage,
            position = groupPositionOf(1, rows),
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = if (isBusy) null else onOpenNextcloud,
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
                onClick = if (isBusy) null else onDisconnect
            )
        }
    }
}
