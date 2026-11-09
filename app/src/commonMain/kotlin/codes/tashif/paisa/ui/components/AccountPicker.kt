package codes.tashif.paisa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/** Collapsed account field + searchable sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPicker(
    accounts: List<Account>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    emptyText: String = "Add an account first from the Accounts tab",
    label: String = "Account"
) {
    val selected = accounts.firstOrNull { it.id == selectedId }
    var showSheet by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    if (accounts.isEmpty()) {
        Text(
            text = emptyText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = MaterialTheme.spacing.smaller)
        )
        return
    }

    PickerCollapsedRow(
        modifier = modifier,
        label = label,
        title = selected?.name ?: "Select account",
        subtitle = selected?.type,
        placeholder = selected == null,
        onClick = {
            haptics.click()
            showSheet = true
        },
        leading = {
            if (selected != null) {
                val accent = AccountVisuals.color(selected.color)
                    ?: MaterialTheme.colorScheme.primaryContainer
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AccountVisuals.icon(selected.type, selected.icon),
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                PickerPlaceholderIcon(Icons.Rounded.AccountBalanceWallet)
            }
        }
    )

    if (showSheet) {
        SearchablePickerSheet(
            title = "Choose account",
            searchPlaceholder = "Search accounts",
            onDismiss = { showSheet = false }
        ) { query ->
            val filtered = remember(accounts, query) {
                val q = query.trim()
                if (q.isEmpty()) accounts
                else accounts.filter {
                    it.name.contains(q, ignoreCase = true) ||
                        it.type.contains(q, ignoreCase = true)
                }
            }
            if (filtered.isEmpty()) {
                PickerEmptySearch(query)
            } else {
                SettingsGroup {
                    filtered.forEachIndexed { index, account ->
                        val accent = AccountVisuals.color(account.color)
                            ?: MaterialTheme.colorScheme.primaryContainer
                        SelectableOptionRow(
                            title = account.name,
                            subtitle = account.type,
                            selected = selectedId == account.id,
                            position = groupPositionOf(index, filtered.size),
                            onClick = {
                                onSelect(account.id)
                                showSheet = false
                            },
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(accent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = AccountVisuals.icon(
                                            account.type,
                                            account.icon
                                        ),
                                        contentDescription = null,
                                        tint = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
