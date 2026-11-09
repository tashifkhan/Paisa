package codes.tashif.paisa.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.components.ChoiceCards
import codes.tashif.paisa.ui.components.ChoiceOption
import codes.tashif.paisa.ui.components.SheetFieldCard
import codes.tashif.paisa.ui.theme.spacing

/** Expense or income, as two choice cards. */
@Composable
internal fun TransactionTypeChoice(
    type: String,
    onTypeChange: (String) -> Unit
) {
    ChoiceCards(
        options = listOf(
            ChoiceOption(
                id = "expense",
                label = "Expense",
                icon = Icons.AutoMirrored.Rounded.TrendingDown
            ),
            ChoiceOption(
                id = "income",
                label = "Income",
                icon = Icons.AutoMirrored.Rounded.TrendingUp
            )
        ),
        selectedId = type,
        onSelect = onTypeChange
    )
}

/** Title (stored as the merchant) and the free-text note. */
@Composable
internal fun TitleAndNoteFields(
    title: String,
    onTitleChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    onTitleNext: () -> Unit,
    onNoteDone: () -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )

    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text("Title") },
        placeholder = { Text("Merchant or description") },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        colors = fieldColors,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onTitleNext() }),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(MaterialTheme.spacing.smaller))

    OutlinedTextField(
        value = note,
        onValueChange = onNoteChange,
        label = { Text("Note") },
        placeholder = { Text("Optional") },
        minLines = 1,
        maxLines = 3,
        shape = MaterialTheme.shapes.extraLarge,
        colors = fieldColors,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onNoteDone() }),
        modifier = Modifier.fillMaxWidth()
    )
}

/** The big amount input at the top of the add sheet. Only digits and '.' get through. */
@Composable
internal fun AmountHeroField(
    amountText: String,
    onAmountChange: (String) -> Unit,
    currency: String,
    onNext: () -> Unit
) {
    SheetFieldCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    onAmountChange(it.filter { ch -> ch.isDigit() || ch == '.' })
                },
                prefix = {
                    Text(
                        text = currency,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { onNext() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent
                ),
                placeholder = {
                    Text(
                        text = "0.00",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Date and time cards side by side in the add sheet. Tapping one opens its picker. */
@Composable
internal fun AddDateTimeRow(
    dateLabel: String,
    timeLabel: String,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
    ) {
        AddPickerCard(
            icon = Icons.Rounded.CalendarMonth,
            label = "Date",
            value = dateLabel,
            onClick = onPickDate,
            modifier = Modifier.weight(1f)
        )
        AddPickerCard(
            icon = Icons.Rounded.Schedule,
            label = "Time",
            value = timeLabel,
            onClick = onPickTime,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AddPickerCard(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SheetFieldCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.size(MaterialTheme.spacing.smaller))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}
