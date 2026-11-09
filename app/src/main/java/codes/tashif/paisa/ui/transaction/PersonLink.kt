package codes.tashif.paisa.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.ui.theme.spacing

/** A person to tie a transaction to: someone already in Debts, or a new name. */
data class PersonLink(
    val personId: Int?,
    val newName: String?,
    val amount: Double
)

/**
 * Pick who a transaction is with (someone already in Debts, or a new name)
 * and how much of it counts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonLinkFields(
    people: List<DebtPersonBalance>,
    selectedPersonId: Int?,
    onSelectPerson: (Int?) -> Unit,
    newName: String,
    onNewNameChange: (String) -> Unit,
    amountText: String,
    onAmountChange: (String) -> Unit,
    amountLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
    ) {
        if (people.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
                modifier = Modifier.fillMaxWidth()
            ) {
                people.forEach { entry ->
                    val person = entry.person
                    FilterChip(
                        selected = selectedPersonId == person.id,
                        onClick = { onSelectPerson(person.id) },
                        label = { Text(person.name) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Handshake, contentDescription = null)
                        }
                    )
                }
                FilterChip(
                    selected = selectedPersonId == null,
                    onClick = { onSelectPerson(null) },
                    label = { Text("New person") },
                    leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) }
                )
            }
        }
        if (selectedPersonId == null) {
            OutlinedTextField(
                value = newName,
                onValueChange = onNewNameChange,
                label = { Text("Their name") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            )
        }
        OutlinedTextField(
            value = amountText,
            onValueChange = { text ->
                if (text.isEmpty() || text.toDoubleOrNull() != null) onAmountChange(text)
            },
            label = { Text(amountLabel) },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** A [PersonLink] from the form, or null when the name or amount is missing. */
fun personLinkOrNull(personId: Int?, newName: String, amountText: String): PersonLink? {
    val amount = amountText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
    val name = newName.trim()
    if (personId == null && name.isEmpty()) return null
    return PersonLink(personId = personId, newName = name.takeIf { personId == null }, amount = amount)
}

/** Copy for the person option. Money out: they owe you. Money in: they paid you back, or lent it. */
fun personLinkTitle(isExpense: Boolean): String =
    if (isExpense) "Someone owes me for this" else "Someone paid me back or lent me this"

fun personLinkSubtitle(isExpense: Boolean): String =
    if (isExpense) {
        "Adds it to what they owe you in Debts"
    } else {
        "Lowers what they owe you in Debts, or adds to what you owe them"
    }

/** Dialog version for transactions that already exist, opened from the transaction screen. */
@Composable
fun PersonLinkDialog(
    people: List<DebtPersonBalance>,
    transactionAmount: Double,
    isExpense: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (PersonLink) -> Unit
) {
    var personId by remember { mutableStateOf(people.firstOrNull()?.person?.id) }
    var newName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf(formatPlain(transactionAmount)) }
    val link = personLinkOrNull(personId, newName, amountText)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Handshake, contentDescription = null) },
        title = { Text(personLinkTitle(isExpense)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                Text(
                    text = personLinkSubtitle(isExpense),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PersonLinkFields(
                    people = people,
                    selectedPersonId = personId,
                    onSelectPerson = { personId = it },
                    newName = newName,
                    onNewNameChange = { newName = it },
                    amountText = amountText,
                    onAmountChange = { amountText = it },
                    amountLabel = if (isExpense) "How much they owe" else "Amount"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { link?.let(onConfirm) }, enabled = link != null) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** 1500.0 → "1500", 12.5 → "12.5", for prefilling an amount field. */
internal fun formatPlain(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
