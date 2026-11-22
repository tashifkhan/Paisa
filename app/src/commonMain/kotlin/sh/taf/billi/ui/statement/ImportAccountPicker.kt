package sh.taf.billi.ui.statement

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
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
import sh.taf.billi.data.Account
import sh.taf.billi.ui.components.AccountVisuals
import sh.taf.billi.ui.haptics.rememberHaptics
import sh.taf.billi.ui.theme.spacing

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
        // A plain DropdownMenu: newer androidx Material 3 hides the scope's
        // ExposedDropdownMenu member that the web build still uses, so no
        // one call compiles on both.
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.exposedDropdownSize()
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
