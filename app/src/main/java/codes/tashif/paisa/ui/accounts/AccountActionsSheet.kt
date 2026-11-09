package codes.tashif.paisa.ui.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountActionsSheet(
    account: Account,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onUpdateBalance: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit
) {
    val actionCount = if (account.isDefault) 3 else 4
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.large)
        ) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    start = MaterialTheme.spacing.small,
                    bottom = MaterialTheme.spacing.smaller
                )
            )
            SettingsGroup {
                SettingsItem(
                    title = "Rename",
                    subtitle = null,
                    icon = Icons.Rounded.Edit,
                    position = groupPositionOf(0, actionCount),
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onRename
                )
                SettingsItem(
                    title = "Update balance",
                    subtitle = "Correct the current balance",
                    icon = Icons.Rounded.Payments,
                    position = groupPositionOf(1, actionCount),
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onUpdateBalance
                )
                if (!account.isDefault) {
                    SettingsItem(
                        title = "Set as default",
                        subtitle = "Preselected when adding transactions",
                        icon = Icons.Rounded.Star,
                        position = groupPositionOf(2, actionCount),
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = onSetDefault
                    )
                }
                SettingsItem(
                    title = "Delete",
                    subtitle = "Removes the account and all its transactions",
                    icon = Icons.Rounded.Delete,
                    position = groupPositionOf(actionCount - 1, actionCount),
                    iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                    iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = onDelete
                )
            }
        }
    }
}
