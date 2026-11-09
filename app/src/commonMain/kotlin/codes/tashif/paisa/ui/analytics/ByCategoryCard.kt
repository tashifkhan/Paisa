package codes.tashif.paisa.ui.analytics

import codes.tashif.paisa.ui.components.PillTabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.ui.components.charts.ChartEntry
import codes.tashif.paisa.ui.components.charts.DonutChart
import codes.tashif.paisa.ui.theme.spacing

/** Donut and ranked list of categories, for spending or income. */
@Composable
internal fun ByCategoryCard(
    breakdownIncome: Boolean,
    onBreakdownChange: (Boolean) -> Unit,
    expenses: List<TransactionWithDetails>,
    incomes: List<TransactionWithDetails>,
    totalExpense: Double,
    totalIncome: Double,
    period: Period,
    currency: String,
    money: (Double) -> String,
    onOpen: (AnalyticsGroup) -> Unit,
    onSeeAll: (String, List<RankItem>) -> Unit
) {
    val ranks = categoryRanks(if (breakdownIncome) incomes else expenses)
    val total = if (breakdownIncome) totalIncome else totalExpense
    ChartCard(title = "By category", subtitle = "Tap a category to see its transactions") {
        PillTabs(
            options = listOf("Spending" to false, "Income" to true),
            selected = breakdownIncome,
            onSelect = onBreakdownChange
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        if (ranks.isEmpty()) {
            Text(
                text = if (breakdownIncome) "No income in ${period.label}" else "No spending in ${period.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.spacing.large),
                textAlign = TextAlign.Center
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DonutChart(
                    entries = ranks.map { ChartEntry(it.label, it.amount, it.color) },
                    centerTitle = money(total),
                    centerSubtitle = if (breakdownIncome) "total received" else "total spent",
                    formatValue = money,
                    shareLabel = if (breakdownIncome) "of income" else "of spending"
                )
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                RankList(ranks.take(LEGEND_ROWS), currency, onClick = onOpen)
                if (ranks.size > LEGEND_ROWS) {
                    SeeAllButton("See all ${ranks.size} categories") {
                        onSeeAll(if (breakdownIncome) "Income by category" else "Spending by category", ranks)
                    }
                }
            }
        }
    }
}
