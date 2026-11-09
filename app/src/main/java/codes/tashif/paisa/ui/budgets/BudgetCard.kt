package codes.tashif.paisa.ui.budgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.Budget
import codes.tashif.paisa.ui.components.CategoryBadge
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.theme.spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BudgetCard(
    budget: Budget,
    title: String,
    spent: Double,
    currency: String,
    categoryIcon: String? = null,
    categoryName: String? = null,
    categoryColor: String? = null
) {
    val progress = if (budget.budgetAmount > 0) {
        (spent / budget.budgetAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val overBudget = spent > budget.budgetAmount
    val periodLabel = runCatching {
        val calendar = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, budget.year)
            set(Calendar.MONTH, budget.month - 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    }.getOrDefault("${budget.month}/${budget.year}")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(
                    iconName = categoryIcon,
                    categoryName = categoryName ?: title,
                    colorHex = categoryColor,
                    size = 42.dp
                )
                Spacer(Modifier.size(MaterialTheme.spacing.small))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${formatMoney(currency, spent)} / " +
                        formatMoney(currency, budget.budgetAmount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (overBudget) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.smaller))
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (overBudget) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            if (overBudget) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                Text(
                    text = "Over budget by ${formatMoney(currency, spent - budget.budgetAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
