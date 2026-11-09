package codes.tashif.paisa.ui.debts

import codes.tashif.paisa.ui.components.AdaptiveSheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.DebtEntry
import codes.tashif.paisa.ui.debts.split.plain
import codes.tashif.paisa.ui.theme.spacing
import kotlin.math.abs

/** Edits an entry added by hand: amount, which way it goes, note and date. Delete lives here too. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditDebtEntrySheet(
    entry: DebtEntry,
    onDismiss: () -> Unit,
    onSave: (amount: Double, note: String, entryDate: String) -> Unit,
    onDelete: () -> Unit
) {
    var amountText by remember { mutableStateOf(plain(abs(entry.amount))) }
    var theyOwe by remember { mutableStateOf(entry.amount >= 0) }
    var note by remember { mutableStateOf(entry.note) }
    var date by remember { mutableStateOf(entry.entryDate) }
    val amount = amountText.toDoubleOrNull()?.takeIf { it > 0 }

    AdaptiveSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text("Edit entry", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("They owe me" to true, "I owe them" to false).forEachIndexed { i, (label, value) ->
                    SegmentedButton(
                        selected = theyOwe == value,
                        onClick = { theyOwe = value },
                        shape = SegmentedButtonDefaults.itemShape(i, 2)
                    ) { Text(label) }
                }
            }
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
            DebtDateButton(iso = date, onChange = { date = it })
            Button(
                onClick = { amount?.let { onSave(if (theyOwe) it else -it, note, date) } },
                enabled = amount != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) { Text("Save", fontWeight = FontWeight.SemiBold) }
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text("Delete entry", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
