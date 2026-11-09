package codes.tashif.paisa.ui.analytics

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import codes.tashif.paisa.data.TransactionWithDetails

/** Accounts ranked by how much they paid. */
@Composable
internal fun ByAccountCard(
    expenses: List<TransactionWithDetails>,
    totalExpense: Double,
    currency: String,
    onOpen: (AnalyticsGroup) -> Unit
) {
        val ranks = expenses.groupBy { it.accountId }
            .map { (id, list) ->
                val sum = list.sumOf { it.amount }
                RankItem(
                    group = AnalyticsGroup(AnalyticsGroup.Kind.ACCOUNT, id.toString()),
                    label = list.first().accountName,
                    amount = sum,
                    count = list.size,
                    share = (sum / totalExpense).toFloat(),
                    color = Color.Unspecified
                )
            }
            .sortedByDescending { it.amount }
        ChartCard(title = "By account", subtitle = "Which account paid") {
            RankList(ranks, currency, onClick = onOpen)
        }
}
