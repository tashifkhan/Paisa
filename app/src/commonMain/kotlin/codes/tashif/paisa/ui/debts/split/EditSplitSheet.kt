package codes.tashif.paisa.ui.debts.split

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.data.repository.DebtShare
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.theme.spacing

/**
 * Splits an existing expense of [total]. [initialPlan] is the split it has now,
 * if any. [onSave] gets what each person owes, with positive amounts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSplitSheet(
    people: List<DebtPersonBalance>,
    total: Double,
    currency: String,
    initialPlan: SplitPlan,
    onDismiss: () -> Unit,
    onSave: (List<DebtShare>) -> Unit
) {
    var plan by remember { mutableStateOf(initialPlan) }
    val outcome = plan.compute(total)
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
            Text("Split with others", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "Total ${formatMoney(currency, total)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SplitEditor(
                people = people,
                plan = plan,
                onPlanChange = { plan = it },
                outcome = outcome,
                currency = currency
            )
            SplitPreview(outcome = outcome, currency = currency)
            SplitSaveButton(outcome = outcome, onClick = { onSave(outcome.owedToMe()) })
        }
    }
}

/** Save for a split sheet, with the reason underneath when it can't be saved yet. */
@Composable
internal fun SplitSaveButton(outcome: SplitOutcome, onClick: () -> Unit) {
    splitProblemText(outcome.problem)?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Button(
        onClick = onClick,
        enabled = outcome.isValid,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) { Text("Save", fontWeight = FontWeight.SemiBold) }
}
