package codes.tashif.paisa.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.components.HiddenMoneyMask
import codes.tashif.paisa.ui.components.MoneyText
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen
import codes.tashif.paisa.ui.theme.spacing

/**
 * Credit cards as a "fuel gauge": the arc shows how much of the combined limit
 * is burned, colored green → amber → red, with available-to-spend as the
 * headline (the number you actually decide with) and per-card rows below.
 */
@Composable
internal fun CreditCardPanel(
    summary: HomeSummary,
    balancesHidden: Boolean = false
) {
    val debt = summary.creditCardDebt
    val limit = summary.creditCardLimit
    val utilization = if (limit > 0) (debt / limit).toFloat().coerceIn(0f, 1f) else null
    val gaugeColor = when {
        utilization == null -> MaterialTheme.colorScheme.primary
        utilization < 0.3f -> IncomeGreen
        utilization < 0.7f -> Color(0xFFFFB74D)
        else -> ExpenseRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Credit cards",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (limit > 0) {
                    MoneyText(
                        currency = summary.currency,
                        amount = (limit - debt).coerceAtLeast(0.0),
                        hidden = balancesHidden,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = gaugeColor,
                        maskCount = 5
                    )
                    Text(
                        text = "available to spend",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                if (balancesHidden) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Owed ",
                            style = if (limit > 0) {
                                MaterialTheme.typography.bodySmall
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                            fontWeight = if (limit > 0) FontWeight.Normal else FontWeight.Bold,
                            color = if (limit > 0) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                ExpenseRed
                            }
                        )
                        HiddenMoneyMask(count = 5, size = if (limit > 0) 7.dp else 9.dp)
                    }
                } else {
                    Text(
                        text = "Owed ${formatMoney(summary.currency, debt)}",
                        style = if (limit > 0) {
                            MaterialTheme.typography.bodySmall
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        fontWeight = if (limit > 0) FontWeight.Normal else FontWeight.Bold,
                        color = if (limit > 0) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            ExpenseRed
                        }
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                summary.creditCards.take(3).forEach { card ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = listOfNotNull(
                                card.bankName ?: card.name,
                                card.accountLast4?.let { "••$it" }
                            ).joinToString(" "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        MoneyText(
                            currency = summary.currency,
                            amount = -card.currentBalance,
                            hidden = balancesHidden,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maskCount = 4
                        )
                    }
                }
            }
            if (utilization != null) {
                CreditGauge(
                    utilization = if (balancesHidden) 0f else utilization,
                    color = gaugeColor,
                    label = if (balancesHidden) null else "${(utilization * 100).toInt()}%",
                    hideLabelAsMask = balancesHidden
                )
            }
        }
    }
}

@Composable
internal fun CreditGauge(
    utilization: Float,
    color: Color,
    label: String?,
    hideLabelAsMask: Boolean = false
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        modifier = Modifier.size(92.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(92.dp)) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2f
            val arcSize = androidx.compose.ui.geometry.Size(
                size.width - stroke,
                size.height - stroke
            )
            // 270° gauge open at the bottom, like a fuel dial
            drawArc(
                color = track,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * utilization,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (hideLabelAsMask) {
                HiddenMoneyMask(count = 3, size = 8.dp)
            } else if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Text(
                text = "used",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
