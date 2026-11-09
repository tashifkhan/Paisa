package codes.tashif.paisa.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.ui.components.MoneyText
import codes.tashif.paisa.ui.format.debtStatus
import codes.tashif.paisa.ui.format.debtTotals
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen
import codes.tashif.paisa.ui.theme.spacing
import kotlin.math.abs

/**
 * Money between you and other people, from Debts: totals and the biggest
 * balances. Tapping opens the Debts tab.
 */
@Composable
internal fun DebtsPanel(
    people: List<DebtPersonBalance>,
    currency: String,
    balancesHidden: Boolean,
    onOpen: () -> Unit
) {
    val (owedToMe, iOwe) = debtTotals(people)
    val open = people.filter { abs(it.balance) >= 0.005 }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Text(
                text = "Debts",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    MoneyText(
                        currency = currency,
                        amount = owedToMe,
                        hidden = balancesHidden,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen,
                        maskCount = 5
                    )
                    Text(
                        text = "owed to you",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (iOwe >= 0.005) {
                    Column(modifier = Modifier.weight(1f)) {
                        MoneyText(
                            currency = currency,
                            amount = iOwe,
                            hidden = balancesHidden,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed,
                            maskCount = 5
                        )
                        Text(
                            text = "you owe",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            open.take(4).forEach { p ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${p.person.name} · ${debtStatus(p.balance).lowercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    MoneyText(
                        currency = currency,
                        amount = abs(p.balance),
                        hidden = balancesHidden,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maskCount = 4
                    )
                }
            }
        }
    }
}
