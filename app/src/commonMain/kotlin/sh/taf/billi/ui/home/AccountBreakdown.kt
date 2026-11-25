package sh.taf.billi.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sh.taf.billi.data.Account
import sh.taf.billi.ui.components.CategoryVisuals
import sh.taf.billi.ui.components.MoneyText
import sh.taf.billi.ui.format.formatMoneyAmount
import sh.taf.billi.ui.theme.spacing
import kotlin.math.roundToInt

/**
 * The expanded half of the balance card: one row per account with its icon,
 * type and digits, and each one's share of the total. Credit cards follow in
 * their own section, as money owed against the limit.
 */
@Composable
internal fun AccountBreakdown(
    currency: String,
    accounts: List<Account>,
    creditCards: List<Account>,
    hidden: Boolean,
    modifier: Modifier = Modifier
) {
    val onCard = MaterialTheme.colorScheme.onPrimary
    // Shares only make sense against money the user has.
    val positiveTotal = accounts.sumOf { it.currentBalance.coerceAtLeast(0.0) }
    Column(modifier = modifier.fillMaxWidth()) {
        if (accounts.isNotEmpty()) {
            SectionLabel("Accounts", accounts.size, onCard)
            accounts.forEachIndexed { i, account ->
                if (i > 0) RowDivider(onCard)
                val share = if (positiveTotal > 0 && account.currentBalance > 0) {
                    (account.currentBalance / positiveTotal * 100).roundToInt()
                } else {
                    null
                }
                AccountRow(
                    account = account,
                    currency = currency,
                    amount = account.currentBalance,
                    hidden = hidden,
                    trailingNote = share?.let { if (it < 1) "<1%" else "$it%" },
                    contentColor = onCard
                )
            }
        }
        if (creditCards.isNotEmpty()) {
            if (accounts.isNotEmpty()) Spacer(Modifier.height(MaterialTheme.spacing.small))
            SectionLabel("Credit cards · not in total", creditCards.size, onCard)
            creditCards.forEachIndexed { i, card ->
                if (i > 0) RowDivider(onCard)
                val owed = (-card.currentBalance).coerceAtLeast(0.0)
                val limit = card.creditLimit?.takeIf { it > 0 && !hidden }
                AccountRow(
                    account = card,
                    currency = currency,
                    amount = owed,
                    hidden = hidden,
                    trailingNote = "owed",
                    contentColor = onCard,
                    usage = limit?.let { (owed / it).toFloat().coerceIn(0f, 1f) },
                    usageLabel = limit?.let {
                        "${((owed / it) * 100).roundToInt()}% of ${formatMoneyAmount(currency, it)}"
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, count: Int, color: Color) {
    Text(
        text = if (count > 1) "$text · $count" else text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color.copy(alpha = 0.7f),
        modifier = Modifier.padding(
            start = MaterialTheme.spacing.extraSmall,
            top = MaterialTheme.spacing.extraSmall,
            bottom = MaterialTheme.spacing.extraSmall
        )
    )
}

@Composable
private fun RowDivider(color: Color) {
    HorizontalDivider(
        modifier = Modifier.padding(start = 48.dp),
        thickness = 1.dp,
        color = color.copy(alpha = 0.1f)
    )
}

@Composable
private fun AccountRow(
    account: Account,
    currency: String,
    amount: Double,
    hidden: Boolean,
    trailingNote: String?,
    contentColor: Color,
    usage: Float? = null,
    usageLabel: String? = null
) {
    val subtitle = accountSubtitle(account)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.smaller),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(contentColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryVisuals.icon(account.icon),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(MaterialTheme.spacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (usage != null) {
                LinearProgressIndicator(
                    progress = { usage },
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth(0.8f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
            }
        }
        Spacer(Modifier.width(MaterialTheme.spacing.small))
        Column(horizontalAlignment = Alignment.End) {
            MoneyText(
                currency = currency,
                amount = amount,
                hidden = hidden,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maskCount = 5
            )
            // A share of the total gives the hidden amounts away, so it hides with them.
            val note = usageLabel ?: trailingNote?.takeUnless { hidden && it.endsWith("%") }
            if (note != null) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.65f)
                )
            }
        }
    }
}

/** "Bank Account · ••4270", leaving out what the name already says. Null when nothing is left. */
private fun accountSubtitle(account: Account): String? {
    val name = account.name
    val type = account.type.takeIf { it.isNotBlank() && !name.contains(it, ignoreCase = true) }
    val digits = account.accountLast4
        ?.takeIf { it.isNotBlank() && it != "WALLET" && !name.contains(it) }
        ?.let { "••$it" }
    return listOfNotNull(type, digits).joinToString(" · ").ifBlank { null }
}

/**
 * An account's name with its last digits, unless the name already ends in them
 * ("SBI SimplyCLICK ····5304" stays as it is instead of gaining "••5304").
 */
internal fun accountNameWithDigits(name: String?, last4: String?): String {
    val base = name.orEmpty()
    val digits = last4?.takeIf { it.isNotBlank() && it != "WALLET" && !base.contains(it) } ?: return base
    return if (base.isBlank()) "••$digits" else "$base ••$digits"
}
