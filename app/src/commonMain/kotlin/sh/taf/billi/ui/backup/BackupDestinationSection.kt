package sh.taf.billi.ui.backup

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import sh.taf.billi.backup.BackupConfig
import sh.taf.billi.backup.BackupProvider
import sh.taf.billi.ui.components.SettingsGroup
import sh.taf.billi.ui.components.SettingsItem
import sh.taf.billi.ui.components.SettingsSectionLabel
import sh.taf.billi.ui.components.groupPositionOf

/** "Back up to": Google Drive where sign-in works, Nextcloud, and Disconnect once one is set up. */
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
        val showDrive = googleDriveSupported || config.provider == BackupProvider.GOOGLE_DRIVE
        val first = if (showDrive) 0 else -1
        val rows = (if (showDrive) 1 else 0) + 1 + (if (connected) 1 else 0)
        if (showDrive) SettingsItem(
            title = "Google Drive",
            subtitle = when {
                config.provider == BackupProvider.GOOGLE_DRIVE ->
                    config.googleEmail ?: "Connected"
                else -> "Hidden app folder in your Google account"
            },
            icon = Icons.Rounded.Cloud,
            position = groupPositionOf(first, rows),
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
                else -> "Your own server, saved in a Billi folder"
            },
            icon = Icons.Rounded.Storage,
            position = groupPositionOf(first + 1, rows),
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
                position = groupPositionOf(first + 2, rows),
                iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                onClick = if (isBusy) null else onDisconnect
            )
        }
    }
}
