package codes.tashif.paisa.ui.debts

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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private const val ISO = "yyyy-MM-dd'T'HH:mm:ss"

/** Local time now, in the format debt entries store. */
internal fun nowEntryDate(): String = SimpleDateFormat(ISO, Locale.US).format(Calendar.getInstance().time)

/** Shows the day of [iso] and lets you pick another. The time of day is kept. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DebtDateButton(iso: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var picking by remember { mutableStateOf(false) }
    val local = remember(iso) {
        Calendar.getInstance().apply {
            runCatching { SimpleDateFormat(ISO, Locale.US).parse(iso.take(19)) }.getOrNull()?.let { time = it }
        }
    }
    OutlinedButton(onClick = { picking = true }, modifier = modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
        Spacer(Modifier.width(MaterialTheme.spacing.smaller))
        Text(SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(local.time))
    }
    if (!picking) return
    // The picker works in UTC midnights.
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = utc.timeInMillis)
    DatePickerDialog(
        onDismissRequest = { picking = false },
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
                    val out = (local.clone() as Calendar).apply {
                        set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH))
                    }
                    onChange(SimpleDateFormat(ISO, Locale.US).format(out.time))
                }
                picking = false
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}
