package codes.tashif.paisa.ui.debts.split

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.data.repository.DebtShare
import codes.tashif.paisa.ui.debts.DebtDateButton
import codes.tashif.paisa.ui.debts.nowEntryDate
import codes.tashif.paisa.ui.theme.spacing

/**
 * Splits a bill that has no transaction in Paisa, such as a dinner someone
 * else paid for. [onSave] gets signed shares (> 0 they owe you), the note and the date.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SplitBillSheet(
    people: List<DebtPersonBalance>,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (shares: List<DebtShare>, note: String, entryDate: String) -> Unit
) {
    var totalText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(nowEntryDate()) }
    var plan by remember { mutableStateOf(SplitPlan()) }
    var payerKey by remember { mutableStateOf<String?>(null) } // null: I paid
    // A payer who was taken out of the split can't still be the payer.
    val payer = plan.people.firstOrNull { it.key == payerKey }
    val outcome = plan.computeBill(totalText.toDoubleOrNull(), payer)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text("Split a bill", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "For a bill that isn't a transaction here. Your accounts stay as they are.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = totalText,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) totalText = it },
                label = { Text("Total") },
                prefix = { Text(currency) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("What for") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            )
            DebtDateButton(iso = date, onChange = { date = it })

            SplitEditor(
                people = people,
                plan = plan,
                onPlanChange = { plan = it },
                outcome = outcome,
                currency = currency
            )

            Text("Who paid", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
                FilterChip(selected = payer == null, onClick = { payerKey = null }, label = { Text("Me") })
                plan.people.forEach { person ->
                    FilterChip(
                        selected = payer?.key == person.key,
                        onClick = { payerKey = person.key },
                        label = { Text(person.name) }
                    )
                }
            }
            if (payer != null) {
                Text(
                    text = "Only your share is recorded. The others owe ${payer.name}, not you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SplitPreview(outcome = outcome, currency = currency, payer = payer)
            SplitSaveButton(
                outcome = outcome,
                onClick = { onSave(outcome.billEntries(payer), note, date) }
            )
        }
    }
}
