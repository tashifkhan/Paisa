package codes.tashif.paisa.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.components.MoneyText
import codes.tashif.paisa.ui.theme.spacing

@Composable
internal fun StatTile(
    modifier: Modifier = Modifier,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    label: String,
    currency: String,
    amount: Double,
    hidden: Boolean,
    icon: ImageVector
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor.copy(alpha = 0.85f)
                )
            }
            Spacer(Modifier.height(6.dp))
            MoneyText(
                currency = currency,
                amount = amount,
                hidden = hidden,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maskCount = 5
            )
        }
    }
}

@Composable
internal fun BreakdownRow(
    label: String,
    currency: String,
    amount: Double,
    hidden: Boolean,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    amountColor: Color = contentColor,
    prefix: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.9f),
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        MoneyText(
            currency = currency,
            amount = amount,
            hidden = hidden,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = amountColor,
            prefix = prefix,
            maskCount = 5
        )
    }
}

/**
 * The shape set every Home cluster shares (balance, credit cards, debts), so the
 * three read as one family: a hero with big top corners, then two tiles whose
 * outer bottom corners curve in to close the cluster.
 */
internal object OverviewShapes {
    val hero = RoundedCornerShape(topStart = 32.dp, topEnd = 28.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    /** A hero with no tiles under it closes itself. */
    val heroAlone = RoundedCornerShape(topStart = 32.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
    val chip = RoundedCornerShape(topStart = 18.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 18.dp)
    val leftTile = RoundedCornerShape(topStart = 12.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 12.dp)
    val rightTile = RoundedCornerShape(topStart = 8.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 28.dp)
    val breakdown = RoundedCornerShape(16.dp)
    val gap = 6.dp
}
