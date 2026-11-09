package codes.tashif.paisa.ui.debts

import codes.tashif.paisa.ui.transaction.setDate
import codes.tashif.paisa.ui.transaction.setCleared
import codes.tashif.paisa.platform.nowMillis
import codes.tashif.paisa.platform.parseDate
import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.platform.PaisaCalendar
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import codes.tashif.paisa.ui.theme.spacing

private const val ISO = "yyyy-MM-dd'T'HH:mm:ss"

/** Local time now, in the format debt entries store. */
internal fun nowEntryDate(): String = formatDate(nowMillis(), ISO, machine = true)

/** Shows the day of [iso] and lets you pick another. The time of day is kept. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DebtDateButton(iso: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var picking by remember { mutableStateOf(false) }
    val local = remember(iso) {
        PaisaCalendar.getInstance().apply {
            parseDate(iso.take(19), ISO, machine = true)?.let { timeInMillis = it }
        }
    }
    OutlinedButton(onClick = { picking = true }, modifier = modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
        Spacer(Modifier.width(MaterialTheme.spacing.smaller))
        Text(formatDate(local.timeInMillis, "EEE, d MMM yyyy"))
    }
    if (!picking) return
    // The picker works in UTC midnights.
    val utc = PaisaCalendar.getUtcInstance().apply {
        setCleared(local.get(PaisaCalendar.YEAR), local.get(PaisaCalendar.MONTH), local.get(PaisaCalendar.DAY_OF_MONTH))
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = utc.timeInMillis)
    DatePickerDialog(
        onDismissRequest = { picking = false },
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    val picked = PaisaCalendar.getUtcInstance().apply { timeInMillis = millis }
                    val out = (local.clone()).apply {
                        setDate(picked.get(PaisaCalendar.YEAR), picked.get(PaisaCalendar.MONTH), picked.get(PaisaCalendar.DAY_OF_MONTH))
                    }
                    onChange(formatDate(out.timeInMillis, ISO, machine = true))
                }
                picking = false
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}
