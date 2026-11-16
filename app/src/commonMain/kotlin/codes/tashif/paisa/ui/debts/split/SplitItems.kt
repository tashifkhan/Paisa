package codes.tashif.paisa.ui.debts.split

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import codes.tashif.paisa.ui.components.SheetFieldCard
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

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
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        if (scanning) {
            SheetFieldCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(MaterialTheme.spacing.small))
                    Text("Reading the bill", style = MaterialTheme.typography.titleSmall)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                BillSourceCard(
                    icon = Icons.Rounded.PhotoCamera,
                    title = "Take photo",
                    subtitle = "Open the camera",
                    onClick = onTakePhoto,
                    modifier = Modifier.weight(1f)
                )
                BillSourceCard(
                    icon = Icons.Rounded.PhotoLibrary,
                    title = "Choose photo",
                    subtitle = "From your gallery",
                    onClick = onChoosePhoto,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!scanMessage.isNullOrBlank()) {
            Text(
                text = scanMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (plan.items.isEmpty()) {
            Text(
                text = "Each line goes to whoever had it. A line can be shared. Tax and tip follow those shares.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        plan.items.forEachIndexed { index, item ->
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
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
                        shape = MaterialTheme.shapes.extraLarge,
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
                        shape = MaterialTheme.shapes.extraLarge,
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
                            FilterChip(
                                selected = key in item.who,
                                onClick = {
                                    haptics.tick()
                                    val next = if (key in item.who) item.who - key else item.who + key
                                    onPlanChange(plan.replaceItem(index, item.copy(who = next)))
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        TextButton(
            onClick = {
                haptics.tick()
                onPlanChange(plan.copy(items = plan.items + BillItem()))
            },
            modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(MaterialTheme.spacing.extraSmall))
            Text("Add an item")
        }

        val total = outcome.total()
        if (plan.items.any { it.amount > 0.0 } && total != null) {
            val tax = toPaise(total - plan.itemsTotal)
            val caption = when {
                tax > 0.004 -> "${formatMoney(currency, tax)} in tax and charges, split by what each person had"
                tax < -0.004 -> "${formatMoney(currency, -tax)} off, taken off in the same shares"
                else -> "Items add up to the total"
            }
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (outcome.left >= 0.005) {
                Text(
                    text = "${formatMoney(currency, outcome.left)} still on nobody",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Half of a row, built like the date and account cards: a tinted circle, a
 * title, and a short line under it.
 */
@Composable
private fun BillSourceCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SheetFieldCard(onClick = onClick, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun SplitPlan.replaceItem(index: Int, item: BillItem): SplitPlan =
    copy(items = items.mapIndexed { i, old -> if (i == index) item else old })
