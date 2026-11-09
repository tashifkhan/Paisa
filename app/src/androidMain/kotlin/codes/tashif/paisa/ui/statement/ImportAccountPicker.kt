package codes.tashif.paisa.ui.statement

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.ui.components.AccountVisuals
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/** "Import into account" dropdown. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportAccountPicker(
    accounts: List<Account>,
    selectedAccount: Account?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (accountId: Int) -> Unit
) {
    val haptics = rememberHaptics()
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium)
    ) {
        OutlinedTextField(
            value = selectedAccount?.name ?: "Select account",
            onValueChange = {},
            readOnly = true,
            label = { Text("Import into account") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = { Text(acc.name) },
                    onClick = {
                        haptics.tick()
                        onSelect(acc.id)
                        onExpandedChange(false)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = AccountVisuals.icon(acc.type, acc.icon),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}
