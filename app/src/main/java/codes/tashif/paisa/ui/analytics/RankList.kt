package codes.tashif.paisa.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.components.CategoryBadge
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing
import kotlin.math.roundToInt

@Composable
internal fun RankList(ranks: List<RankItem>, currency: String, onClick: (AnalyticsGroup) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ranks.forEach { rank -> RankRow(rank, currency, onClick = { onClick(rank.group) }) }
    }
}

/**
 * One ranked row: a meaningful icon, the name with a plain second line, the
 * amount and its share on the right, and a slim bar in the row's own colour.
 * Tapping opens its transactions.
 */
@Composable
internal fun RankRow(rank: RankItem, currency: String, onClick: () -> Unit) {
    val haptics = rememberHaptics()
    val (container, content, toneAccent) = rankTone(rank.tone)
    val accent = when {
        rank.color != Color.Unspecified -> rank.color
        rank.icon != null -> toneAccent
        else -> MaterialTheme.colorScheme.primary
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable {
                haptics.click()
                onClick()
            }
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RankAvatar(rank, container, content, 40.dp)
            Spacer(Modifier.width(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rank.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                rank.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(MaterialTheme.spacing.smaller))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatMoney(currency, rank.amount).removeSuffix(".00"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(rank.share * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .padding(start = 40.dp + MaterialTheme.spacing.small)
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(rank.share.coerceIn(0.02f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
        }
    }
}

/** Material You container, content and accent colours, taken in turn by [tone]. */
@Composable
private fun rankTone(tone: Int): Triple<Color, Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (tone % 3) {
        0 -> Triple(c.primaryContainer, c.onPrimaryContainer, c.primary)
        1 -> Triple(c.secondaryContainer, c.onSecondaryContainer, c.secondary)
        else -> Triple(c.tertiaryContainer, c.onTertiaryContainer, c.tertiary)
    }
}

@Composable
private fun RankAvatar(rank: RankItem, container: Color, content: Color, size: Dp) {
    when {
        rank.iconName != null ->
            CategoryBadge(iconName = rank.iconName, categoryName = rank.label, colorHex = rank.colorHex, size = size)
        rank.icon != null -> Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center
        ) {
            Icon(rank.icon, contentDescription = null, tint = content, modifier = Modifier.size(size * 0.5f))
        }
        else -> GroupAvatar(rank.label, null, null, MaterialTheme.colorScheme.primary, size)
    }
}

@Composable
internal fun GroupAvatar(label: String, iconName: String?, colorHex: String?, accent: Color, size: Dp) {
    if (iconName != null) {
        CategoryBadge(iconName = iconName, categoryName = label, colorHex = colorHex, size = size)
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.take(1).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun SeeAllButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text)
    }
}
