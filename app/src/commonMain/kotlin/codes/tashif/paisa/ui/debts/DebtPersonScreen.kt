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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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


/**
 * One person's page: their balance, quick add buttons, settle up, and the
 * history. A linked entry opens its transaction; one added by hand opens an edit sheet.
 */
@Composable
internal fun DebtPersonScreen(
    entry: DebtPersonBalance,
    entries: List<DebtEntry>,
    currency: String,
    onBack: () -> Unit,
    onAddEntry: (amount: Double, note: String) -> Unit,
    onUpdateEntry: (id: Int, amount: Double, note: String, entryDate: String) -> Unit,
    onDeleteEntry: (Int) -> Unit,
    onOpenTransaction: (transactionSyncId: String) -> Unit,
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
    var editing by remember { mutableStateOf<DebtEntry?>(null) }

    Box(Modifier.fillMaxSize()) {
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
                // Room for the floating toolbar over the end of the list.
                bottom = 120.dp
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
                    onClick = {
                        haptics.click()
                        val syncId = e.transactionSyncId
                        if (syncId != null) onOpenTransaction(syncId) else editing = e
                    }
                )
            }
        }
    }
        DebtActionsToolbar(
            canSettle = abs(entry.balance) >= SETTLED,
            onTheyOwe = {
                haptics.click()
                adding = true
            },
            onIOwe = {
                haptics.click()
                adding = false
            },
            onSettle = {
                haptics.click()
                confirmSettle = true
            },
            modifier = Modifier
                // The app frame already pads for the bottom bar and gesture bar.
                .align(Alignment.BottomCenter)
                .padding(bottom = MaterialTheme.spacing.medium)
        )
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

    editing?.let { e ->
        EditDebtEntrySheet(
            entry = e,
            onDismiss = { editing = null },
            onSave = { amount, note, date ->
                haptics.confirm()
                onUpdateEntry(e.id, amount, note, date)
                editing = null
            },
            onDelete = {
                haptics.reject()
                onDeleteEntry(e.id)
                editing = null
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

/**
 * The person's actions in an M3 floating toolbar at the bottom: add to
 * either side of the balance, with settle up as the toolbar's own FAB
 * while there's anything to settle.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DebtActionsToolbar(
    canSettle: Boolean,
    onTheyOwe: () -> Unit,
    onIOwe: () -> Unit,
    onSettle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val content: @Composable RowScope.() -> Unit = {
        TextButton(onClick = onTheyOwe) {
            Icon(Icons.Rounded.CallReceived, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("They owe me")
        }
        TextButton(onClick = onIOwe) {
            Icon(Icons.Rounded.CallMade, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("I owe them")
        }
    }
    if (canSettle) {
        HorizontalFloatingToolbar(
            expanded = true,
            floatingActionButton = {
                TooltipFab(label = "Settle up") {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = onSettle) {
                        Icon(Icons.Rounded.Handshake, contentDescription = "Settle up")
                    }
                }
            },
            modifier = modifier,
            content = content
        )
    } else {
        HorizontalFloatingToolbar(expanded = true, modifier = modifier, content = content)
    }
}

/** A plain tooltip naming an icon-only button, shown on long press or hover. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipFab(label: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        content = content
    )
}
