package codes.tashif.paisa.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.ui.components.MoneyText
import codes.tashif.paisa.ui.theme.spacing

/**
 * M3 multi-shape balance cluster — distinct corner radii per tile (shape scale:
 * extra-large hero, medium cookie eye, large income, asymmetric expense),
 * matching Material You / Expressive "varied shapes" guidance from m3.material.io.
 */
@Composable
internal fun BalanceCard(
    currency: String,
    totalBalance: Double,
    income: Double,
    expense: Double,
    accounts: List<Account> = emptyList(),
    balancesHidden: Boolean = false,
    onToggleBalancesHidden: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    val heroShape = OverviewShapes.hero
    val eyeShape = OverviewShapes.chip
    val incomeShape = OverviewShapes.leftTile
    val expenseShape = OverviewShapes.rightTile
    val breakdownShape = OverviewShapes.breakdown

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(OverviewShapes.gap)
    ) {
        // ── Hero: total balance ──────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = heroShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = onToggleBalancesHidden,
                        modifier = Modifier.size(40.dp),
                        shape = eyeShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (balancesHidden) {
                                Icons.Rounded.VisibilityOff
                            } else {
                                Icons.Rounded.Visibility
                            },
                            contentDescription = if (balancesHidden) {
                                "Show balances"
                            } else {
                                "Hide balances"
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (expanded) {
                                    Icons.Rounded.ExpandLess
                                } else {
                                    Icons.Rounded.ExpandMore
                                },
                                contentDescription = if (expanded) {
                                    "Hide breakdown"
                                } else {
                                    "Show breakdown"
                                },
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                MoneyText(
                    currency = currency,
                    amount = totalBalance,
                    hidden = balancesHidden,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maskCount = 7
                )

                AnimatedVisibility(visible = expanded) {
                    val (creditCards, bankAccounts) = accounts.partition {
                        it.type == "Credit Card"
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MaterialTheme.spacing.small),
                        shape = breakdownShape,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.spacing.small,
                                vertical = MaterialTheme.spacing.smaller
                            )
                        ) {
                            bankAccounts.forEach { account ->
                                BreakdownRow(
                                    label = listOfNotNull(
                                        account.name,
                                        account.accountLast4?.takeIf { it != "WALLET" }
                                            ?.let { "••$it" }
                                    ).joinToString(" "),
                                    currency = currency,
                                    amount = account.currentBalance,
                                    hidden = balancesHidden
                                )
                            }
                            if (creditCards.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                                Text(
                                    text = "Credit cards (not in total)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                                creditCards.forEach { card ->
                                    BreakdownRow(
                                        label = listOfNotNull(
                                            card.name,
                                            card.accountLast4?.let { "••$it" }
                                        ).joinToString(" "),
                                        currency = currency,
                                        amount = card.currentBalance,
                                        hidden = balancesHidden
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Income + Expense companion tiles (distinct shapes) ───────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OverviewShapes.gap)
        ) {
            StatTile(
                modifier = Modifier.weight(1f),
                shape = incomeShape,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                label = "Income",
                currency = currency,
                amount = income,
                hidden = balancesHidden,
                icon = Icons.AutoMirrored.Rounded.TrendingUp
            )
            StatTile(
                modifier = Modifier.weight(1f),
                shape = expenseShape,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                label = "Expense",
                currency = currency,
                amount = expense,
                hidden = balancesHidden,
                icon = Icons.AutoMirrored.Rounded.TrendingDown
            )
        }
    }
}
