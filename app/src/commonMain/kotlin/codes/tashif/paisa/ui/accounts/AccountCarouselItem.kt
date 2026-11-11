package codes.tashif.paisa.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.theme.spacing

/**
 * Account surface for M3 multi-browse carousel.
 * Shape/size are owned by [Modifier.maskClip] from [CarouselItemScope] so items
 * morph and clip exactly like the m3.material.io demos.
 */
@Composable
internal fun AccountCarouselItem(
    account: Account,
    currency: String,
    index: Int,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Material You: cards take the theme's container colors in turn, so they follow
    // the wallpaper palette instead of each account's fixed color.
    val scheme = MaterialTheme.colorScheme
    val (cardColor, onCard) = when (index % 3) {
        0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        1 -> scheme.secondaryContainer to scheme.onSecondaryContainer
        else -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    }
    val gradient = Brush.linearGradient(
        colors = listOf(cardColor, cardColor.copy(alpha = 0.78f).compositeOver(scheme.surfaceContainerHighest))
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(gradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.type.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = onCard.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onCard,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Account actions",
                        tint = onCard.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = listOfNotNull(
                    account.bankName,
                    account.accountLast4?.let { "•••• $it" }
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = onCard.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            Text(
                text = formatMoney(currency, account.currentBalance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = onCard,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            account.creditLimit?.let { limit ->
                Text(
                    text = "Limit ${formatMoney(currency, limit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = onCard.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
