package codes.tashif.paisa.ui.transaction

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import java.util.Calendar
import java.util.TimeZone

/**
 * Date picker for a transaction. [month] is zero-based like [Calendar.MONTH].
 * [onPicked] gets the chosen year, month and day; [onDismiss] closes the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionDatePickerDialog(
    year: Int,
    month: Int,
    day: Int,
    onDismiss: () -> Unit,
    onPicked: (year: Int, month: Int, day: Int) -> Unit
) {
    val initialUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = millis
                    }
                    onPicked(
                        picked.get(Calendar.YEAR),
                        picked.get(Calendar.MONTH),
                        picked.get(Calendar.DAY_OF_MONTH)
                    )
                }
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

/** 12-hour time picker for a transaction. [onPicked] gets the chosen hour and minute. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionTimePickerDialog(
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onPicked: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onPicked(timePickerState.hour, timePickerState.minute)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Select time") },
        text = { TimePicker(state = timePickerState) }
    )
}
