package sh.taf.billi.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CalendarViewMonth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.ui.draw.clip
import sh.taf.billi.ui.components.ConnectedToggleGroup
import sh.taf.billi.ui.components.FilterSectionCard
import sh.taf.billi.ui.components.ToggleOption

import sh.taf.billi.platform.BilliCalendar
import sh.taf.billi.platform.nowMillis
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import sh.taf.billi.ui.haptics.rememberHaptics
import sh.taf.billi.ui.theme.spacing
import sh.taf.billi.platform.formatDate
import sh.taf.billi.ui.transaction.setCleared

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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ConnectedToggleGroup(
            options = listOf(
                ToggleOption(AnalyticsPeriod.MONTH, "Month", Icons.Rounded.CalendarViewMonth),
                ToggleOption(AnalyticsPeriod.YEAR, "Year", Icons.Rounded.CalendarToday),
                ToggleOption(AnalyticsPeriod.CUSTOM, "Custom", Icons.Rounded.DateRange)
            ),
            isChecked = { it == kind },
            onClick = onKind
        )
        if (kind == AnalyticsPeriod.CUSTOM) {
            CustomRangeControls(custom = custom, onChange = onCustom)
        } else {
            // Step back and forward a month or a year at a time.
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = {
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
                FilledTonalIconButton(
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
}

/** Local millis to the UTC midnight of the same calendar day, which is what the date picker uses. */
private fun toPickerDay(localMillis: Long): Long {
    val local = BilliCalendar.getInstance().apply { timeInMillis = localMillis }
    return BilliCalendar.getUtcInstance().apply {
        setCleared(local.get(BilliCalendar.YEAR), local.get(BilliCalendar.MONTH), local.get(BilliCalendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** A picked day plus a time of day, in local millis. */
private fun fromPickerDay(utcDay: Long, hour: Int, minute: Int): Long {
    val day = BilliCalendar.getUtcInstance().apply { timeInMillis = utcDay }
    return BilliCalendar.getInstance().apply {
        setCleared(day.get(BilliCalendar.YEAR), day.get(BilliCalendar.MONTH), day.get(BilliCalendar.DAY_OF_MONTH), hour, minute)
    }.timeInMillis
}

private fun withTime(localMillis: Long, hour: Int, minute: Int): Long =
    BilliCalendar.getInstance().apply {
        timeInMillis = localMillis
        set(BilliCalendar.HOUR_OF_DAY, hour)
        set(BilliCalendar.MINUTE, minute)
        set(BilliCalendar.SECOND, 0)
        set(BilliCalendar.MILLISECOND, 0)
    }.timeInMillis

private fun hourMinute(localMillis: Long): Pair<Int, Int> =
    BilliCalendar.getInstance().apply { timeInMillis = localMillis }.let {
        it.get(BilliCalendar.HOUR_OF_DAY) to it.get(BilliCalendar.MINUTE)
    }

/** Whole days ending today: today, 7, 30 or 90 days. */
private fun lastDays(days: Int): Pair<Long, Long> {
    val end = withTime(nowMillis(), 23, 59)
    val start = BilliCalendar.getInstance().apply {
        timeInMillis = withTime(nowMillis(), 0, 0)
        add(BilliCalendar.DAY_OF_MONTH, -(days - 1))
    }.timeInMillis
    return start to end
}

/** One end of the range: the day opens the calendar, the time chip the clock. */
@Composable
private fun RangeEndRow(caption: String, day: String, time: String, top: Boolean, onDay: () -> Unit, onTime: () -> Unit) {
    val big = 20.dp
    val small = 6.dp
    val shape = if (top) RoundedCornerShape(big, big, small, small) else RoundedCornerShape(small, small, big, big)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onDay)
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(caption, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(day, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        FilledTonalButton(onClick = onTime, contentPadding = PaddingValues(horizontal = 14.dp)) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(time)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeControls(custom: Pair<Long, Long>, onChange: (Long, Long) -> Unit) {
    val (start, end) = custom
    var showDates by remember { mutableStateOf(false) }
    var editingTime by remember { mutableStateOf<Boolean?>(null) } // true = start, false = end

    val presets = listOf(1, 7, 30, 90)
    FilterSectionCard(
        title = "Range",
        summary = "${formatDate(start, "d MMM")} to ${formatDate(end, "d MMM yyyy")}",
        onClear = null
    ) {
        ConnectedToggleGroup(
            options = presets.map { ToggleOption(it, if (it == 1) "Today" else "${it}d") },
            isChecked = { custom == lastDays(it) },
            onClick = { days ->
                val range = lastDays(days)
                onChange(range.first, range.second)
            }
        )
        // Start and end as one grouped list: tap the day for the calendar, the time to change it.
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            RangeEndRow(
                caption = "Starts",
                day = formatDate(start, "EEE, d MMM yyyy"),
                time = formatDate(start, "h:mm a"),
                top = true,
                onDay = { showDates = true },
                onTime = { editingTime = true }
            )
            RangeEndRow(
                caption = "Ends",
                day = formatDate(end, "EEE, d MMM yyyy"),
                time = formatDate(end, "h:mm a"),
                top = false,
                onDay = { showDates = true },
                onTime = { editingTime = false }
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
