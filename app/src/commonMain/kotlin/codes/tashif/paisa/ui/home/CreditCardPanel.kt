package codes.tashif.paisa.ui.home

import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.graphics.StrokeCap

import androidx.compose.ui.geometry.Size

import androidx.compose.ui.geometry.Offset

import androidx.compose.foundation.Canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.ui.components.HiddenMoneyMask
import codes.tashif.paisa.ui.components.InfoTip
import codes.tashif.paisa.ui.components.MoneyText
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen
import codes.tashif.paisa.ui.theme.spacing

private val UsageAmber = Color(0xFFFFB74D)

/**
 * Credit cards in one compact card, in the balance card's shape language:
 * what you can still spend, a usage bar, and owed / limit on one line. With
 * more than one card it expands into a per-card breakdown, like the balance hero.
 */
@Composable
internal fun CreditCardPanel(
    summary: HomeSummary,
    balancesHidden: Boolean = false
) {
    val debt = summary.creditCardDebt
    val limit = summary.creditCardLimit
    val hasLimit = limit > 0
    val utilization = if (hasLimit) (debt / limit).toFloat().coerceIn(0f, 1f) else 0f
    val usageColor = when {
        utilization < 0.3f -> IncomeGreen
        utilization < 0.7f -> UsageAmber
        else -> ExpenseRed
    }
    val canExpand = summary.creditCards.size > 1
    var expanded by remember { mutableStateOf(false) }
    val onCard = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (canExpand) Modifier.clickable { expanded = !expanded } else Modifier),
        shape = OverviewShapes.heroAlone,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Credit cards",
                            style = MaterialTheme.typography.labelLarge,
                            color = onCard.copy(alpha = 0.75f)
                        )
                        InfoTip(
                            title = "Credit usage",
                            text = "Available is your combined limit minus what you owe on every card. " +
                                "The dial shows how much of the limit is in use. Keeping it under " +
                                "about 30% is usually better for your credit score.",
                            tint = onCard.copy(alpha = 0.6f)
                        )
                        if (canExpand) {
                            Spacer(Modifier.width(4.dp))
                            ExpandChip(expanded = expanded, tint = onCard)
                        }
                    }
                    MoneyText(
                        currency = summary.currency,
                        amount = if (hasLimit) (limit - debt).coerceAtLeast(0.0) else debt,
                        hidden = balancesHidden,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onCard,
                        maskCount = 6
                    )
                    Text(
                        text = if (hasLimit) "available to spend" else "owed across your cards",
                        style = MaterialTheme.typography.labelMedium,
                        color = onCard.copy(alpha = 0.65f)
                    )
                    if (hasLimit) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MoneyText(
                                currency = summary.currency,
                                amount = debt,
                                hidden = balancesHidden,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = onCard.copy(alpha = 0.85f),
                                maskCount = 4
                            )
                            Text(
                                " owed of ",
                                style = MaterialTheme.typography.labelMedium,
                                color = onCard.copy(alpha = 0.65f)
                            )
                            MoneyText(
                                currency = summary.currency,
                                amount = limit,
                                hidden = balancesHidden,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = onCard.copy(alpha = 0.85f),
                                maskCount = 4
                            )
                        }
                    }
                }
                if (hasLimit) {
                    Spacer(Modifier.width(MaterialTheme.spacing.small))
                    UsageDial(
                        utilization = utilization,
                        color = usageColor,
                        track = onCard.copy(alpha = 0.10f),
                        hidden = balancesHidden
                    )
                }
            }

            if (canExpand) {
                AnimatedVisibility(visible = expanded) {
                    CardBreakdown(
                        cards = summary.creditCards,
                        currency = summary.currency,
                        hidden = balancesHidden,
                        tint = onCard
                    )
                }
            }
        }
    }
}

/**
 * The fuel-dial gauge: a 270° arc open at the bottom, filled by how much of
 * the limit is used, green under 30%, amber under 70%, red above. The
 * percentage sits in the middle; hidden balances empty the dial and mask it.
 */
@Composable
private fun UsageDial(utilization: Float, color: Color, track: Color, hidden: Boolean) {
    val fill by animateFloatAsState(if (hidden) 0f else utilization, label = "credit dial")
    Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(84.dp)) {
            val stroke = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2f
            val arc = Size(size.width - stroke.width, size.height - stroke.width)
            drawArc(track, startAngle = 135f, sweepAngle = 270f, useCenter = false, topLeft = Offset(inset, inset), size = arc, style = stroke)
            if (fill > 0f) {
                drawArc(color, startAngle = 135f, sweepAngle = 270f * fill, useCenter = false, topLeft = Offset(inset, inset), size = arc, style = stroke)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (hidden) {
                HiddenMoneyMask(count = 3, size = 7.dp)
            } else {
                Text(
                    text = "${(utilization * 100).toInt()}%",
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

/** A small expand chip for tonal cards, after the one on the balance hero. */
@Composable
internal fun ExpandChip(expanded: Boolean, tint: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.08f),
        modifier = Modifier.size(30.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Hide breakdown" else "Show breakdown",
                tint = tint.copy(alpha = 0.9f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CardBreakdown(cards: List<Account>, currency: String, hidden: Boolean, tint: Color) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.spacing.smaller),
        shape = OverviewShapes.breakdown,
        color = tint.copy(alpha = 0.06f)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.smaller
            )
        ) {
            cards.forEach { card ->
                BreakdownRow(
                    label = listOfNotNull(
                        card.bankName ?: card.name,
                        card.accountLast4?.let { "••$it" }
                    ).joinToString(" "),
                    currency = currency,
                    // A negative balance is money owed; show it as a positive debt.
                    amount = (-card.currentBalance).coerceAtLeast(0.0),
                    hidden = hidden,
                    contentColor = tint
                )
            }
        }
    }
}
