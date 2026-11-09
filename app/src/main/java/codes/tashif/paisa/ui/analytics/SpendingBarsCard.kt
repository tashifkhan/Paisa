package codes.tashif.paisa.ui.analytics

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.ui.components.charts.BarChart
import codes.tashif.paisa.ui.components.charts.ChartEntry
import java.util.Calendar

/** Spending per hour, day or month, depending on the period length. */
@Composable
internal fun SpendingBarsCard(
    period: Period,
    expenses: List<TransactionWithDetails>,
    money: (Double) -> String
) {
        val bars = period.buckets.map { bucket ->
            ChartEntry(
                label = bucket.label,
                value = expenses.filter { bucket.contains(it.transactionDate) }.sumOf { it.amount },
                color = Color.Unspecified
            )
        }
        val unitName = when (period.bucketField) {
            Calendar.HOUR_OF_DAY -> "hour"
            Calendar.DAY_OF_MONTH -> "day"
            Calendar.MONTH -> "month"
            else -> "year"
        }
        val monthly = unitName == "month"
        ChartCard(
            title = when (unitName) {
                "hour" -> "Hourly spending"
                "day" -> "Daily spending"
                "month" -> "Monthly spending"
                else -> "Yearly spending"
            },
            subtitle = "Tap a bar for the total"
        ) {
            BarChart(
                entries = bars,
                barColor = MaterialTheme.colorScheme.primary,
                highlightEvery = when {
                    bars.size <= 8 -> 1
                    bars.size <= 14 -> 2
                    monthly -> 3
                    else -> (bars.size / 6).coerceAtLeast(1)
                },
                formatValue = money,
                tooltipLabel = { entry -> period.buckets.getOrNull(bars.indexOfFirst { it === entry })?.tip ?: entry.label },
                supportingLabel = {
                    when {
                        it.value <= 0 -> "No spending"
                        else -> "Spent that $unitName"
                    }
                }
            )
        }
}
