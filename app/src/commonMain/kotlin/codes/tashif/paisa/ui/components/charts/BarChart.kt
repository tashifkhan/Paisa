package codes.tashif.paisa.ui.components.charts

import codes.tashif.paisa.platform.formatFixed
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.haptics.rememberHaptics

/**
 * M3 vertical bar chart — rounded tops, subtle grid, rich tooltip on tap.
 */
@Composable
fun BarChart(
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightEvery: Int = 5,
    formatValue: (Double) -> String = { formatFixed(it, 0) },
    tooltipLabel: (ChartEntry) -> String = { it.label },
    supportingLabel: (ChartEntry) -> String = { if (it.value > 0) "Daily spend" else "No spend" }
) {
    if (entries.isEmpty()) return
    val maxValue = entries.maxOf { it.value }.takeIf { it > 0 } ?: return
    var selected by remember(entries) { mutableStateOf<Int?>(null) }
    val haptics = rememberHaptics()
    val primary = barColor
    val selectedColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val baselineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Column(modifier = modifier.fillMaxWidth()) {
        ChartTooltipSlot(visible = selected != null) {
            selected?.let { index ->
                entries.getOrNull(index)?.let { entry ->
                    Md3ChartTooltip(
                        title = tooltipLabel(entry),
                        value = formatValue(entry.value),
                        supporting = supportingLabel(entry),
                        accent = if (entry.color == Color.Unspecified) primary else entry.color
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .pointerInput(entries) {
                    detectTapGestures { tap ->
                        val slot = size.width / entries.size.toFloat()
                        val index = (tap.x / slot).toInt().coerceIn(0, entries.lastIndex)
                        val next = if (selected == index) null else index
                        if (next != selected) haptics.tick()
                        selected = next
                    }
                }
        ) {
            val chartTop = 8.dp.toPx()
            val chartBottom = size.height - 4.dp.toPx()
            val chartHeight = chartBottom - chartTop

            // Horizontal guide lines (M3 outline-variant grid)
            for (i in 0..3) {
                val y = chartTop + chartHeight * (i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            // Baseline
            drawLine(
                color = baselineColor,
                start = Offset(0f, chartBottom),
                end = Offset(size.width, chartBottom),
                strokeWidth = 1.5.dp.toPx()
            )

            val slot = size.width / entries.size
            val barWidth = (slot * 0.55f).coerceIn(4.dp.toPx(), 18.dp.toPx())
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

            entries.forEachIndexed { index, entry ->
                val isSelected = selected == index
                val dimmed = selected != null && !isSelected
                val barHeight = if (entry.value <= 0) {
                    0f
                } else {
                    ((entry.value / maxValue) * chartHeight).toFloat()
                        .coerceAtLeast(6.dp.toPx())
                }
                val color = when {
                    entry.color != Color.Unspecified && !isSelected -> entry.color
                    isSelected -> selectedColor
                    else -> primary
                }.let { if (dimmed) it.copy(alpha = 0.28f) else it }

                if (barHeight > 0f) {
                    val left = index * slot + (slot - barWidth) / 2f
                    val top = chartBottom - barHeight
                    // Pill-shaped bar (fully rounded) — M3 expressive bars
                    drawRoundRect(
                        brush = if (isSelected) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    color,
                                    color.copy(alpha = 0.75f)
                                ),
                                startY = top,
                                endY = chartBottom
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(color, color),
                                startY = top,
                                endY = chartBottom
                            )
                        },
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = radius
                    )
                    if (isSelected) {
                        // Selection marker above bar
                        drawCircle(
                            color = selectedColor,
                            radius = 3.5.dp.toPx(),
                            center = Offset(left + barWidth / 2f, top - 6.dp.toPx())
                        )
                    }
                }
            }
        }

        if (highlightEvery > 0) {
            val shown = entries.filterIndexed { index, _ ->
                index == 0 || index == entries.lastIndex || (index + 1) % highlightEvery == 0
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                shown.forEach { entry ->
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
