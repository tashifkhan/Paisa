package sh.taf.billi.ui.debts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sh.taf.billi.ui.components.MoneyText
import sh.taf.billi.ui.components.elasticCard
import sh.taf.billi.ui.home.OverviewShapes
import sh.taf.billi.ui.theme.spacing
import kotlin.math.abs

/**
 * The top of Debts, shaped like Home's balance cluster: the net balance as a
 * hero, then one tile per direction
 * with how many people are in it.
 */
@Composable
internal fun DebtTotalsCard(
    currency: String,
    owedToMe: Double,
    iOwe: Double,
    owingCount: Int = 0,
    owedCount: Int = 0
) {
    val net = owedToMe - iOwe
    val onHero = MaterialTheme.colorScheme.onPrimary
    Column(verticalArrangement = Arrangement.spacedBy(OverviewShapes.gap)) {
        Surface(
            modifier = Modifier.fillMaxWidth().elasticCard(),
            shape = OverviewShapes.hero,
            color = MaterialTheme.colorScheme.primary
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
                Text(
                    text = when {
                        net >= SETTLED -> "You're owed overall"
                        net <= -SETTLED -> "You owe overall"
                        else -> "All square"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = onHero.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(6.dp))
                MoneyText(
                    currency = currency,
                    amount = abs(net),
                    hidden = false,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = onHero,
                    maskCount = 7,
                    animated = true
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(OverviewShapes.gap)) {
            DirectionTile(
                modifier = Modifier.weight(1f),
                shape = OverviewShapes.leftTile,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                icon = Icons.AutoMirrored.Rounded.CallReceived,
                label = "Owed to you",
                currency = currency,
                amount = owedToMe,
                people = owingCount
            )
            DirectionTile(
                modifier = Modifier.weight(1f),
                shape = OverviewShapes.rightTile,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                icon = Icons.AutoMirrored.Rounded.CallMade,
                label = "You owe",
                currency = currency,
                amount = iOwe,
                people = owedCount
            )
        }
    }
}

@Composable
private fun DirectionTile(
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector,
    label: String,
    currency: String,
    amount: Double,
    people: Int
) {
    Surface(modifier = modifier.elasticCard(), shape = shape, color = containerColor) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = contentColor.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = contentColor.copy(alpha = 0.85f))
            }
            Spacer(Modifier.height(6.dp))
            MoneyText(
                currency = currency,
                amount = amount,
                hidden = false,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (amount >= SETTLED) contentColor else contentColor.copy(alpha = 0.6f),
                maskCount = 5,
                animated = true
            )
            Text(
                text = when (people) {
                    0 -> "Nobody"
                    1 -> "1 person"
                    else -> "$people people"
                },
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}
