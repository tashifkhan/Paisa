package codes.tashif.paisa.ui.debts.split

import codes.tashif.paisa.ui.components.PillTabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.spacing
import kotlin.math.abs

/**
 * Who's in a split and how it's divided. Takes the [plan] and reports every
 * change through [onPlanChange]. [outcome] is the plan worked out against the total.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplitEditor(
    people: List<DebtPersonBalance>,
    plan: SplitPlan,
    onPlanChange: (SplitPlan) -> Unit,
    outcome: SplitOutcome,
    currency: String,
    modifier: Modifier = Modifier
) {
    var newName by remember { mutableStateOf("") }
    val addNewName = {
        val name = newName.trim()
        if (name.isNotEmpty()) {
            val known = people.firstOrNull { it.person.name.equals(name, ignoreCase = true) }?.person
            val person = known?.let { SplitPerson(it.id, it.name) } ?: SplitPerson(null, name)
            if (!plan.has(person)) onPlanChange(plan.copy(people = plan.people + person))
            newName = ""
        }
    }
    // New names typed here show as chips next to the people already in Debts.
    val chips = people.map { SplitPerson(it.person.id, it.person.name) } +
        plan.people.filter { it.personId == null }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = plan.includeMe,
                onClick = { onPlanChange(plan.copy(includeMe = !plan.includeMe)) },
                label = { Text("Include me") },
                leadingIcon = if (plan.includeMe) {
                    { Icon(Icons.Rounded.Check, contentDescription = null) }
                } else {
                    null
                }
            )
            chips.forEach { person ->
                FilterChip(
                    selected = plan.has(person),
                    onClick = { onPlanChange(plan.toggle(person)) },
                    label = { Text(person.name) },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) }
                )
            }
        }
        OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("Add a name") },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { addNewName() }),
            trailingIcon = {
                IconButton(onClick = addNewName, enabled = newName.isNotBlank()) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        PillTabs(
            options = listOf("Equal" to SplitMode.EQUAL, "Custom amounts" to SplitMode.CUSTOM),
            selected = plan.mode,
            onSelect = { mode ->
                val next = when {
                    mode == plan.mode -> plan
                    mode == SplitMode.CUSTOM -> plan.toCustom(outcome)
                    else -> plan.copy(mode = mode)
                }
                onPlanChange(next)
            }
        )

        if (plan.mode == SplitMode.CUSTOM) {
            val rows = plan.people.map { it.key to it.name } +
                if (plan.includeMe) listOf(SplitPlan.ME_KEY to "Your share") else emptyList()
            rows.forEach { (key, label) ->
                OutlinedTextField(
                    value = plan.customText[key].orEmpty(),
                    onValueChange = { text ->
                        if (text.isEmpty() || text.toDoubleOrNull() != null) {
                            onPlanChange(plan.copy(customText = plan.customText + (key to text)))
                        }
                    },
                    label = { Text(label) },
                    prefix = { Text(currency) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (abs(outcome.left) >= 0.005) {
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
        }
    }
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
