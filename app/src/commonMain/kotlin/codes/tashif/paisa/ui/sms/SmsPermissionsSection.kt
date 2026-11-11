package codes.tashif.paisa.ui.sms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MarkChatRead
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/**
 * Permission rows in the settings-list style, then the scan actions. Emits its
 * children without a wrapper so it lays out the same inside a LazyColumn item.
 * A missing permission's row is tappable and opens the system prompt.
 */
@Composable
fun SmsPermissionsSection(
    hasReadSms: Boolean,
    hasReceiveSms: Boolean,
    hasNotifications: Boolean,
    isScanning: Boolean,
    onGrantAndScan: () -> Unit,
    onScanNew: () -> Unit,
    onFullRescan: () -> Unit,
    onCancelScan: () -> Unit,
    onMarkScanned: () -> Unit = {}
) {
    val haptics = rememberHaptics()
    val request = {
        haptics.click()
        onGrantAndScan()
    }

    SettingsSectionLabel("Permissions")
    SettingsGroup {
        PermissionItem(
            index = 0,
            icon = Icons.Rounded.Sms,
            title = "Read SMS",
            purpose = "Finds bank messages already in your inbox",
            granted = hasReadSms,
            missingLabel = "Needed",
            onRequest = request
        )
        PermissionItem(
            index = 1,
            icon = Icons.Rounded.MarkChatRead,
            title = "Receive SMS",
            purpose = "Adds new transactions the moment the SMS arrives",
            granted = hasReceiveSms,
            missingLabel = "Needed",
            onRequest = request
        )
        PermissionItem(
            index = 2,
            icon = Icons.Rounded.Notifications,
            title = "Notifications",
            purpose = "Tells you when Paisa adds something",
            granted = hasNotifications,
            missingLabel = "Optional",
            onRequest = request
        )
    }

    Spacer(Modifier.height(MaterialTheme.spacing.medium))
    when {
        !hasReadSms -> Button(
            onClick = request,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            ButtonContent(Icons.Rounded.Sms, "Allow SMS access and scan")
        }

        isScanning -> OutlinedButton(
            onClick = {
                haptics.reject()
                onCancelScan()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            ButtonContent(Icons.Rounded.Stop, "Stop scan")
        }

        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                Button(
                    onClick = {
                        haptics.click()
                        onScanNew()
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    ButtonContent(Icons.Rounded.Sync, "Scan new")
                }
                OutlinedButton(
                    onClick = {
                        haptics.click()
                        onFullRescan()
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    ButtonContent(Icons.Rounded.History, "Full rescan")
                }
            }
            Text(
                "Scan new checks messages since the last scan. Full rescan reads the whole " +
                    "inbox again and skips anything already saved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = MaterialTheme.spacing.small,
                    end = MaterialTheme.spacing.small,
                    top = MaterialTheme.spacing.smaller
                )
            )
            TextButton(
                onClick = {
                    haptics.click()
                    onMarkScanned()
                },
                modifier = Modifier.padding(start = MaterialTheme.spacing.extraSmall)
            ) {
                Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Mark inbox as scanned up to now")
            }
        }
    }
}

@Composable
private fun ButtonContent(icon: ImageVector, label: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(MaterialTheme.spacing.smaller))
    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun PermissionItem(
    index: Int,
    icon: ImageVector,
    title: String,
    purpose: String,
    granted: Boolean,
    missingLabel: String,
    onRequest: () -> Unit
) {
    SettingsItem(
        title = title,
        subtitle = purpose,
        icon = icon,
        position = groupPositionOf(index, 3),
        iconContainerColor = if (granted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        iconContentColor = if (granted) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        onClick = if (granted) null else onRequest,
        trailing = {
            if (granted) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Allowed",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    missingLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (missingLabel == "Needed") {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    )
}
