package codes.tashif.paisa.ui.debts.split

import codes.tashif.paisa.ui.components.PillTabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import codes.tashif.paisa.ai.BillScanService
import codes.tashif.paisa.core.AppLockGuard
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.statement.rememberPhotoPicker
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.spacing
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Who's in a split and how it's divided. Takes the [plan] and reports every
 * change through [onPlanChange]. [outcome] is the plan worked out against the total.
 * [onScannedTotal] gets the total printed on a bill, when a scan finds one.
 */
@Composable
fun SplitEditor(
    people: List<DebtPersonBalance>,
    plan: SplitPlan,
    onPlanChange: (SplitPlan) -> Unit,
    outcome: SplitOutcome,
    currency: String,
    modifier: Modifier = Modifier,
    onScannedTotal: (Double) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val scanner = koinInject<BillScanService>()
    val snackbar = koinInject<SnackbarManager>()
    val lock = koinInject<AppLockGuard>()
    val latestPlan by rememberUpdatedState(plan)
    val latestOnPlan by rememberUpdatedState(onPlanChange)
    val latestOnTotal by rememberUpdatedState(onScannedTotal)
    var scanning by remember { mutableStateOf(false) }
    val openPhotos = rememberPhotoPicker { files ->
        val file = files.firstOrNull() ?: return@rememberPhotoPicker
        scope.launch {
            scanning = true
            try {
                val bill = scanner.scan(file)
                val items = bill.items.map { BillItem(it.name, plain(it.amount)) }
                latestOnPlan(latestPlan.copy(mode = SplitMode.ITEMS, items = items))
                bill.total?.let(latestOnTotal)
                snackbar.show(if (items.size == 1) "Read 1 item" else "Read ${items.size} items")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbar.show(e.message?.takeIf { it.isNotBlank() } ?: "Couldn't read that bill")
            } finally {
                scanning = false
            }
        }
    }
    fun scan() {
        if (scanning) return
        if (!scanner.canRun()) {
            snackbar.show("Add an AI provider in Settings before scanning a bill")
            return
        }
        lock.onExternalActivityLaunched()
        openPhotos()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
    ) {
        SplitPeoplePicker(
            people = people,
            plan = plan,
            onPlanChange = onPlanChange,
            currency = currency
        )

        PillTabs(
            options = listOf(
                "Equal" to SplitMode.EQUAL,
                "Amount" to SplitMode.CUSTOM,
                "%" to SplitMode.PERCENT,
                "Items" to SplitMode.ITEMS
            ),
            selected = plan.mode,
            onSelect = { mode ->
                if (mode != plan.mode) {
                    val next = when (mode) {
                        SplitMode.CUSTOM -> plan.toCustom(outcome)
                        SplitMode.PERCENT -> plan.toPercent(outcome, outcome.total())
                        else -> plan.copy(mode = mode)
                    }
                    onPlanChange(next)
                }
            }
        )

        val rows = plan.people.map { it.key to it.name } +
            if (plan.includeMe) listOf(SplitPlan.ME_KEY to "Your share") else emptyList()
        when (plan.mode) {
            SplitMode.EQUAL -> Unit
            SplitMode.CUSTOM -> {
                ShareFields(
                    rows = rows,
                    prefix = currency,
                    valueOf = { plan.customText[it].orEmpty() }
                ) { key, text ->
                    onPlanChange(plan.copy(customText = plan.customText + (key to text)))
                }
                ShareRemainder(outcome, currency)
            }
            SplitMode.PERCENT -> {
                ShareFields(
                    rows = rows,
                    suffix = "%",
                    valueOf = { plan.percentText[it].orEmpty() }
                ) { key, text ->
                    onPlanChange(plan.copy(percentText = plan.percentText + (key to text)))
                }
                Text(
                    text = "${percentText(plan.percentTotal)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (plan.percentTotal > 100.005) {
                        ExpenseRed
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                ShareRemainder(outcome, currency)
            }
            SplitMode.ITEMS -> SplitItemsEditor(
                plan = plan,
                outcome = outcome,
                currency = currency,
                scanning = scanning,
                onPlanChange = onPlanChange,
                onScan = ::scan
            )
        }
    }
}

@Composable
private fun ShareFields(
    rows: List<Pair<String, String>>,
    prefix: String? = null,
    suffix: String? = null,
    valueOf: (String) -> String,
    onValue: (String, String) -> Unit
) {
    rows.forEach { (key, label) ->
        OutlinedTextField(
            value = valueOf(key),
            onValueChange = { text ->
                if (text.isEmpty() || text.toDoubleOrNull() != null) onValue(key, text)
            },
            label = { Text(label) },
            prefix = prefix?.let { { Text(it) } },
            suffix = suffix?.let { { Text(it) } },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ShareRemainder(outcome: SplitOutcome, currency: String) {
    if (abs(outcome.left) < 0.005) return
    Text(
        text = if (outcome.left > 0) {
            "${formatMoney(currency, outcome.left)} left"
        } else {
            "${formatMoney(currency, -outcome.left)} over the total"
        },
        style = MaterialTheme.typography.bodyMedium,
        color = if (outcome.left > 0) MaterialTheme.colorScheme.onSurfaceVariant else ExpenseRed
    )
}

/**
 * "Radhika owes ₹600" lines and "Your share ₹600". When [payer] paid, only
 * what you owe them is shown, since that's all that gets recorded.
 */
@Composable
fun SplitPreview(
    outcome: SplitOutcome,
    currency: String,
    modifier: Modifier = Modifier,
    payer: SplitPerson? = null
) {
    val lines = buildList {
        if (payer == null) {
            outcome.shares.filter { it.amount > 0.0 }.forEach {
                add("${it.person.name} owes" to it.amount)
            }
        } else if (outcome.myShare > 0.0) {
            add("You owe ${payer.name}" to outcome.myShare)
        }
        if (outcome.myShare > 0.0) add("Your share" to outcome.myShare)
    }
    if (lines.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        lines.forEach { (label, amount) ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    formatMoney(currency, amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Short reason the split can't be saved yet, or null. */
fun splitProblemText(problem: SplitProblem?): String? = when (problem) {
    null -> null
    SplitProblem.NO_TOTAL -> "Enter the total first"
    SplitProblem.NO_PEOPLE -> "Pick who you're splitting with"
    SplitProblem.NOTHING_ASSIGNED -> "Nothing is assigned yet"
    SplitProblem.OVER_TOTAL -> "The shares are more than the total"
    SplitProblem.NEEDS_ME -> "Turn on Include me when someone else paid"
}
