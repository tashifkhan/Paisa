package codes.tashif.paisa.ui.analytics

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import codes.tashif.paisa.data.TransactionWithDetails

/** Merchants ranked by spending. */
@Composable
internal fun TopPlacesCard(
    expenses: List<TransactionWithDetails>,
    totalExpense: Double,
    currency: String,
    onOpen: (AnalyticsGroup) -> Unit,
    onSeeAll: (String, List<RankItem>) -> Unit
) {
        val ranks = expenses.groupBy { merchantKey(it) }
            .map { (name, list) ->
                val sum = list.sumOf { it.amount }
                RankItem(
                    group = AnalyticsGroup(AnalyticsGroup.Kind.MERCHANT, name),
                    label = name,
                    amount = sum,
                    count = list.size,
                    share = (sum / totalExpense).toFloat(),
                    color = Color.Unspecified
                )
            }
            .sortedByDescending { it.amount }
        ChartCard(title = "Top places", subtitle = "Where the money went") {
            RankList(ranks.take(LEGEND_ROWS), currency, onClick = onOpen)
            if (ranks.size > LEGEND_ROWS) {
                SeeAllButton("See all ${ranks.size} places") { onSeeAll("Spending by place", ranks) }
            }
        }
}
