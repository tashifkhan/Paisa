package codes.tashif.paisa.ui.components.charts

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
    formatValue: (Double) -> String = { "%.0f".format(it) }
) {
    if (labels.isEmpty()) return
    val maxValue = (seriesA + seriesB).maxOrNull()?.takeIf { it > 0 } ?: return
    var selected by remember(labels, seriesA, seriesB) { mutableStateOf<Int?>(null) }
    val haptics = rememberHaptics()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val baselineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val markerColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier.fillMaxWidth()) {
        ChartTooltipSlot(visible = selected != null) {
            selected?.let { index ->
                val a = seriesA.getOrElse(index) { 0.0 }
                val b = seriesB.getOrElse(index) { 0.0 }
                Md3ChartTooltip(
                    title = labels[index],
                    value = "$nameA ${formatValue(a)}",
                    supporting = "$nameB ${formatValue(b)}",
                    accent = colorA
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
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
