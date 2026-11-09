package codes.tashif.paisa.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.backup.BackupViewModel.Companion.formatDate
import codes.tashif.paisa.ui.backup.BackupViewModel.Companion.formatSize
import codes.tashif.paisa.ui.theme.spacing

/** Last backup, what it backs up to, and the big Back up button. */
@Composable
internal fun BackupStatusCard(
    lastBackupAt: Long,
    lastBackupSize: Long,
    lastError: String?,
    providerLabel: String?,
    busy: String?,
    canBackUp: Boolean,
    onBackUp: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                androidx.compose.material3.Icon(
                    Icons.Rounded.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Last backup",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (lastBackupAt > 0) {
                            "${formatDate(lastBackupAt)} · ${formatSize(lastBackupSize)}"
                        } else {
                            "Never"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text(
                text = if (providerLabel != null) {
                    "Backs up your Paisa data to $providerLabel so you can restore it " +
                        "on a new phone or after reinstalling."
                } else {
                    "Pick Google Drive or Nextcloud below to start backing up."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (lastError != null) {
                Text(
                    text = lastError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (busy != null) {
                Text(
                    busy,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                Button(onClick = onBackUp, enabled = canBackUp) {
                    androidx.compose.material3.Icon(
                        Icons.Rounded.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(MaterialTheme.spacing.smaller))
                    Text("Back up", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
