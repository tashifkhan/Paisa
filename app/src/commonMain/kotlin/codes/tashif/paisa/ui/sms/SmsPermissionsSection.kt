package codes.tashif.paisa.ui.sms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/**
 * Permission status and the scan buttons. Emits its children without a
 * wrapper so it lays out the same inside a LazyColumn item.
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
    onCancelScan: () -> Unit
) {
    val haptics = rememberHaptics()

    Text(
        "Permissions",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(MaterialTheme.spacing.smaller))
    PermissionRow(label = "Read SMS", granted = hasReadSms, requiredHint = "Needed")
    PermissionRow(label = "Receive SMS", granted = hasReceiveSms, requiredHint = "Needed")
    PermissionRow(
        label = "Notifications",
        granted = hasNotifications,
        requiredHint = "Optional"
    )
    Spacer(Modifier.height(MaterialTheme.spacing.small))
    if (!hasReadSms) {
        Button(onClick = {
            haptics.click()
            onGrantAndScan()
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Grant SMS permission & scan")
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
        ) {
            Button(
                onClick = {
                    haptics.click()
                    onScanNew()
                },
                enabled = !isScanning,
                modifier = Modifier.weight(1f)
            ) {
                Text("Scan new")
            }
            OutlinedButton(
                onClick = {
                    haptics.click()
                    onFullRescan()
                },
                enabled = !isScanning,
                modifier = Modifier.weight(1f)
            ) {
                Text("Full rescan")
            }
        }
        if (isScanning) {
            TextButton(
                onClick = {
                    haptics.reject()
                    onCancelScan()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel scan")
            }
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    requiredHint: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) {
                Icons.Rounded.CheckCircle
            } else {
                Icons.Rounded.RadioButtonUnchecked
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (granted) "Granted" else requiredHint,
            style = MaterialTheme.typography.labelMedium,
            color = if (granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
