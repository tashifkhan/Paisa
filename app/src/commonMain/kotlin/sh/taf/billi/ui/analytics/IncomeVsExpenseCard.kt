package sh.taf.billi.ui.analytics

import sh.taf.billi.platform.BilliCalendar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import sh.taf.billi.data.TransactionWithDetails
import sh.taf.billi.ui.components.charts.GroupedBarChart
import sh.taf.billi.ui.theme.ExpenseRed
import sh.taf.billi.ui.theme.IncomeGreen
import sh.taf.billi.ui.theme.spacing

/** Income and expense per month: each month of the year, or the last 6 months. */
@Composable
internal fun IncomeVsExpenseCard(
    periodKind: AnalyticsPeriod,
    period: Period,
    transactions: List<TransactionWithDetails>,
    money: (Double) -> String
) {
    val yearly = periodKind == AnalyticsPeriod.YEAR
    val months = if (yearly) {
        (0 until 12).map { m ->
            (period.cal.clone()).apply { set(BilliCalendar.DAY_OF_MONTH, 1); set(BilliCalendar.MONTH, m) }
        }
    } else {
        (5 downTo 0).map { back -> (period.lastMonth.clone()).apply { add(BilliCalendar.MONTH, -back) } }
    }
    val trend = months.map { cal ->
        val prefix = Period.fmt("yyyy-MM", cal)
        val txs = transactions.filter { it.transactionDate.startsWith(prefix) }
        Triple(
            Period.fmt("MMM", cal),
            txs.filter { it.type == "income" }.sumOf { it.amount },
            txs.filter { it.type == "expense" }.sumOf { it.amount }
        )
    }
    ChartCard(
        title = "Income vs expense",
        info = "Income vs expense" to "Each pair of bars is one month: money in on the left, " +
            "money out on the right. Tap a pair to see both totals.",
        subtitle = if (yearly) "Each month of ${period.short}" else "Last 6 months · tap a group"
    ) {
        GroupedBarChart(
            labels = trend.map { it.first },
            seriesA = trend.map { it.second },
            seriesB = trend.map { it.third },
            colorA = IncomeGreen,
            colorB = ExpenseRed,
            nameA = "Income",
            nameB = "Expense",
            formatValue = money,
            note = { a, b -> "Net " + (if (a >= b) "+" else "−") + money(kotlin.math.abs(a - b)) }
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
            LegendDot(color = IncomeGreen, label = "Income")
            LegendDot(color = ExpenseRed, label = "Expense")
        }
    }
}
