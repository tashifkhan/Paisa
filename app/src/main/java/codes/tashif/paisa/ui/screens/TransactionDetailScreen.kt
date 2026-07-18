package codes.tashif.paisa.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.PaisaViewModel
import codes.tashif.paisa.ui.components.AccountPicker
import codes.tashif.paisa.ui.components.CategoryBadge
import codes.tashif.paisa.ui.components.CategoryPicker
import codes.tashif.paisa.ui.components.CircleIconButton
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.components.SheetFieldCard
import codes.tashif.paisa.ui.components.SheetToggleCard
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen
import codes.tashif.paisa.ui.theme.spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    viewModel: PaisaViewModel,
    onBack: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val selectedId by viewModel.selectedTransactionId.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currency ?: "₹"
    val haptics = rememberHaptics()

    BackHandler(onBack = onBack)

    val current = transactions.firstOrNull { it.id == selectedId }
    // Only bail out once data has loaded and the id is genuinely gone
    // (e.g. deleted elsewhere) — not while the first emission is pending.
    LaunchedEffect(current, transactions) {
        if (current == null && transactions.isNotEmpty()) onBack()
    }
    if (current == null) {
        return
    }

    val isManual = current.source == "manual"
    val isSms = current.source == "sms"
    val isStatement = current.source == "statement"

    var selectedCategoryId by remember(current.id, current.categoryId) {
        mutableIntStateOf(current.categoryId)
    }
    var selectedAccountId by remember(current.id, current.accountId) {
        mutableIntStateOf(current.accountId)
    }
    var rememberMerchant by remember(current.id) { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var smsExpanded by remember(current.id) { mutableStateOf(false) }
    var title by remember(current.id) { mutableStateOf(current.merchantName.orEmpty()) }
    var note by remember(current.id) { mutableStateOf(current.note) }
    var amountText by remember(current.id) {
        mutableStateOf(
            if (current.amount % 1.0 == 0.0) {
                current.amount.toInt().toString()
            } else {
                String.format(Locale.US, "%.2f", current.amount)
            }
        )
    }

    val initialCal = remember(current.id, current.transactionDate) {
        parseToCalendar(current.transactionDate)
    }
    var year by remember(current.id) { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var month by remember(current.id) { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }
    var day by remember(current.id) { mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember(current.id) { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember(current.id) { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val isIncome = current.type == "income"
    val accentColor = if (isIncome) IncomeGreen else ExpenseRed

    val filteredCategories = categories.filter { it.type == current.type }
        .ifEmpty { categories }

    val displayDate = remember(year, month, day, hour, minute) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        SimpleDateFormat("EEE, d MMM yyyy · h:mm a", Locale.getDefault()).format(cal.time)
    }

    val sourceLabel = when (current.source) {
        "sms" -> "SMS"
        "statement" -> "Statement"
        else -> "Manual"
    }

    // Build detail rows: only real data, SMS-only fields never for manual
    data class RowSpec(val icon: ImageVector, val label: String, val value: String)
    val detailRows = buildList {
        add(RowSpec(Icons.Rounded.CalendarMonth, "Date & Time", displayDate))
        add(RowSpec(Icons.Rounded.Info, "Source", sourceLabel))
        add(RowSpec(Icons.Rounded.Category, "Category", current.categoryName))
        if (!isManual) {
            current.bankName?.takeIf { it.isNotBlank() }?.let {
                add(RowSpec(Icons.Rounded.AccountBalance, "Bank", it))
            }
        }
        val accountLabel = listOfNotNull(
            current.accountName.takeIf { it.isNotBlank() },
            current.accountNumber
                ?.takeIf { !isManual && it.isNotBlank() }
                ?.let { "••$it" }
        ).joinToString(" ")
        if (accountLabel.isNotBlank()) {
            add(RowSpec(Icons.Rounded.AccountBalanceWallet, "Account", accountLabel))
        }
        if (!isManual) {
            current.balanceAfter?.let {
                add(RowSpec(Icons.Rounded.Savings, "Balance after", formatMoney(currency, it)))
            }
            current.reference?.takeIf { it.isNotBlank() }?.let {
                add(RowSpec(Icons.Rounded.Tag, "Reference", it))
            }
        }
    }

    val originalIso = remember(year, month, day, hour, minute) {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(
            Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }.time
        )
    }
    val parsedAmount = amountText.toDoubleOrNull()
    val storedDateKey = current.transactionDate.take(16) // yyyy-MM-dd'T'HH:mm
    val editedDateKey = originalIso.take(16)
    val detailsChanged = title.trim() != current.merchantName.orEmpty() ||
        note.trim() != current.note ||
        (isManual && parsedAmount != null && kotlin.math.abs(parsedAmount - current.amount) > 0.0001) ||
        (isManual && editedDateKey != storedDateKey) ||
        (isManual && selectedAccountId != current.accountId)
    val categoryChanged = selectedCategoryId != current.categoryId
    val merchantForMapping = title.trim().ifBlank { current.merchantName?.trim().orEmpty() }
    val canSave = detailsChanged || categoryChanged ||
        (rememberMerchant && merchantForMapping.isNotEmpty())

    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(
            title = "Transaction",
            onBack = onBack,
            actions = {
                CircleIconButton(
                    icon = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    onClick = {
                        haptics.click()
                        showDeleteConfirm = true
                    }
                )
            }
        )

        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            // --- HERO ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CategoryBadge(
                        iconName = current.categoryIcon,
                        categoryName = current.categoryName,
                        colorHex = current.categoryColor,
                        size = 64.dp
                    )
                    Spacer(Modifier.height(MaterialTheme.spacing.small))
                    Text(
                        text = title.trim().ifBlank {
                            current.merchantName?.takeIf { it.isNotBlank() }
                                ?: note.trim().ifBlank { current.categoryName }
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isIncome) "Income" else "Expense",
                                style = MaterialTheme.typography.labelLarge,
                                color = accentColor,
                                modifier = Modifier.padding(
                                    horizontal = MaterialTheme.spacing.small,
                                    vertical = MaterialTheme.spacing.extraSmall
                                )
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = sourceLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(
                                    horizontal = MaterialTheme.spacing.small,
                                    vertical = MaterialTheme.spacing.extraSmall
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(MaterialTheme.spacing.small))
                    Text(
                        text = (if (isIncome) "+" else "−") + formatMoney(
                            currency,
                            parsedAmount ?: current.amount
                        ),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            // --- DETAILS (read-only snapshot) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(vertical = MaterialTheme.spacing.extraSmall)) {
                    detailRows.forEachIndexed { index, row ->
                        DetailRow(
                            icon = row.icon,
                            label = row.label,
                            value = row.value,
                            showDivider = index < detailRows.lastIndex
                        )
                    }
                }
            }

            // --- SMS (only when source is SMS and body exists) ---
            if (isSms && !current.smsBody.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.toggle(on = !smsExpanded)
                                smsExpanded = !smsExpanded
                            }
                            .padding(MaterialTheme.spacing.medium)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Message,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(MaterialTheme.spacing.small))
                            Text(
                                text = if (smsExpanded) "Hide SMS" else "Show SMS",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (smsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (smsExpanded) {
                            Spacer(Modifier.height(MaterialTheme.spacing.small))
                            Text(
                                text = current.smsBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            current.smsSender?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))
                                Text(
                                    text = "From $it",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            // --- EDIT FIELDS ---
            Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))
            Text(
                text = "Edit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (isManual) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount") },
                    prefix = { Text(currency) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("Merchant or description") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note") },
                placeholder = { Text("Add a note…") },
                minLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (isManual) {
                Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
                ) {
                    SheetFieldCard(
                        modifier = Modifier.weight(1f),
                        onClick = { showDatePicker = true }
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
                        onClick = { showTimePicker = true }
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

                Spacer(Modifier.height(MaterialTheme.spacing.smaller))
                AccountPicker(
                    accounts = accounts,
                    selectedId = selectedAccountId,
                    onSelect = { selectedAccountId = it }
                )
            }

            Spacer(Modifier.height(MaterialTheme.spacing.smaller))
            CategoryPicker(
                categories = filteredCategories,
                selectedId = selectedCategoryId,
                onSelect = { selectedCategoryId = it }
            )

            if (merchantForMapping.isNotEmpty()) {
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                SheetToggleCard(
                    icon = Icons.Rounded.Bookmark,
                    title = "Always use for “$merchantForMapping”",
                    subtitle = if (isSms || isStatement) {
                        "Future SMS and imports with this merchant get this category"
                    } else {
                        "Future transactions with this title get this category"
                    },
                    checked = rememberMerchant,
                    onCheckedChange = { rememberMerchant = it }
                )
            }

            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            Button(
                onClick = {
                    if (isManual && (parsedAmount == null || parsedAmount <= 0.0)) {
                        haptics.reject()
                        viewModel.showSnackbar("Enter a valid amount")
                        return@Button
                    }
                    haptics.confirm()
                    viewModel.saveTransactionEdits(
                        transactionId = current.id,
                        title = title,
                        note = note,
                        categoryId = selectedCategoryId,
                        rememberMerchant = rememberMerchant && merchantForMapping.isNotEmpty(),
                        amount = if (isManual) parsedAmount else null,
                        transactionDate = if (isManual) originalIso else null,
                        accountId = if (isManual) selectedAccountId else null
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave
            ) {
                Text("Save changes")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
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
                TextButton(onClick = {
                    haptics.reject()
                    showDeleteConfirm = false
                    viewModel.deleteTransaction(current.id)
                    onBack()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDatePicker) {
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
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = millis
                        }
                        year = picked.get(Calendar.YEAR)
                        month = picked.get(Calendar.MONTH)
                        day = picked.get(Calendar.DAY_OF_MONTH)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = timePickerState.hour
                    minute = timePickerState.minute
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Select time") },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(MaterialTheme.spacing.small))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}

private fun parseToCalendar(iso: String): Calendar {
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

