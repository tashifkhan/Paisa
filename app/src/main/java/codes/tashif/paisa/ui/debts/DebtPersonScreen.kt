package codes.tashif.paisa.ui.debts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.DebtEntry
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing
import kotlin.math.abs


/** One person's page: their balance, quick add buttons, settle up, and the history. */
@Composable
internal fun DebtPersonScreen(
    entry: DebtPersonBalance,
    entries: List<DebtEntry>,
    currency: String,
    onBack: () -> Unit,
    onAddEntry: (amount: Double, note: String) -> Unit,
    onDeleteEntry: (Int) -> Unit,
    onSettle: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    val person = entry.person
    val haptics = rememberHaptics()
    var adding by remember { mutableStateOf<Boolean?>(null) } // true: they owe more, false: less
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmSettle by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        DebtPersonTopBar(
            name = person.name,
            onBack = onBack,
            onRename = { renaming = true },
            onDelete = { confirmDelete = true }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                bottom = MaterialTheme.spacing.extraLarge
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                Text(
                    text = formatMoney(currency, abs(entry.balance)),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor(entry.balance)
                )
                Text(
                    text = when {
                        entry.balance >= SETTLED -> "${person.name} owes you"
                        entry.balance <= -SETTLED -> "You owe ${person.name}"
                        else -> "You're even"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(MaterialTheme.spacing.medium))
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    FilledTonalButton(onClick = { adding = true }, modifier = Modifier.weight(1f)) {
                        Text("They owe me")
                    }
                    FilledTonalButton(onClick = { adding = false }, modifier = Modifier.weight(1f)) {
                        Text("I owe them")
                    }
                }
                if (abs(entry.balance) >= SETTLED) {
                    OutlinedButton(
                        onClick = { confirmSettle = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MaterialTheme.spacing.smaller)
                    ) {
                        Icon(Icons.Rounded.Handshake, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(MaterialTheme.spacing.smaller))
                        Text("Settle up")
                    }
                }
                Spacer(Modifier.height(MaterialTheme.spacing.large))
                Text(
                    text = "History",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = MaterialTheme.spacing.smaller)
                )
                if (entries.isEmpty()) {
                    Text(
                        text = "Nothing yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            itemsIndexed(entries, key = { _, e -> e.id }) { index, e ->
                DebtEntryRow(
                    entry = e,
                    currency = currency,
                    index = index,
                    count = entries.size,
                    onDelete = {
                        haptics.reject()
                        onDeleteEntry(e.id)
                    }
                )
            }
        }
    }

    adding?.let { theyOwe ->
        AddDebtEntrySheet(
            title = if (theyOwe) "${person.name} owes you" else "You owe ${person.name}",
            subtitle = if (theyOwe) {
                "You lent money or paid for them"
            } else {
                "They paid you back, or you borrowed"
            },
            onDismiss = { adding = null },
            onSave = { amount, note ->
                haptics.confirm()
                onAddEntry(if (theyOwe) amount else -amount, note)
                adding = null
            }
        )
    }

    if (confirmSettle) {
        SettleDebtDialog(
            name = person.name,
            amount = formatMoney(currency, abs(entry.balance)),
            onDismiss = { confirmSettle = false },
            onConfirm = {
                onSettle()
                confirmSettle = false
            }
        )
    }

    if (renaming) {
        RenameDebtPersonDialog(
            person = person,
            onDismiss = { renaming = false },
            onSave = {
                onRename(it)
                renaming = false
            }
        )
    }

    if (confirmDelete) {
        DeleteDebtPersonDialog(
            name = person.name,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                onDelete()
                confirmDelete = false
            }
        )
    }
}
