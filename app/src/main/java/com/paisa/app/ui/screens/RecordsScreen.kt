package com.paisa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paisa.app.data.PaisaViewModel
import com.paisa.app.data.TransactionWithDetails
import com.paisa.app.ui.components.EmptyState
import com.paisa.app.ui.components.PaisaTopBar
import com.paisa.app.ui.theme.ExpenseRed
import com.paisa.app.ui.theme.IncomeGreen
import com.paisa.app.ui.theme.spacing
import java.util.Locale

@Composable
fun RecordsScreen(viewModel: PaisaViewModel) {
    val summary by viewModel.homeSummary.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        PaisaTopBar(title = "Paisa")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                bottom = MaterialTheme.spacing.extraLarge
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
        ) {
            item {
                BalanceCard(
                    currency = summary.currency,
                    totalBalance = summary.totalBalance,
                    income = summary.monthIncome,
                    expense = summary.monthExpense
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = MaterialTheme.spacing.smaller)
                )
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyState(
                        title = "No transactions yet",
                        subtitle = "Tap + to record your first expense or income"
                    )
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    TransactionRow(tx = tx, currency = summary.currency)
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(
    currency: String,
    totalBalance: Double,
    income: Double,
    expense: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
            Text(
                text = "Total balance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatMoney(currency, totalBalance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Text(
                        text = formatMoney(currency, income),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Text(
                        text = formatMoney(currency, expense),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: TransactionWithDetails, currency: String) {
    val isIncome = tx.type == "income"
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val sign = if (isIncome) "+" else "−"
    val chipColor = runCatching { Color(android.graphics.Color.parseColor(tx.categoryColor)) }
        .getOrDefault(MaterialTheme.colorScheme.primaryContainer)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(chipColor.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tx.categoryName.take(1).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = listOfNotNull(
                        tx.accountName,
                        tx.note.takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            Text(
                text = "$sign${formatMoney(currency, tx.amount)}",
                style = MaterialTheme.typography.titleSmall,
                color = amountColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

internal fun formatMoney(currency: String, amount: Double): String {
    return String.format(Locale.getDefault(), "%s%,.2f", currency, amount)
}
