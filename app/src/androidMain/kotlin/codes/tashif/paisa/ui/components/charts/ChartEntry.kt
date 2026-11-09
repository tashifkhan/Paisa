package codes.tashif.paisa.ui.components.charts

import androidx.compose.ui.graphics.Color

/** One slice/bar of chart data. */
data class ChartEntry(
    val label: String,
    val value: Double,
    val color: Color
)
