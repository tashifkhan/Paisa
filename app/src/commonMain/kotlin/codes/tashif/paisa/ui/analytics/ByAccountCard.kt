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
        val ranks = accountRanks(expenses, totalExpense)
        ChartCard(title = "By account", subtitle = "Which account paid") {
            RankList(ranks, currency, onClick = onOpen)
        }
}
