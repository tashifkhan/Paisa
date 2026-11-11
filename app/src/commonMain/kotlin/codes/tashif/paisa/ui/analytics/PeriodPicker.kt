package codes.tashif.paisa.ui.analytics

import codes.tashif.paisa.ui.components.PillTabs

import codes.tashif.paisa.platform.PaisaCalendar
import codes.tashif.paisa.platform.nowMillis
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing
import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.ui.transaction.setCleared

@Composable
internal fun PeriodPicker(
    kind: AnalyticsPeriod,
    label: String,
    canGoForward: Boolean,
    custom: Pair<Long, Long>,
    onKind: (AnalyticsPeriod) -> Unit,
    onShift: (Int) -> Unit,
    onCustom: (Long, Long) -> Unit
) {
    val haptics = rememberHaptics()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PillTabs(
            options = listOf(
                "Month" to AnalyticsPeriod.MONTH,
                "Year" to AnalyticsPeriod.YEAR,
                "Custom" to AnalyticsPeriod.CUSTOM
            ),
            selected = kind,
            onSelect = onKind,
            height = 52.dp
        )
        if (kind == AnalyticsPeriod.CUSTOM) {
            CustomRangeControls(custom = custom, onChange = onCustom)
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                haptics.tick()
                onShift(1)
            }) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous")
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = {
                    haptics.tick()
                    onShift(-1)
                },
                enabled = canGoForward
            ) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next")
            }
        }
    }
}

/** Local millis to the UTC midnight of the same calendar day, which is what the date picker uses. */
private fun toPickerDay(localMillis: Long): Long {
    val local = PaisaCalendar.getInstance().apply { timeInMillis = localMillis }
    return PaisaCalendar.getUtcInstance().apply {
        setCleared(local.get(PaisaCalendar.YEAR), local.get(PaisaCalendar.MONTH), local.get(PaisaCalendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** A picked day plus a time of day, in local millis. */
private fun fromPickerDay(utcDay: Long, hour: Int, minute: Int): Long {
    val day = PaisaCalendar.getUtcInstance().apply { timeInMillis = utcDay }
    return PaisaCalendar.getInstance().apply {
        setCleared(day.get(PaisaCalendar.YEAR), day.get(PaisaCalendar.MONTH), day.get(PaisaCalendar.DAY_OF_MONTH), hour, minute)
    }.timeInMillis
}

private fun withTime(localMillis: Long, hour: Int, minute: Int): Long =
    PaisaCalendar.getInstance().apply {
        timeInMillis = localMillis
        set(PaisaCalendar.HOUR_OF_DAY, hour)
        set(PaisaCalendar.MINUTE, minute)
        set(PaisaCalendar.SECOND, 0)
        set(PaisaCalendar.MILLISECOND, 0)
    }.timeInMillis

private fun hourMinute(localMillis: Long): Pair<Int, Int> =
    PaisaCalendar.getInstance().apply { timeInMillis = localMillis }.let {
        it.get(PaisaCalendar.HOUR_OF_DAY) to it.get(PaisaCalendar.MINUTE)
    }

/** Whole days ending today: today, 7, 30 or 90 days. */
private fun lastDays(days: Int): Pair<Long, Long> {
    val end = withTime(nowMillis(), 23, 59)
    val start = PaisaCalendar.getInstance().apply {
        timeInMillis = withTime(nowMillis(), 0, 0)
        add(PaisaCalendar.DAY_OF_MONTH, -(days - 1))
    }.timeInMillis
    return start to end
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CustomRangeControls(custom: Pair<Long, Long>, onChange: (Long, Long) -> Unit) {
    val (start, end) = custom
    var showDates by remember { mutableStateOf(false) }
    var editingTime by remember { mutableStateOf<Boolean?>(null) } // true = start, false = end

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
            listOf("Today" to 1, "7 days" to 7, "30 days" to 30, "90 days" to 90).forEach { (text, days) ->
                val range = lastDays(days)
                FilterChip(
                    selected = custom == range,
                    onClick = { onChange(range.first, range.second) },
                    label = { Text(text) }
                )
            }
        }
        AssistChip(
            onClick = { showDates = true },
            label = {
                Text(
                    text = "${formatDate(start, "d MMM yyyy")}  →  ${formatDate(end, "d MMM yyyy")}",
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            },
            leadingIcon = { Icon(Icons.Rounded.DateRange, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
            AssistChip(
                onClick = { editingTime = true },
                label = { Text("From ${formatDate(start, "h:mm a")}") },
                leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null) },
                modifier = Modifier.weight(1f)
            )
            AssistChip(
                onClick = { editingTime = false },
                label = { Text("To ${formatDate(end, "h:mm a")}") },
                leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showDates) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = toPickerDay(start),
            initialSelectedEndDateMillis = toPickerDay(end)
        )
        DatePickerDialog(
            onDismissRequest = { showDates = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val from = state.selectedStartDateMillis
                        if (from != null) {
                            val to = state.selectedEndDateMillis ?: from
                            val (sh, sm) = hourMinute(start)
                            val (eh, em) = hourMinute(end)
                            onChange(fromPickerDay(from, sh, sm), fromPickerDay(to, eh, em))
                        }
                        showDates = false
                    },
                    enabled = state.selectedStartDateMillis != null
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDates = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state = state,
                title = {
                    Text(
                        "Pick a date range",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    editingTime?.let { isStart ->
        val (h, m) = hourMinute(if (isStart) start else end)
        val state = rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = false)
        AlertDialog(
            onDismissRequest = { editingTime = null },
            title = { Text(if (isStart) "Start time" else "End time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    if (isStart) {
                        onChange(withTime(start, state.hour, state.minute), end)
                    } else {
                        onChange(start, withTime(end, state.hour, state.minute))
                    }
                    editingTime = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { editingTime = null }) { Text("Cancel") }
            }
        )
    }
}
