package codes.tashif.paisa.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen
import codes.tashif.paisa.ui.theme.spacing

/** One transaction card: category badge, title, details line and signed amount. */
@Composable
fun TransactionRow(
    tx: TransactionWithDetails,
    currency: String,
    balancesHidden: Boolean = false,
    onClick: () -> Unit = {}
) {
    val isIncome = tx.type == "income"
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val sign = if (isIncome) "+" else "−"
    val sourceLabel = when (tx.source) {
        "sms" -> "SMS"
        "statement" -> "Statement"
        else -> null
    }
    val haptics = rememberHaptics()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.click()
                onClick()
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryBadge(
                iconName = tx.categoryIcon,
                categoryName = tx.categoryName,
                colorHex = tx.categoryColor,
                size = 42.dp
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.merchantName?.takeIf { it.isNotBlank() }
                        ?: tx.note.takeIf { it.isNotBlank() }
                        ?: tx.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = listOfNotNull(
                        tx.categoryName,
                        tx.accountName,
                        tx.bankName?.takeIf { tx.source != "manual" },
                        sourceLabel
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MoneyText(
                currency = currency,
                amount = tx.amount,
                hidden = balancesHidden,
                style = MaterialTheme.typography.titleSmall,
                color = amountColor,
                fontWeight = FontWeight.Bold,
                prefix = sign,
                maskCount = 5
            )
        }
    }
}
