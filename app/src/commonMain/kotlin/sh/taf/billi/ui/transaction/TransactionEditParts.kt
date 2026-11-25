package sh.taf.billi.ui.transaction

import sh.taf.billi.ui.sms.smsImportSupported

import sh.taf.billi.platform.parseDate
import sh.taf.billi.platform.formatDate
import sh.taf.billi.platform.BilliCalendar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
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
import sh.taf.billi.ui.components.SheetFieldCard
import sh.taf.billi.ui.components.SheetToggleCard
import sh.taf.billi.ui.theme.spacing

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
                        BilliCalendar.getInstance().apply {
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
                        BilliCalendar.getInstance().apply {
                            set(BilliCalendar.HOUR_OF_DAY, hour)
                            set(BilliCalendar.MINUTE, minute)
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
                        "Rescans and reposts won’t re-import it."
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

/** Amount (manual entries only) and title fields on the detail screen. The note has its own Markdown card. */
@Composable
internal fun EditTextFields(
    isManual: Boolean,
    currency: String,
    amountText: String,
    onAmountChange: (String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
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
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth()
    )
}


internal fun parseToCalendar(iso: String): BilliCalendar {
    val cal = BilliCalendar.getInstance()
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

/**
 * Two rules from one edit, at the bottom of the page. Rename turns the old
 * title into the new one ("SwiggyLTD" → "Swiggy"); category always files
 * that title under the picked category. Rename only shows once the title
 * actually changed.
 */
@Composable
internal fun MerchantRuleToggles(
    merchant: String,
    renameFrom: String?,
    categoryName: String?,
    renameChecked: Boolean,
    onRenameChange: (Boolean) -> Unit,
    categoryChecked: Boolean,
    onCategoryChange: (Boolean) -> Unit
) {
    if (merchant.isEmpty() || (renameFrom == null && categoryName == null)) return
    val future = if (smsImportSupported) "SMS, notifications and imports" else "imports"
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        Text(
            text = "Rules",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = MaterialTheme.spacing.small, top = MaterialTheme.spacing.small)
        )
        if (renameFrom != null) {
            SheetToggleCard(
                icon = Icons.Rounded.DriveFileRenameOutline,
                title = "Rename “$renameFrom” to “$merchant”",
                subtitle = "Every past transaction, and new ones from $future",
                checked = renameChecked,
                onCheckedChange = onRenameChange
            )
        }
        if (categoryName != null) {
            SheetToggleCard(
                icon = Icons.Rounded.Bookmark,
                title = "Always put “$merchant” in $categoryName",
                subtitle = "Past and future transactions with this title",
                checked = categoryChecked,
                onCheckedChange = onCategoryChange
            )
        }
    }
}
