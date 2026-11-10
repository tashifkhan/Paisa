package codes.tashif.paisa.ui.components.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** One series in a chart tooltip: its colour, its name (or none), and the value. */
data class TooltipLine(val color: Color, val label: String?, val value: String)

/**
 * The card every chart shows on tap or hover: the point's title (a date or
 * a name), one row per series with its colour, and an optional note.
 */
@Composable
fun ChartTooltipCard(
    title: String,
    lines: List<TooltipLine>,
    note: String? = null
) {
    val shape = RoundedCornerShape(14.dp)
    val onCard = MaterialTheme.colorScheme.inverseOnSurface
    Column(
        modifier = Modifier
            .widthIn(min = 120.dp, max = 260.dp)
            .shadow(6.dp, shape)
            .background(MaterialTheme.colorScheme.inverseSurface, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = onCard.copy(alpha = 0.75f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        lines.forEach { line ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                // A thin ring keeps pale series colours visible on the card.
                Box(
                    Modifier
                        .size(9.dp)
                        .background(line.color, CircleShape)
                        .border(1.dp, onCard.copy(alpha = 0.35f), CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                if (line.label != null) {
                    Text(
                        text = line.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = onCard.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(14.dp))
                }
                Text(
                    text = line.value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = onCard,
                    maxLines = 1
                )
            }
        }
        if (!note.isNullOrBlank()) {
            Text(
                text = note,
                style = MaterialTheme.typography.labelSmall,
                color = onCard.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Draws [tooltip] over [content], next to [anchor] (a point in [content]'s
 * pixels): above it when there's room, below otherwise, and kept inside the
 * chart's width. It takes no layout space, so the chart never moves when a
 * tooltip opens, and it ignores touches, so taps still reach the chart.
 */
@Composable
internal fun ChartWithTooltip(
    anchor: Offset?,
    modifier: Modifier = Modifier,
    tooltip: (@Composable () -> Unit)?,
    content: @Composable () -> Unit
) {
    val shown by animateFloatAsState(
        targetValue = if (anchor != null && tooltip != null) 1f else 0f,
        animationSpec = tween(140),
        label = "tooltip_alpha"
    )
    Layout(
        modifier = modifier,
        content = {
            Box { content() }
            Box(
                Modifier.graphicsLayer {
                    alpha = shown
                    val scale = 0.94f + 0.06f * shown
                    scaleX = scale
                    scaleY = scale
                }
            ) {
                if (anchor != null && tooltip != null) tooltip()
            }
        }
    ) { measurables, constraints ->
        val chart = measurables[0].measure(constraints)
        val tip = measurables[1].measure(Constraints(maxWidth = chart.width.coerceAtLeast(0)))
        layout(chart.width, chart.height) {
            chart.place(0, 0)
            if (anchor != null && tip.width > 0) {
                val gap = 10.dp.roundToPx()
                val maxX = (chart.width - tip.width).coerceAtLeast(0)
                val maxY = (chart.height - tip.height).coerceAtLeast(0)
                val above = (anchor.y - tip.height - gap).roundToInt()
                val leftOf = (anchor.x - tip.width - gap).roundToInt()
                val rightOf = (anchor.x + gap).roundToInt()
                val sideY = (anchor.y - tip.height / 2f).roundToInt().coerceIn(0, maxY)
                val (x, y) = when {
                    // Above the point, centred on it.
                    above >= 0 -> (anchor.x - tip.width / 2f).roundToInt().coerceIn(0, maxX) to above
                    // No room above (a tall bar): beside it, on the roomier side.
                    anchor.x >= chart.width / 2f && leftOf >= 0 -> leftOf to sideY
                    rightOf + tip.width <= chart.width -> rightOf to sideY
                    leftOf >= 0 -> leftOf to sideY
                    // Last resort: below it.
                    else -> (anchor.x - tip.width / 2f).roundToInt().coerceIn(0, maxX) to
                        (anchor.y + gap * 1.5f).roundToInt().coerceIn(0, maxY)
                }
                tip.place(x, y, zIndex = 1f)
            }
        }
    }
}
