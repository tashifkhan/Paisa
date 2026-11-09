package codes.tashif.paisa.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.ui.components.charts.GroupedBarChart
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen
import codes.tashif.paisa.ui.theme.spacing
import java.util.Calendar

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
            (period.cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.MONTH, m) }
        }
    } else {
        (5 downTo 0).map { back -> (period.lastMonth.clone() as Calendar).apply { add(Calendar.MONTH, -back) } }
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
            formatValue = money
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
            LegendDot(color = IncomeGreen, label = "Income")
            LegendDot(color = ExpenseRed, label = "Expense")
        }
    }
}
