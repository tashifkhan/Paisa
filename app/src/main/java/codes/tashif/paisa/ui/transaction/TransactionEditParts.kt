package codes.tashif.paisa.ui.transaction

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
import androidx.compose.material.icons.rounded.Handshake
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
                    text = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(
                        Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                        }.time
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
                    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(
                        Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                        }.time
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


internal fun parseToCalendar(iso: String): Calendar {
    val cal = Calendar.getInstance()
    val parsers = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    )
    for (parser in parsers) {
        val date = runCatching { parser.parse(iso.take(19)) }.getOrNull()
            ?: runCatching { parser.parse(iso.take(10)) }.getOrNull()
        if (date != null) {
            cal.time = date
            return cal
        }
    }
    return cal
}

/**
 * "Always use" merchant rule (when there is a merchant) and the Debts link.
 * The Debts card never stays on; tapping it opens the person dialog.
 */
@Composable
internal fun DetailToggles(
    merchantForMapping: String,
    fromSmsOrImport: Boolean,
    rememberMerchant: Boolean,
    onRememberMerchantChange: (Boolean) -> Unit,
    isExpense: Boolean,
    onLinkPerson: () -> Unit
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

    // UPI to a friend, a bill you covered: put it on what they owe.
    Spacer(Modifier.height(MaterialTheme.spacing.small))
    SheetToggleCard(
        icon = Icons.Rounded.Handshake,
        title = personLinkTitle(isExpense),
        subtitle = personLinkSubtitle(isExpense),
        checked = false,
        onCheckedChange = { onLinkPerson() }
    )
}
