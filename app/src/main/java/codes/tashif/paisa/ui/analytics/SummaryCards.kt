package codes.tashif.paisa.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen
import codes.tashif.paisa.ui.theme.spacing
import kotlin.math.roundToInt

/** Income, expense and saved for the period, with the change against the one before. */
@Composable
internal fun SummaryCards(
    periodKind: AnalyticsPeriod,
    period: Period,
    currency: String,
    totalIncome: Double,
    totalExpense: Double,
    prevIncome: Double,
    prevExpense: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
    ) {
        MonthTotalCard(
            label = "Income",
            amount = compactMoney(currency, totalIncome),
            color = IncomeGreen,
            change = signedPercent(totalIncome, prevIncome)?.let { if (periodKind == AnalyticsPeriod.CUSTOM) it else "$it vs ${period.prevLabel}" },
            modifier = Modifier.weight(1f)
        )
        MonthTotalCard(
            label = "Expense",
            amount = compactMoney(currency, totalExpense),
            color = ExpenseRed,
            change = signedPercent(totalExpense, prevExpense)?.let { if (periodKind == AnalyticsPeriod.CUSTOM) it else "$it vs ${period.prevLabel}" },
            modifier = Modifier.weight(1f)
        )
        MonthTotalCard(
            label = "Saved",
            amount = compactMoney(currency, totalIncome - totalExpense),
            color = if (totalIncome - totalExpense >= 0) IncomeGreen else ExpenseRed,
            change = if (totalIncome > 0) {
                "${((totalIncome - totalExpense) / totalIncome * 100).roundToInt()}% of income"
            } else {
                null
            },
            modifier = Modifier.weight(1f)
        )
    }
}
