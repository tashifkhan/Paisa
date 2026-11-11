package codes.tashif.paisa.ui.debts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import codes.tashif.paisa.ui.format.DEBT_SETTLED
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen


/** Balances smaller than this count as settled. */
internal const val SETTLED = DEBT_SETTLED

/** Green when they owe you, red when you owe them, muted when even. */
@Composable
internal fun balanceColor(balance: Double): Color = when {
    balance >= SETTLED -> IncomeGreen
    balance <= -SETTLED -> ExpenseRed
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
