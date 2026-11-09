package codes.tashif.paisa.ui.analytics

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.ui.components.charts.BarChart
import codes.tashif.paisa.ui.components.charts.ChartEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Spending totals for each weekday in the period. */
@Composable
internal fun DayOfWeekCard(
    period: Period,
    expenses: List<TransactionWithDetails>,
    money: (Double) -> String
) {
        val names = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val fullNames = listOf("Sundays", "Mondays", "Tuesdays", "Wednesdays", "Thursdays", "Fridays", "Saturdays")
        val parser = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
        val byDay = DoubleArray(7)
        val cal = Calendar.getInstance()
        expenses.forEach { tx ->
            runCatching { parser.parse(tx.transactionDate.take(10)) }.getOrNull()?.let {
                cal.time = it
                byDay[cal.get(Calendar.DAY_OF_WEEK) - 1] += tx.amount
            }
        }
        val top = byDay.indices.maxByOrNull { byDay[it] } ?: 0
        ChartCard(
            title = "By day of the week",
            subtitle = "You spend the most on ${fullNames[top]}"
        ) {
            BarChart(
                entries = names.mapIndexed { i, n -> ChartEntry(n, byDay[i], Color.Unspecified) },
                barColor = MaterialTheme.colorScheme.tertiary,
                highlightEvery = 1,
                formatValue = money,
                tooltipLabel = { "${fullNames[names.indexOf(it.label)]} in ${period.short}" },
                supportingLabel = { if (it.value > 0) "Total spent" else "No spending" }
            )
        }
}
