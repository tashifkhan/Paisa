package codes.tashif.paisa.ui.notifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.notifications.AwaitingAccount
import codes.tashif.paisa.ui.components.SheetToggleCard
import codes.tashif.paisa.ui.format.formatMoneyAmount
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/**
 * "Which account paid?" for a payment an app reported without saying. The
 * "always" switch starts on: a UPI app usually pays from the same account.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseAccountSheet(
    item: AwaitingAccount,
    accounts: List<Account>,
    currency: String,
    onChoose: (account: Account, remember: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberHaptics()
    var selected by remember(item.transaction.id) { mutableStateOf<Account?>(null) }
    var remember by remember(item.transaction.id) { mutableStateOf(true) }
    val tx = item.transaction
    val from = item.last4?.let { "${item.appName} ••$it" } ?: item.appName

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.medium)
        ) {
            Text(item.question, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "${formatMoneyAmount(currency, tx.amount)} · ${tx.merchantName ?: "Unknown"} · from $from",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (accounts.isEmpty()) {
                Text(
                    "Add your bank account from the Accounts tab first.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium)
                )
                return@Column
            }
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp).padding(top = MaterialTheme.spacing.small)) {
                items(accounts, key = { it.id }) { account ->
                    val isSelected = selected?.id == account.id
                    ListItem(
                        headlineContent = { Text(account.name) },
                        supportingContent = {
                            Text(listOfNotNull(account.type, account.accountLast4?.let { "••$it" }).joinToString(" · "))
                        },
                        leadingContent = { RadioButton(selected = isSelected, onClick = null) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = {
                                haptics.click()
                                selected = account
                            }
                        )
                    )
                }
            }
            SheetToggleCard(
                icon = Icons.Rounded.Link,
                title = "Always use for $from",
                subtitle = "Moves its other waiting payments too",
                checked = remember,
                onCheckedChange = { remember = it },
                modifier = Modifier.padding(top = MaterialTheme.spacing.small)
            )
            Button(
                onClick = {
                    val account = selected ?: return@Button
                    haptics.confirm()
                    onChoose(account, remember)
                },
                enabled = selected != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.small)
            ) {
                Text(selected?.let { "Use ${it.name}" } ?: "Pick an account")
            }
        }
    }
}

/** On a transaction that waits for its account: says so, and opens [ChooseAccountSheet]. */
@Composable
fun AwaitingAccountCard(item: AwaitingAccount, onChoose: () -> Unit) {
    val from = item.last4?.let { "${item.appName} ••$it" } ?: item.appName
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.question,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    "$from didn't say which account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            FilledTonalButton(onClick = onChoose, modifier = Modifier.padding(start = MaterialTheme.spacing.small)) {
                Text("Choose")
            }
        }
    }
}
