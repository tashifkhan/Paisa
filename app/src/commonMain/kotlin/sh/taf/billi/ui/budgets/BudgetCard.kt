package sh.taf.billi.ui.budgets

import sh.taf.billi.ui.transaction.setCleared
import sh.taf.billi.platform.formatDate
import sh.taf.billi.platform.BilliCalendar
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
import sh.taf.billi.data.Budget
import sh.taf.billi.ui.components.CategoryBadge
import sh.taf.billi.ui.format.formatMoney
import sh.taf.billi.ui.theme.spacing
import sh.taf.billi.ui.components.elasticCard
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable

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
    // The bar fills from empty when the card appears, and eases to a new value after that.
    val filled = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        filled.animateTo(progress, tween(durationMillis = 480, easing = FastOutSlowInEasing))
    }
    val periodLabel = runCatching {
        val calendar = BilliCalendar.getInstance().apply {
            setCleared(budget.year, budget.month - 1, 1)
        }
        formatDate(calendar.timeInMillis, "MMMM yyyy")
    }.getOrDefault("${budget.month}/${budget.year}")

    Card(
        modifier = Modifier.fillMaxWidth().elasticCard(),
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
                progress = { filled.value },
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
