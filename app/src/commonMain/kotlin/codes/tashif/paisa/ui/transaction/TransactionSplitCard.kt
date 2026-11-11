package codes.tashif.paisa.ui.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.DebtEntryWithPerson
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.data.repository.DebtShare
import codes.tashif.paisa.ui.components.SheetToggleCard
import codes.tashif.paisa.ui.debts.split.EditSplitSheet
import codes.tashif.paisa.ui.debts.split.SplitPerson
import codes.tashif.paisa.ui.debts.split.SplitPlan
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing
import kotlin.math.abs

/**
 * Who a transaction is split with, and the ways to change it. With no split,
 * an expense offers "Split with others" and income "Someone paid me back".
 */
@Composable
internal fun TransactionSplitSection(
    split: List<DebtEntryWithPerson>,
    people: List<DebtPersonBalance>,
    total: Double,
    isExpense: Boolean,
    currency: String,
    onOpenPerson: (Int) -> Unit,
    onSave: (List<DebtShare>) -> Unit,
    onRemove: () -> Unit
) {
    val haptics = rememberHaptics()
    var editing by remember { mutableStateOf(false) }

    if (split.isEmpty()) {
        SheetToggleCard(
            icon = Icons.Rounded.Handshake,
            title = personLinkTitle(isExpense),
            subtitle = personLinkSubtitle(isExpense),
            checked = false,
            onCheckedChange = { editing = true }
        )
    } else {
        SplitCard(
            split = split,
            currency = currency,
            onOpenPerson = onOpenPerson,
            onEdit = { editing = true },
            onRemove = {
                haptics.reject()
                onRemove()
            }
        )
    }

    if (!editing) return
    val save = { shares: List<DebtShare> ->
        haptics.confirm()
        onSave(shares)
        editing = false
    }
    if (isExpense) {
        EditSplitSheet(
            people = people,
            total = total,
            currency = currency,
            initialPlan = remember(split) {
                SplitPlan.fromShares(total, split.map { SplitPerson(it.entry.personId, it.personName) to abs(it.entry.amount) })
            },
            onDismiss = { editing = false },
            onSave = save
        )
    } else {
        val first = split.firstOrNull()
        PersonLinkDialog(
            people = people,
            transactionAmount = total,
            isExpense = false,
            onDismiss = { editing = false },
            onConfirm = { link -> save(listOf(DebtShare(link.personId, link.newName, link.amount))) },
            initialPersonId = first?.entry?.personId,
            initialAmount = first?.entry?.amount?.let(::abs)
        )
    }
}

@Composable
private fun SplitCard(
    split: List<DebtEntryWithPerson>,
    currency: String,
    onOpenPerson: (Int) -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(vertical = MaterialTheme.spacing.small)) {
            Text(
                text = "Split",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium)
            )
            split.forEach { row ->
                val amount = row.entry.amount
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPerson(row.entry.personId) }
                        .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.smaller),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Handshake, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(MaterialTheme.spacing.small))
                    Text(
                        text = if (amount >= 0) "${row.personName} owes" else "${row.personName} paid back",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatMoney(currency, abs(amount)),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Open ${row.personName}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                TextButton(onClick = onEdit) { Text("Edit split") }
                TextButton(onClick = onRemove) {
                    Text("Remove split", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
