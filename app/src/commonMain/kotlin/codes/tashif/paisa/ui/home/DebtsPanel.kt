package codes.tashif.paisa.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.ui.components.MoneyText
import codes.tashif.paisa.ui.format.debtTotals
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen
import codes.tashif.paisa.ui.theme.spacing
import kotlin.math.abs

/**
 * Money between you and other people in one compact card: the net position
 * and owed to you / you owe on one line. No names here; tapping opens Debts,
 * where the people are.
 */
@Composable
internal fun DebtsPanel(
    people: List<DebtPersonBalance>,
    currency: String,
    balancesHidden: Boolean,
    onOpen: () -> Unit
) {
    val (owedToMe, iOwe) = debtTotals(people)
    val net = owedToMe - iOwe
    val onCard = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = OverviewShapes.heroAlone,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Text(
                text = "Debts",
                style = MaterialTheme.typography.labelLarge,
                color = onCard.copy(alpha = 0.75f)
            )
            Row(verticalAlignment = Alignment.Bottom) {
                MoneyText(
                    currency = currency,
                    amount = abs(net),
                    hidden = balancesHidden,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = onCard,
                    maskCount = 6
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        net >= 0.005 -> "owed to you overall"
                        net <= -0.005 -> "you owe overall"
                        else -> "even overall"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = onCard.copy(alpha = 0.65f),
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Total("Owed to you", owedToMe, IncomeGreen, currency, balancesHidden, onCard)
                Text("  ·  ", style = MaterialTheme.typography.labelMedium, color = onCard.copy(alpha = 0.4f))
                Total("You owe", iOwe, ExpenseRed, currency, balancesHidden, onCard)
            }
        }
    }
}

@Composable
private fun Total(label: String, amount: Double, color: Color, currency: String, hidden: Boolean, tint: Color) {
    Text("$label ", style = MaterialTheme.typography.labelMedium, color = tint.copy(alpha = 0.65f))
    MoneyText(
        currency = currency,
        amount = amount,
        hidden = hidden,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (amount >= 0.005) color else tint.copy(alpha = 0.65f),
        maskCount = 4
    )
}
