package sh.taf.billi.ui.analytics

import sh.taf.billi.platform.BilliCalendar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import sh.taf.billi.data.TransactionWithDetails
import sh.taf.billi.ui.components.charts.BarChart
import sh.taf.billi.ui.components.charts.ChartEntry
import sh.taf.billi.platform.parseDate

/** Spending totals for each weekday in the period. */
@Composable
internal fun DayOfWeekCard(
    period: Period,
    expenses: List<TransactionWithDetails>,
    money: (Double) -> String
) {
        val names = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val fullNames = listOf("Sundays", "Mondays", "Tuesdays", "Wednesdays", "Thursdays", "Fridays", "Saturdays")
        val byDay = DoubleArray(7)
        val cal = BilliCalendar.getInstance()
        expenses.forEach { tx ->
            parseDate(tx.transactionDate.take(10), "yyyy-MM-dd", machine = true)?.let {
                cal.timeInMillis = it
                byDay[cal.get(BilliCalendar.DAY_OF_WEEK) - 1] += tx.amount
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
