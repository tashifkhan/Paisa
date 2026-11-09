package codes.tashif.paisa.ui.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.ui.theme.spacing

/** Average a day, count, biggest expense and the change against the period before. */
@Composable
internal fun AnalyticsQuickStats(
    period: Period,
    periodTx: List<TransactionWithDetails>,
    expenses: List<TransactionWithDetails>,
    totalExpense: Double,
    prevExpense: Double,
    money: (Double) -> String,
    onOpenTransaction: (Int) -> Unit
) {
    val biggest = expenses.maxByOrNull { it.amount }
    QuickStats(
        stats = listOf(
            "Average a day" to money(totalExpense / period.daysElapsed.coerceAtLeast(1)),
            "Transactions" to "${periodTx.size}",
            "Biggest expense" to (biggest?.let { "${money(it.amount)} · ${merchantKey(it)}" } ?: "None"),
            "Vs ${period.prevLabel}" to when {
                prevExpense <= 0.0 -> "No data"
                totalExpense <= prevExpense -> "${money(prevExpense - totalExpense)} less spent"
                else -> "${money(totalExpense - prevExpense)} more spent"
            }
        ),
        onBiggest = biggest?.let { { onOpenTransaction(it.id) } }
    )
}

@Composable
private fun QuickStats(stats: List<Pair<String, String>>, onBiggest: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            stats.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    row.forEach { (label, value) ->
                        val click = if (label == "Biggest expense") onBiggest else null
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .then(if (click != null) Modifier.clickable(onClick = click) else Modifier)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
