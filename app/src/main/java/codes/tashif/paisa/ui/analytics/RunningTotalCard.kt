package codes.tashif.paisa.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.ui.components.charts.LineChart
import codes.tashif.paisa.ui.theme.spacing

/** Cumulative spending through the period, against the period before. */
@Composable
internal fun RunningTotalCard(
    periodKind: AnalyticsPeriod,
    period: Period,
    expenses: List<TransactionWithDetails>,
    prevExpenses: List<TransactionWithDetails>,
    money: (Double) -> String
) {
        val cumulative = { txs: List<TransactionWithDetails>, buckets: List<Bucket>, upTo: Int ->
            var running = 0.0
            buckets.mapIndexed { i, bucket ->
                running += txs.filter { bucket.contains(it.transactionDate) }.sumOf { it.amount }
                if (i < upTo) running else null
            }
        }
        val size = maxOf(period.buckets.size, period.prevBuckets.size)
        // Only what has already happened counts toward "so far".
        val now = cumulative(expenses.filter { period.hasHappened(it.transactionDate) }, period.buckets, period.bucketsElapsed)
        val before = cumulative(prevExpenses, period.prevBuckets, period.prevBuckets.size)
        val labels = List(size) { i -> period.buckets.getOrNull(i)?.label ?: "" }
        ChartCard(
            title = "Running total",
            subtitle = if (periodKind == AnalyticsPeriod.CUSTOM) {
                "Spending so far, against the same length of time just before"
            } else {
                "Spending so far, ${period.short} against ${period.prevLabel}"
            }
        ) {
            LineChart(
                labels = labels,
                seriesA = now + List(size - now.size) { null },
                seriesB = before + List(size - before.size) { null },
                colorA = MaterialTheme.colorScheme.primary,
                colorB = MaterialTheme.colorScheme.outline,
                nameA = period.short,
                nameB = period.prevLabel.replaceFirstChar { it.uppercase() },
                formatValue = money,
                tooltipLabel = { i -> period.buckets.getOrNull(i)?.tip ?: "" }
            )
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
                LegendDot(MaterialTheme.colorScheme.primary, period.short)
                LegendDot(MaterialTheme.colorScheme.outline, period.prevLabel.replaceFirstChar { it.uppercase() })
            }
        }
}
