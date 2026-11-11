package codes.tashif.paisa.ui.transaction

import codes.tashif.paisa.platform.parseDate
import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.platform.PaisaCalendar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.components.SheetFieldCard
import codes.tashif.paisa.ui.components.SheetToggleCard
import codes.tashif.paisa.ui.theme.spacing

/** Date and time cards for editing a manual transaction. [month] is zero-based. */
@Composable
internal fun EditDateTimeRow(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit
) {
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
) {
    SheetFieldCard(
        modifier = Modifier.weight(1f),
        onClick = onPickDate
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(MaterialTheme.spacing.smaller))
            Column {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDate(
                        PaisaCalendar.getInstance().apply {
                            setDate(year, month, day)
                        }.timeInMillis,
                        "d MMM yyyy"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
    SheetFieldCard(
        modifier = Modifier.weight(1f),
        onClick = onPickTime
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(MaterialTheme.spacing.smaller))
            Column {
                Text(
                    text = "Time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDate(
                        PaisaCalendar.getInstance().apply {
                            set(PaisaCalendar.HOUR_OF_DAY, hour)
                            set(PaisaCalendar.MINUTE, minute)
                        }.timeInMillis,
                        "h:mm a"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
}

/** Confirms deleting the transaction. Non-manual entries also mention SMS rescans. */
@Composable
internal fun DeleteTransactionDialog(
    isManual: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete transaction?") },
        text = {
            Text(
                if (isManual) {
                    "This removes it from history and reverts its effect on the account balance."
                } else {
                    "This removes it from history and reverts its effect on the account balance. " +
                        "SMS rescans won’t re-import it."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/** Amount (manual entries only), title and note fields on the detail screen. */
@Composable
internal fun EditTextFields(
    isManual: Boolean,
    currency: String,
    amountText: String,
    onAmountChange: (String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit
) {
    if (isManual) {
        OutlinedTextField(
            value = amountText,
            onValueChange = onAmountChange,
            label = { Text("Amount") },
            prefix = { Text(currency) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
            modifier = Modifier.fillMaxWidth()
        )
    }

    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text("Title") },
        placeholder = { Text("Merchant or description") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = note,
        onValueChange = onNoteChange,
        label = { Text("Note") },
        placeholder = { Text("Add a note…") },
        minLines = 2,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth()
    )
}


internal fun parseToCalendar(iso: String): PaisaCalendar {
    val cal = PaisaCalendar.getInstance()
    val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd")
    for (pattern in patterns) {
        val date = parseDate(iso.take(19), pattern) ?: parseDate(iso.take(10), pattern)
        if (date != null) {
            cal.timeInMillis = date
            return cal
        }
    }
    return cal
}

/** "Always use" merchant rule, shown when there is a merchant. */
@Composable
internal fun DetailToggles(
    merchantForMapping: String,
    fromSmsOrImport: Boolean,
    rememberMerchant: Boolean,
    onRememberMerchantChange: (Boolean) -> Unit
) {
    if (merchantForMapping.isNotEmpty()) {
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        SheetToggleCard(
            icon = Icons.Rounded.Bookmark,
            title = "Always use for “$merchantForMapping”",
            subtitle = if (fromSmsOrImport) {
                "Future SMS and imports with this merchant get this category"
            } else {
                "Future transactions with this title get this category"
            },
            checked = rememberMerchant,
            onCheckedChange = onRememberMerchantChange
        )
    }

}
