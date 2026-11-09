package codes.tashif.paisa.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.DebtEntry
import codes.tashif.paisa.data.DebtPerson
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.data.PaisaViewModel
import codes.tashif.paisa.ui.components.ChoiceCards
import codes.tashif.paisa.ui.components.ChoiceOption
import codes.tashif.paisa.ui.components.EmptyState
import codes.tashif.paisa.ui.components.PaisaTopBar
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.components.groupShape
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen
import codes.tashif.paisa.ui.theme.spacing
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

/** Below this, a balance counts as settled (floating point dust). */
private const val SETTLED = 0.005

/** "Owes you", "You owe" or "Settled" for a balance. */
internal fun debtStatus(balance: Double): String = when {
    balance >= SETTLED -> "Owes you"
    balance <= -SETTLED -> "You owe"
    else -> "Settled"
}

/** Totals across everyone: what's owed to you, and what you owe (both positive). */
internal fun debtTotals(people: List<DebtPersonBalance>): Pair<Double, Double> =
    people.sumOf { it.balance.coerceAtLeast(0.0) } to people.sumOf { (-it.balance).coerceAtLeast(0.0) }

/** Money between you and other people. Lives apart from accounts, so it never moves a balance. */
@Composable
fun DebtsScreen(viewModel: PaisaViewModel) {
    val people by viewModel.debtPeople.collectAsState()
    val openId by viewModel.openDebtPersonId.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currency ?: "₹"

    val open = people.firstOrNull { it.person.id == openId }
    if (open != null) {
        BackHandler { viewModel.openDebtPerson(null) }
        DebtPersonScreen(viewModel = viewModel, entry = open, currency = currency)
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PaisaTopBar(title = "Debts")
        if (people.isEmpty()) {
            EmptyState(
                title = "Nobody owes anything",
                subtitle = "Add someone with +, or mark a transaction with " +
                    "\"Someone owes me for this\""
            )
            return@Column
        }
        val (owedToMe, iOwe) = debtTotals(people)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                bottom = MaterialTheme.spacing.extraLarge * 3
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                DebtTotalsCard(currency = currency, owedToMe = owedToMe, iOwe = iOwe)
                Spacer(Modifier.height(MaterialTheme.spacing.medium))
            }
            itemsIndexed(people, key = { _, p -> p.person.id }) { index, p ->
                DebtPersonRow(
                    entry = p,
                    currency = currency,
                    index = index,
                    count = people.size,
                    onClick = { viewModel.openDebtPerson(p.person.id) }
                )
            }
        }
    }
}

@Composable
internal fun DebtTotalsCard(currency: String, owedToMe: Double, iOwe: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatMoney(currency, owedToMe),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen
                )
                Text(
                    text = "owed to you",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatMoney(currency, iOwe),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (iOwe >= SETTLED) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "you owe",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DebtPersonRow(
    entry: DebtPersonBalance,
    currency: String,
    index: Int,
    count: Int,
    onClick: () -> Unit
) {
    val haptics = rememberHaptics()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.click()
                onClick()
            },
        shape = groupShape(groupPositionOf(index, count)),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small + 2.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.person.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(Modifier.width(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.person.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = debtStatus(entry.balance),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatMoney(currency, abs(entry.balance)),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = balanceColor(entry.balance)
            )
        }
    }
}

@Composable
private fun balanceColor(balance: Double): Color = when {
    balance >= SETTLED -> IncomeGreen
    balance <= -SETTLED -> ExpenseRed
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtPersonScreen(
    viewModel: PaisaViewModel,
    entry: DebtPersonBalance,
    currency: String
) {
    val person = entry.person
    val entries by viewModel.openDebtEntries.collectAsState()
    val haptics = rememberHaptics()
    var adding by remember { mutableStateOf<Boolean?>(null) } // true: they owe more, false: less
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmSettle by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = MaterialTheme.spacing.smaller, vertical = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.openDebtPerson(null) }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = person.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More for ${person.name}")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            renaming = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            confirmDelete = true
                        }
                    )
                }
            }
        }

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
                        viewModel.deleteDebtEntry(e.id)
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
                viewModel.addDebtEntry(person.id, if (theyOwe) amount else -amount, note)
                adding = null
            }
        )
    }

    if (confirmSettle) {
        AlertDialog(
            onDismissRequest = { confirmSettle = false },
            title = { Text("Settle up with ${person.name}?") },
            text = {
                Text("Adds one entry for ${formatMoney(currency, abs(entry.balance))} so you're even. History stays.")
            },
            confirmButton = {
                TextButton(onClick = {
                    haptics.confirm()
                    viewModel.settleDebt(person)
                    confirmSettle = false
                }) { Text("Settle up") }
            },
            dismissButton = { TextButton(onClick = { confirmSettle = false }) { Text("Cancel") } }
        )
    }

    if (renaming) {
        RenameDebtPersonDialog(
            person = person,
            onDismiss = { renaming = false },
            onSave = {
                viewModel.renameDebtPerson(person, it)
                renaming = false
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${person.name}?") },
            text = { Text("Removes them and their whole history from Debts. Your transactions stay as they are.") },
            confirmButton = {
                TextButton(onClick = {
                    haptics.reject()
                    viewModel.deleteDebtPerson(person)
                    confirmDelete = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DebtEntryRow(
    entry: DebtEntry,
    currency: String,
    index: Int,
    count: Int,
    onDelete: () -> Unit
) {
    val up = entry.amount >= 0
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = groupShape(groupPositionOf(index, count)),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.medium,
                top = MaterialTheme.spacing.small,
                bottom = MaterialTheme.spacing.small
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (up) Icons.AutoMirrored.Rounded.CallMade else Icons.AutoMirrored.Rounded.CallReceived,
                contentDescription = if (up) "They owe more" else "They owe less",
                tint = if (up) IncomeGreen else ExpenseRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.note.ifBlank { if (up) "Lent" else "Paid back" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = displayDate(entry.entryDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = (if (up) "+" else "−") + formatMoney(currency, abs(entry.amount)),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (up) IncomeGreen else ExpenseRed
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun displayDate(iso: String): String = runCatching {
    val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(iso)
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(parsed!!)
}.getOrDefault(iso.take(10))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDebtEntrySheet(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onSave: (amount: Double, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull()?.takeIf { it > 0 }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = amountText,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amountText = it },
                label = { Text("Amount") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("What for (optional)") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { amount?.let { onSave(it, note) } },
                enabled = amount != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) { Text("Save", fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** New person from the Debts tab's + button, with an optional starting amount either way. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtPersonSheet(
    viewModel: PaisaViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var theyOwe by remember { mutableStateOf(true) }
    val haptics = rememberHaptics()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text("Add person", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Their name") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            )
            ChoiceCards(
                options = listOf(
                    ChoiceOption("owes", "They owe me", Icons.AutoMirrored.Rounded.CallMade),
                    ChoiceOption("owe", "I owe them", Icons.AutoMirrored.Rounded.CallReceived)
                ),
                selectedId = if (theyOwe) "owes" else "owe",
                onSelect = { theyOwe = it == "owes" }
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amountText = it },
                label = { Text("How much (optional)") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    haptics.confirm()
                    val amount = abs(amountText.toDoubleOrNull() ?: 0.0)
                    viewModel.addDebtPerson(name, if (theyOwe) amount else -amount)
                    onDismiss()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) { Text("Add", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun RenameDebtPersonDialog(
    person: DebtPerson,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember(person.id) { mutableStateOf(person.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Name") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
