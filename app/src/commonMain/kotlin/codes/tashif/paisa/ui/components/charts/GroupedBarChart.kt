package codes.tashif.paisa.ui.components.charts

import codes.tashif.paisa.platform.formatFixed
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import codes.tashif.paisa.ui.platform.hoverIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.haptics.rememberHaptics

/**
 * M3 grouped two-series bar chart with rich tooltip on tap.
 */
@Composable
fun GroupedBarChart(
    labels: List<String>,
    seriesA: List<Double>,
    seriesB: List<Double>,
    colorA: Color,
    colorB: Color,
    modifier: Modifier = Modifier,
    nameA: String = "A",
    nameB: String = "B",
    formatValue: (Double) -> String = { formatFixed(it, 0) },
    /** An extra line under the two values, from series A and B at that point. */
    note: ((a: Double, b: Double) -> String?)? = null
) {
    if (labels.isEmpty()) return
    val maxValue = (seriesA + seriesB).maxOrNull()?.takeIf { it > 0 } ?: return
    var selected by remember(labels, seriesA, seriesB) { mutableStateOf<Int?>(null) }
    val haptics = rememberHaptics()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val baselineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val markerColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier.fillMaxWidth()) {
        var chartSize by remember { mutableStateOf(IntSize.Zero) }
        val density = LocalDensity.current.density
        val anchor = selected?.let { index ->
            if (chartSize == IntSize.Zero) return@let null
            val top = 8f * density
            val bottom = chartSize.height - 4f * density
            val slot = chartSize.width / labels.size.toFloat()
            val highest = maxOf(seriesA.getOrElse(index) { 0.0 }, seriesB.getOrElse(index) { 0.0 })
            Offset(index * slot + slot / 2f, bottom - ((highest / maxValue) * (bottom - top)).toFloat() - 6f * density)
        }
        val tooltip: (@Composable () -> Unit)? = selected?.let { index ->
            {
                val a = seriesA.getOrElse(index) { 0.0 }
                val b = seriesB.getOrElse(index) { 0.0 }
                ChartTooltipCard(
                    title = labels[index],
                    lines = listOf(
                        TooltipLine(colorA, nameA, formatValue(a)),
                        TooltipLine(colorB, nameB, formatValue(b))
                    ),
                    note = note?.invoke(a, b)
                )
            }
        }

        ChartWithTooltip(anchor = anchor, tooltip = tooltip) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .onSizeChanged { chartSize = it }
                // A mouse shows the tooltip on hover; touch still taps.
                .hoverIndex(labels, { pos, size -> (pos.x / (size.width / labels.size.toFloat())).toInt().coerceIn(0, labels.lastIndex) }) { selected = it }
                .pointerInput(labels) {
                    detectTapGestures { tap ->
                        val slot = size.width / labels.size.toFloat()
                        val index = (tap.x / slot).toInt().coerceIn(0, labels.lastIndex)
                        val next = if (selected == index) null else index
                        if (next != selected) haptics.tick()
                        selected = next
                    }
                }
        ) {
            val chartTop = 8.dp.toPx()
            val chartBottom = size.height - 4.dp.toPx()
            val chartHeight = chartBottom - chartTop

            for (i in 0..3) {
                val y = chartTop + chartHeight * (i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            drawLine(
                color = baselineColor,
                start = Offset(0f, chartBottom),
                end = Offset(size.width, chartBottom),
                strokeWidth = 1.5.dp.toPx()
            )

            val slot = size.width / labels.size
            val barWidth = (slot * 0.26f).coerceAtMost(14.dp.toPx())
            val gap = 4.dp.toPx()
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

            labels.indices.forEach { index ->
                val dim = selected != null && selected != index
                val isSelected = selected == index
                val centerX = index * slot + slot / 2f
                val aHeight = ((seriesA.getOrElse(index) { 0.0 } / maxValue) * chartHeight)
                    .toFloat()
                val bHeight = ((seriesB.getOrElse(index) { 0.0 } / maxValue) * chartHeight)
                    .toFloat()

                if (aHeight > 0) {
                    val h = aHeight.coerceAtLeast(6.dp.toPx())
                    drawRoundRect(
                        color = if (dim) colorA.copy(alpha = 0.28f) else colorA,
                        topLeft = Offset(centerX - barWidth - gap / 2f, chartBottom - h),
                        size = Size(barWidth, h),
                        cornerRadius = radius
                    )
                }
                if (bHeight > 0) {
                    val h = bHeight.coerceAtLeast(6.dp.toPx())
                    drawRoundRect(
                        color = if (dim) colorB.copy(alpha = 0.28f) else colorB,
                        topLeft = Offset(centerX + gap / 2f, chartBottom - h),
                        size = Size(barWidth, h),
                        cornerRadius = radius
                    )
                }
                if (isSelected) {
                    drawCircle(
                        color = markerColor,
                        radius = 3.dp.toPx(),
                        center = Offset(centerX, chartTop - 2.dp.toPx())
                    )
                }
            }
        }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
