package codes.tashif.paisa.ui.debts

import codes.tashif.paisa.platform.parseDate
import codes.tashif.paisa.platform.formatDate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.DebtEntry
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.components.groupShape
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen
import codes.tashif.paisa.ui.theme.spacing
import kotlin.math.abs


/**
 * One line of a person's history. [onClick] opens the transaction for a
 * linked entry, or the edit sheet for one added by hand.
 */
@Composable
internal fun DebtEntryRow(
    entry: DebtEntry,
    currency: String,
    index: Int,
    count: Int,
    onClick: () -> Unit
) {
    val up = entry.amount >= 0
    val linked = entry.transactionSyncId != null
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = groupShape(groupPositionOf(index, count)),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.small,
                top = MaterialTheme.spacing.small,
                bottom = MaterialTheme.spacing.small
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (up) Icons.AutoMirrored.Rounded.CallMade else Icons.AutoMirrored.Rounded.CallReceived,
                contentDescription = if (up) "They owe more" else "They owe less",
                tint = if (up) IncomeGreen else ExpenseRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.note.ifBlank { if (up) "Lent" else "Paid back" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayDate(entry.entryDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (linked) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = MaterialTheme.spacing.small)
                                .size(14.dp)
                        )
                        Text(
                            text = "From transaction",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = (if (up) "+" else "−") + formatMoney(currency, abs(entry.amount)),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (up) IncomeGreen else ExpenseRed
            )
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = if (linked) "Open transaction" else "Edit entry",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

internal fun displayDate(iso: String): String =
    parseDate(iso, "yyyy-MM-dd'T'HH:mm:ss", machine = true)?.let { formatDate(it, "d MMM yyyy") }
        ?: iso.take(10)
