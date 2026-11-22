package sh.taf.billi.ui.debts.split

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import sh.taf.billi.ui.format.formatMoney
import sh.taf.billi.ui.ai.LocalAiEnabled
import sh.taf.billi.ui.haptics.rememberHaptics
import sh.taf.billi.ui.theme.spacing

private val AMOUNT = Regex("""\d*\.?\d{0,2}""")

/**
 * One row per line of a bill, plus who had it. Several people on a line share
 * it. Tax, tip and discounts follow those shares.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun SplitItemsEditor(
    plan: SplitPlan,
    outcome: SplitOutcome,
    currency: String,
    scanning: Boolean,
    scanMessage: String? = null,
    onPlanChange: (SplitPlan) -> Unit,
    onTakePhoto: () -> Unit,
    onChoosePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    val who = plan.people.map { it.key to it.name } +
        if (plan.includeMe) listOf(SplitPlan.ME_KEY to "You") else emptyList()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
    ) {
        if (scanning) {
            Text(
                "Reading the bill",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else if (LocalAiEnabled.current) {
            // A model reads the bill, so the buttons go when AI is off.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                FilledTonalButton(onClick = onTakePhoto, modifier = Modifier.weight(1f)) {
                    ButtonIcon(Icons.Rounded.PhotoCamera)
                    Text("Scan a bill", maxLines = 1)
                }
                OutlinedButton(onClick = onChoosePhoto, modifier = Modifier.weight(1f)) {
                    ButtonIcon(Icons.Rounded.PhotoLibrary)
                    Text("Choose photo", maxLines = 1)
                }
            }
        }

        if (!scanMessage.isNullOrBlank()) SplitCaption(scanMessage, isError = true)

        if (plan.items.isEmpty()) {
            Text(
                text = "Each line goes to whoever had it. A line can be shared. Tax and tip follow those shares.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        plan.items.forEachIndexed { index, item ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = MaterialTheme.spacing.small,
                        top = MaterialTheme.spacing.smaller,
                        bottom = MaterialTheme.spacing.smaller
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { name ->
                                onPlanChange(plan.replaceItem(index, item.copy(name = name.take(60))))
                            },
                            label = { Text("Item") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.weight(1.4f)
                        )
                        OutlinedTextField(
                            value = item.amountText,
                            onValueChange = { text ->
                                if (text.matches(AMOUNT)) {
                                    onPlanChange(plan.replaceItem(index, item.copy(amountText = text)))
                                }
                            },
                            label = { Text("Amount") },
                            prefix = { Text(currency) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            haptics.tick()
                            onPlanChange(plan.copy(items = plan.items.filterIndexed { i, _ -> i != index }))
                        }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Remove item")
                        }
                    }
                    if (who.isEmpty()) {
                        Text(
                            text = "Add someone above to assign this",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
                            who.forEach { (key, label) ->
                                val selected = key in item.who
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        haptics.tick()
                                        val next = if (selected) item.who - key else item.who + key
                                        onPlanChange(plan.replaceItem(index, item.copy(who = next)))
                                    },
                                    label = { Text(label) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        TextButton(
            onClick = {
                haptics.tick()
                onPlanChange(plan.copy(items = plan.items + BillItem()))
            }
        ) {
            ButtonIcon(Icons.Rounded.Add)
            Text("Add an item")
        }

        val total = outcome.total()
        if (plan.items.any { it.amount > 0.0 } && total != null) {
            val tax = toPaise(total - plan.itemsTotal)
            SplitCaption(
                when {
                    tax > 0.004 -> "${formatMoney(currency, tax)} in tax and charges, split by what each person had"
                    tax < -0.004 -> "${formatMoney(currency, -tax)} off, taken off in the same shares"
                    else -> "Items add up to the total"
                }
            )
            if (outcome.left >= 0.005) {
                SplitCaption("${formatMoney(currency, outcome.left)} still on nobody")
            }
        }
    }
}

@Composable
private fun ButtonIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
}

private fun SplitPlan.replaceItem(index: Int, item: BillItem): SplitPlan =
    copy(items = items.mapIndexed { i, old -> if (i == index) item else old })
