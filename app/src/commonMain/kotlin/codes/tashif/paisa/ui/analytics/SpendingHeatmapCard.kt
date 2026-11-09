package codes.tashif.paisa.ui.analytics

import codes.tashif.paisa.platform.PaisaCalendar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.ui.components.charts.CalendarHeatmap
import codes.tashif.paisa.ui.format.dayLabel

/** PaisaCalendar of the month with each day shaded by spending. */
@Composable
internal fun SpendingHeatmapCard(
    period: Period,
    expenses: List<TransactionWithDetails>,
    money: (Double) -> String
) {
    ChartCard(
        title = "Spending heatmap",
        subtitle = "Tap a day for details",
        info = "Spending heatmap" to "Each square is a day of the month. The darker it is, " +
            "the more you spent that day. Empty squares had no spending."
    ) {
        val firstDayOffset = (period.cal.clone()).apply {
            set(PaisaCalendar.DAY_OF_MONTH, 1)
        }.get(PaisaCalendar.DAY_OF_WEEK) - PaisaCalendar.SUNDAY
        CalendarHeatmap(
            values = period.buckets.map { day ->
                expenses.filter { day.contains(it.transactionDate) }.sumOf { it.amount }
            },
            firstDayOffset = firstDayOffset,
            color = MaterialTheme.colorScheme.primary,
            formatValue = money,
            dayLabel = { day -> "$day ${period.short}" }
        )
    }
}
