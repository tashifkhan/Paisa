package codes.tashif.paisa.ui.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/** One read-only line in the details card. */
internal data class DetailRowSpec(val icon: ImageVector, val label: String, val value: String)

/** Detail rows for [tx]: only real data, and SMS-only fields never for manual entries. */
internal fun transactionDetailRows(
    tx: TransactionWithDetails,
    displayDate: String,
    sourceLabel: String,
    currency: String
): List<DetailRowSpec> {
    val isManual = tx.source == "manual"
    return buildList {
        add(DetailRowSpec(Icons.Rounded.CalendarMonth, "Date & Time", displayDate))
        add(DetailRowSpec(Icons.Rounded.Info, "Source", sourceLabel))
        add(DetailRowSpec(Icons.Rounded.Category, "Category", tx.categoryName))
        if (!isManual) {
            tx.bankName?.takeIf { it.isNotBlank() }?.let {
                add(DetailRowSpec(Icons.Rounded.AccountBalance, "Bank", it))
            }
        }
        val accountLabel = listOfNotNull(
            tx.accountName.takeIf { it.isNotBlank() },
            tx.accountNumber
                ?.takeIf { !isManual && it.isNotBlank() }
                ?.let { "••$it" }
        ).joinToString(" ")
        if (accountLabel.isNotBlank()) {
            add(DetailRowSpec(Icons.Rounded.AccountBalanceWallet, "Account", accountLabel))
        }
        if (!isManual) {
            tx.balanceAfter?.let {
                add(DetailRowSpec(Icons.Rounded.Savings, "Balance after", formatMoney(currency, it)))
            }
            tx.reference?.takeIf { it.isNotBlank() }?.let {
                add(DetailRowSpec(Icons.Rounded.Tag, "Reference", it))
            }
        }
    }
}

/** Read-only snapshot of the transaction: date, source, category, bank and so on. */
@Composable
internal fun TransactionDetailsCard(rows: List<DetailRowSpec>) {
Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
) {
    Column(modifier = Modifier.padding(vertical = MaterialTheme.spacing.extraSmall)) {
        rows.forEachIndexed { index, row ->
            DetailRow(
                icon = row.icon,
                label = row.label,
                value = row.value,
                showDivider = index < rows.lastIndex
            )
        }
    }
}
}

/** The original SMS, collapsed until tapped. */
@Composable
internal fun SmsMessageCard(
    body: String,
    sender: String?,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val haptics = rememberHaptics()
Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.toggle(on = !expanded)
                    onToggle()
            }
            .padding(MaterialTheme.spacing.medium)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Rounded.Message,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(MaterialTheme.spacing.small))
            Text(
                text = if (expanded) "Hide SMS" else "Show SMS",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (expanded) {
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            sender?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))
                Text(
                    text = "From $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(MaterialTheme.spacing.small))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}
