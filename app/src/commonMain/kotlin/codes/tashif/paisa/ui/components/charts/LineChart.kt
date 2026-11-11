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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import codes.tashif.paisa.ui.platform.hoverIndex
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.haptics.rememberHaptics

/**
 * Two-series line chart for running totals, such as this month's spending
 * against last month's. Null points are not drawn, so a series can stop early
 * (today, in the current month). Tap to read both values on that day.
 */
@Composable
fun LineChart(
    labels: List<String>,
    seriesA: List<Double?>,
    seriesB: List<Double?>,
    colorA: Color,
    colorB: Color,
    modifier: Modifier = Modifier,
    nameA: String = "A",
    nameB: String = "B",
    formatValue: (Double) -> String = { formatFixed(it, 0) },
    tooltipLabel: (Int) -> String = { labels.getOrElse(it) { "" } }
) {
    if (labels.isEmpty()) return
    val maxValue = (seriesA + seriesB).filterNotNull().maxOrNull()?.takeIf { it > 0 } ?: return
    var selected by remember(labels, seriesA, seriesB) { mutableStateOf<Int?>(null) }
    val haptics = rememberHaptics()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val markerColor = MaterialTheme.colorScheme.outline

    Column(modifier = modifier.fillMaxWidth()) {
        var chartSize by remember { mutableStateOf(IntSize.Zero) }
        val anchor = selected?.let { index ->
            if (chartSize == IntSize.Zero) return@let null
            val density = LocalDensity.current.density
            val top = 8f * density
            val bottom = chartSize.height - 4f * density
            val step = if (labels.size > 1) chartSize.width / (labels.size - 1f) else 0f
            val highest = listOfNotNull(seriesA.getOrNull(index), seriesB.getOrNull(index)).maxOrNull()
            val y = highest?.let { bottom - (it / maxValue).toFloat() * (bottom - top) } ?: bottom
            Offset(index * step, y)
        }
        val tooltip: (@Composable () -> Unit)? = selected?.let { index ->
            {
                val a = seriesA.getOrNull(index)
                val b = seriesB.getOrNull(index)
                ChartTooltipCard(
                    title = tooltipLabel(index),
                    lines = listOfNotNull(
                        a?.let { TooltipLine(colorA, nameA, formatValue(it)) },
                        b?.let { TooltipLine(colorB, nameB, formatValue(it)) }
                    ),
                    note = when {
                        a != null && b != null && a != b ->
                            "${formatValue(kotlin.math.abs(a - b))} ${if (a > b) "more" else "less"} than $nameB"
                        a == null && b != null -> "$nameA hasn't reached this day"
                        else -> null
                    }
                )
            }
        }

        ChartWithTooltip(anchor = anchor, tooltip = tooltip) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .onSizeChanged { chartSize = it }
                // A mouse shows the tooltip on hover; touch still taps.
                .hoverIndex(labels, { pos, size -> (pos.x / (if (labels.size > 1) size.width / (labels.size - 1).toFloat() else size.width.toFloat()) + 0.5f).toInt().coerceIn(0, labels.lastIndex) }) { selected = it }
                .pointerInput(labels) {
                    detectTapGestures { tap ->
                        val step = if (labels.size > 1) size.width / (labels.size - 1).toFloat() else size.width.toFloat()
                        val index = (tap.x / step + 0.5f).toInt().coerceIn(0, labels.lastIndex)
                        val next = if (selected == index) null else index
                        if (next != selected) haptics.tick()
                        selected = next
                    }
                }
        ) {
            val top = 8.dp.toPx()
            val bottom = size.height - 4.dp.toPx()
            val height = bottom - top
            for (i in 0..3) {
                val y = top + height * (i / 3f)
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            val step = if (labels.size > 1) size.width / (labels.size - 1) else 0f
            fun point(i: Int, v: Double) = Offset(i * step, bottom - (v / maxValue).toFloat() * height)

            fun drawSeries(values: List<Double?>, color: Color, fill: Boolean, dashed: Boolean) {
                val pts = values.mapIndexedNotNull { i, v -> v?.let { point(i, it) } }
                if (pts.isEmpty()) return
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                }
                if (fill && pts.size > 1) {
                    val area = androidx.compose.ui.graphics.Path().apply {
                        addPath(path)
                        lineTo(pts.last().x, bottom)
                        lineTo(pts.first().x, bottom)
                        close()
                    }
                    drawPath(
                        area,
                        Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0f)), startY = top, endY = bottom)
                    )
                }
                drawPath(
                    path,
                    color,
                    style = Stroke(
                        width = if (dashed) 2.dp.toPx() else 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                        pathEffect = if (dashed) {
                            androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
                        } else {
                            null
                        }
                    )
                )
            }
            drawSeries(seriesB, colorB, fill = false, dashed = true)
            drawSeries(seriesA, colorA, fill = true, dashed = false)

            selected?.let { index ->
                val x = index * step
                drawLine(markerColor, Offset(x, top), Offset(x, bottom), strokeWidth = 1.dp.toPx())
                seriesB.getOrNull(index)?.let { drawCircle(colorB, 4.dp.toPx(), point(index, it)) }
                seriesA.getOrNull(index)?.let { drawCircle(colorA, 5.dp.toPx(), point(index, it)) }
            }
        }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(0, labels.lastIndex / 2, labels.lastIndex).distinct().forEach { i ->
                Text(
                    text = labels[i],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
