package codes.tashif.paisa.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.haptics.rememberHaptics
import kotlin.math.atan2
import kotlin.math.hypot

/** One slice/bar of chart data. */
data class ChartEntry(
    val label: String,
    val value: Double,
    val color: Color
)

/**
 * Material 3 rich-tooltip surface for chart interactions.
 * Matches M3 tooltip tokens: elevated container, rounded shape, title + supporting text.
 */
@Composable
fun Md3ChartTooltip(
    title: String,
    value: String,
    supporting: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        // Wide enough for long category labels; still caps so it doesn't span the full card.
        modifier = modifier.widthIn(min = 96.dp, max = 320.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        tonalElevation = 0.dp,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!supporting.isNullOrBlank()) {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.75f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartTooltipSlot(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    // Min height keeps layout stable when empty; no max so multi-line tooltips aren't clipped.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f)
        ) {
            content()
        }
    }
}

/**
 * M3-style donut chart. Tap a slice for a rich tooltip; center shows total or selection.
 */
@Composable
fun DonutChart(
    entries: List<ChartEntry>,
    centerTitle: String,
    centerSubtitle: String,
    modifier: Modifier = Modifier,
    formatValue: (Double) -> String = { "%.0f".format(it) }
) {
    val total = entries.sumOf { it.value }.takeIf { it > 0 } ?: return
    var selected by remember(entries) { mutableStateOf<Int?>(null) }
    val haptics = rememberHaptics()
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val gapDegrees = if (entries.size > 1) 3f else 0f
    val pick = selected?.let { entries.getOrNull(it) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChartTooltipSlot(visible = pick != null) {
            pick?.let { entry ->
                Md3ChartTooltip(
                    title = entry.label,
                    value = formatValue(entry.value),
                    supporting = "${(entry.value / total * 100).toInt()}% of spending",
                    accent = entry.color
                )
            }
        }

        Box(
            modifier = Modifier
                .size(200.dp)
                .pointerInput(entries) {
                    detectTapGestures { tap ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val strokePx = 28.dp.toPx()
                        val outer = size.width.coerceAtMost(size.height) / 2f
                        val inner = outer - strokePx * 1.5f
                        val dist = hypot(tap.x - cx, tap.y - cy)
                        if (dist !in inner..outer + strokePx / 2f) {
                            selected = null
                            return@detectTapGestures
                        }
                        var angle = Math.toDegrees(
                            atan2((tap.y - cy).toDouble(), (tap.x - cx).toDouble())
                        ) + 90.0
                        if (angle < 0) angle += 360.0
                        var cumulative = 0.0
                        val hit = entries.indexOfFirst { entry ->
                            val sweep = entry.value / total * 360.0
                            val inSlice = angle >= cumulative && angle < cumulative + sweep
                            cumulative += sweep
                            inSlice
                        }
                        val next = when {
                            hit == -1 -> null
                            hit == selected -> null
                            else -> hit
                        }
                        if (next != selected) haptics.tick()
                        selected = next
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val stroke = 28.dp.toPx()
                val diameter = size.minDimension - stroke
                val topLeft = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f
                )
                // Soft track ring (M3 surface container)
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                var startAngle = -90f
                entries.forEachIndexed { index, entry ->
                    val rawSweep = ((entry.value / total) * 360f).toFloat()
                    val sweep = (rawSweep - gapDegrees).coerceAtLeast(0f)
                    if (sweep > 0f) {
                        val isSelected = selected == index
                        val dimmed = selected != null && !isSelected
                        drawArc(
                            color = if (dimmed) entry.color.copy(alpha = 0.28f) else entry.color,
                            startAngle = startAngle + gapDegrees / 2f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(
                                width = if (isSelected) stroke * 1.18f else stroke,
                                cap = StrokeCap.Butt
                            )
                        )
                    }
                    startAngle += rawSweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (pick != null) {
                    Text(
                        text = pick.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatValue(pick.value),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = centerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = centerSubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * M3 vertical bar chart — rounded tops, subtle grid, rich tooltip on tap.
 */
@Composable
fun BarChart(
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightEvery: Int = 5,
    formatValue: (Double) -> String = { "%.0f".format(it) },
    tooltipLabel: (ChartEntry) -> String = { it.label }
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
                        supporting = if (entry.value > 0) "Daily spend" else "No spend",
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

/**
 * Month calendar heatmap with M3 cells and rich tooltip on tap.
 */
@Composable
fun CalendarHeatmap(
    values: List<Double>,
    firstDayOffset: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    formatValue: (Double) -> String = { "%.0f".format(it) },
    dayLabel: (Int) -> String = { "Day $it" }
) {
    if (values.isEmpty()) return
    val maxValue = values.max().coerceAtLeast(1e-9)
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
    var selected by remember(values) { mutableStateOf<Int?>(null) }
    val haptics = rememberHaptics()
    val outline = MaterialTheme.colorScheme.outline

    Column(modifier = modifier.fillMaxWidth()) {
        ChartTooltipSlot(visible = selected != null) {
            selected?.let { day ->
                val amount = values[day]
                Md3ChartTooltip(
                    title = dayLabel(day + 1),
                    value = formatValue(amount),
                    supporting = if (amount > 0) "Spent this day" else "No spending",
                    accent = if (amount > 0) color else track
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        val totalCells = firstDayOffset + values.size
        val weeks = (totalCells + 6) / 7
        repeat(weeks) { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { weekday ->
                    val dayIndex = week * 7 + weekday - firstDayOffset
                    val valid = dayIndex in values.indices
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(3.dp)
                            .height(36.dp)
                            .then(
                                if (valid) {
                                    val intensity =
                                        (values[dayIndex] / maxValue).toFloat()
                                    val isSelected = selected == dayIndex
                                    val cellColor = if (values[dayIndex] > 0) {
                                        color.copy(alpha = 0.12f + 0.88f * intensity)
                                    } else {
                                        track
                                    }
                                    Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(cellColor)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    width = 2.dp,
                                                    color = outline,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable {
                                            haptics.tick()
                                            selected = if (selected == dayIndex) {
                                                null
                                            } else {
                                                dayIndex
                                            }
                                        }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (valid) {
                            val intensity = (values[dayIndex] / maxValue).toFloat()
                            Text(
                                text = (dayIndex + 1).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected == dayIndex) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Medium
                                },
                                color = if (values[dayIndex] > 0 && intensity > 0.55f) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

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
