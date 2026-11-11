package codes.tashif.paisa.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.ui.components.MoneyText
import codes.tashif.paisa.ui.format.dayLabel
import codes.tashif.paisa.ui.theme.spacing

@Composable
internal fun DayHeader(
    day: String,
    currency: String,
    net: Double,
    balancesHidden: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = MaterialTheme.spacing.small,
                bottom = MaterialTheme.spacing.extraSmall
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayLabel(day),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Round to paise first so float noise like 1e-13 doesn't render as "+₹0.00".
        val cents = kotlin.math.round(net * 100)
        if (cents != 0.0) {
            MoneyText(
                currency = currency,
                amount = kotlin.math.abs(cents) / 100,
                hidden = balancesHidden,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                prefix = if (cents > 0) "+" else "−",
                maskCount = 5
            )
        }
    }
}

/** Net money movement for one day: income adds, expenses subtract. */
internal fun List<TransactionWithDetails>.dayNet(): Double = sumOf {
    when (it.type) {
        "income" -> it.amount
        "expense" -> -it.amount
        else -> 0.0
    }
}
