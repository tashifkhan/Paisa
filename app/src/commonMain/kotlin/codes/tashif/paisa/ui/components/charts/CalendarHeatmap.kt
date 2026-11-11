package codes.tashif.paisa.ui.components.charts

import codes.tashif.paisa.platform.formatFixed
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.haptics.rememberHaptics

/**
 * Month calendar heatmap with M3 cells and rich tooltip on tap.
 */
@Composable
fun CalendarHeatmap(
    values: List<Double>,
    firstDayOffset: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    formatValue: (Double) -> String = { formatFixed(it, 0) },
    dayLabel: (Int) -> String = { "Day $it" }
) {
    if (values.isEmpty()) return
    val maxValue = values.max().coerceAtLeast(1e-9)
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
    var selected by remember(values) { mutableStateOf<Int?>(null) }
    val haptics = rememberHaptics()
    val outline = MaterialTheme.colorScheme.outline

    var gridWidth by remember { mutableStateOf(0) }
    var headerHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current.density
    val anchor = selected?.let { day ->
        if (gridWidth == 0) return@let null
        val cell = day + firstDayOffset
        val cellWidth = gridWidth / 7f
        // Each week row is a 36dp cell with 3dp padding above and below.
        val y = headerHeight + 4f * density + (cell / 7) * 42f * density + 3f * density
        Offset((cell % 7 + 0.5f) * cellWidth, y)
    }
    val tooltip: (@Composable () -> Unit)? = selected?.let { day ->
        {
            val amount = values[day]
            ChartTooltipCard(
                title = dayLabel(day + 1),
                lines = listOf(TooltipLine(if (amount > 0) color else track, null, formatValue(amount))),
                note = if (amount > 0) null else "No spending"
            )
        }
    }

    ChartWithTooltip(anchor = anchor, tooltip = tooltip, modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.fillMaxWidth().onSizeChanged { gridWidth = it.width }) {
        Row(modifier = Modifier.fillMaxWidth().onSizeChanged { headerHeight = it.height }) {
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
}
