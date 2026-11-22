package sh.taf.billi.ui.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sh.taf.billi.data.TransactionWithDetails

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
            Stat("Average a day", money(totalExpense / period.daysElapsed.coerceAtLeast(1))),
            Stat("Transactions", "${periodTx.size}"),
            Stat("Biggest expense",
                biggest?.let { money(it.amount) } ?: "None",
                biggest?.let { merchantKey(it) }
            ),
            Stat("Vs ${period.prevLabel}",
                when {
                    prevExpense <= 0.0 -> "No data"
                    totalExpense <= prevExpense -> money(prevExpense - totalExpense)
                    else -> money(totalExpense - prevExpense)
                },
                when {
                    prevExpense <= 0.0 -> null
                    totalExpense <= prevExpense -> "less spent"
                    else -> "more spent"
                }
            )
        ),
        onBiggest = biggest?.let { { onOpenTransaction(it.id) } }
    )
}

private class Stat(val label: String, val value: String, val detail: String? = null)

/**
 * Four stats as one Expressive grouped grid: separate tiles with small
 * inner corners and large outer ones.
 */
@Composable
private fun QuickStats(stats: List<Stat>, onBiggest: (() -> Unit)?) {
    val big = 24.dp
    val small = 6.dp
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        stats.chunked(2).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                row.forEachIndexed { col, stat ->
                    val top = rowIndex == 0
                    val left = col == 0
                    val shape = RoundedCornerShape(
                        topStart = if (top && left) big else small,
                        topEnd = if (top && !left) big else small,
                        bottomStart = if (!top && left) big else small,
                        bottomEnd = if (!top && !left) big else small
                    )
                    val click = if (stat.label == "Biggest expense") onBiggest else null
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .then(if (click != null) Modifier.clickable(onClick = click) else Modifier)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stat.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stat.value,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        stat.detail?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
