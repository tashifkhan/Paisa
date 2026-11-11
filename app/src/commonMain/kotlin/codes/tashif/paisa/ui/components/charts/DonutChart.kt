package codes.tashif.paisa.ui.components.charts

import codes.tashif.paisa.platform.radiansToDegrees
import codes.tashif.paisa.platform.formatFixed
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import codes.tashif.paisa.ui.platform.hoverIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.haptics.rememberHaptics
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * M3-style donut chart. Tap a slice for a rich tooltip; center shows total or selection.
 */
@Composable
fun DonutChart(
    entries: List<ChartEntry>,
    centerTitle: String,
    centerSubtitle: String,
    modifier: Modifier = Modifier,
    formatValue: (Double) -> String = { formatFixed(it, 0) },
    shareLabel: String = "of spending"
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
        val strokePx = with(LocalDensity.current) { 28.dp.toPx() }
        // Which slice is under a point, or null off the ring.
        val sliceAt: (Offset, IntSize) -> Int? = { point, size ->
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outer = size.width.coerceAtMost(size.height) / 2f
            val inner = outer - strokePx * 1.5f
            val dist = hypot(point.x - cx, point.y - cy)
            if (dist !in inner..outer + strokePx / 2f) {
                null
            } else {
                var angle = radiansToDegrees(
                    atan2((point.y - cy).toDouble(), (point.x - cx).toDouble())
                ) + 90.0
                if (angle < 0) angle += 360.0
                var cumulative = 0.0
                entries.indexOfFirst { entry ->
                    val sweep = entry.value / total * 360.0
                    val inSlice = angle >= cumulative && angle < cumulative + sweep
                    cumulative += sweep
                    inSlice
                }.takeIf { it >= 0 }
            }
        }

        Box(
            modifier = Modifier
                .size(200.dp)
                // A mouse shows the tooltip on hover; touch still taps.
                .hoverIndex(entries, { pos, size -> sliceAt(pos, size) }) { selected = it }
                .pointerInput(entries) {
                    detectTapGestures { tap ->
                        val hit = sliceAt(tap, size)
                        val next = if (hit == selected) null else hit
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
                    Text(
                        text = "${(pick.value / total * 100).toInt()}% $shareLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
