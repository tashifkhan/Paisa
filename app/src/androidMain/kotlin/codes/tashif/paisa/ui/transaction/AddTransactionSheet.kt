package codes.tashif.paisa.ui.transaction

import androidx.compose.foundation.layout.Column
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.components.AccountPicker
import codes.tashif.paisa.ui.components.CategoryPicker
import codes.tashif.paisa.ui.components.SheetToggleCard
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    onDismiss: () -> Unit,
    viewModel: TransactionViewModel = koinViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = rememberHaptics()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currency ?: "₹"

    var type by remember { mutableStateOf("expense") }
    var amountText by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var selectedAccountId by remember { mutableStateOf<Int?>(null) }
    var rememberMerchant by remember { mutableStateOf(false) }
    // "Split with others" for an expense, "Someone paid me back" for income
    val split = rememberAddSplitState()
    val people by viewModel.debtPeople.collectAsState()

    val now = remember { Calendar.getInstance() }
    var year by remember { mutableStateOf(now.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(now.get(Calendar.MONTH)) }
    var day by remember { mutableStateOf(now.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableStateOf(now.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(now.get(Calendar.MINUTE)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val filteredCategories = categories.filter { it.type == type }

    LaunchedEffect(type, filteredCategories) {
        if (selectedCategoryId == null || filteredCategories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = filteredCategories.firstOrNull()?.id
        }
    }
    LaunchedEffect(accounts) {
        if (selectedAccountId == null) {
            selectedAccountId = (accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull())?.id
        }
    }

    val dateLabel = remember(year, month, day) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
        }
        SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(cal.time)
    }
    val timeLabel = remember(hour, minute) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge)
        ) {
            Text(
                text = "New transaction",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Log a manual expense or income",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(MaterialTheme.spacing.medium))

            TransactionTypeChoice(
                type = type,
                onTypeChange = { type = it }
            )

            Spacer(Modifier.height(MaterialTheme.spacing.medium))

            // Amount hero
            AmountHeroField(
                amountText = amountText,
                onAmountChange = { amountText = it },
                currency = currency,
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )

            Spacer(Modifier.height(MaterialTheme.spacing.small))

            TitleAndNoteFields(
                title = title,
                onTitleChange = { title = it },
                note = note,
                onNoteChange = { note = it },
                onTitleNext = { focusManager.moveFocus(FocusDirection.Down) },
                onNoteDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            )

            Spacer(Modifier.height(MaterialTheme.spacing.small))

            AddDateTimeRow(
                dateLabel = dateLabel,
                timeLabel = timeLabel,
                onPickDate = { showDatePicker = true },
                onPickTime = { showTimePicker = true }
            )

            Spacer(Modifier.height(MaterialTheme.spacing.small))
            CategoryPicker(
                categories = filteredCategories,
                selectedId = selectedCategoryId,
                onSelect = { selectedCategoryId = it },
                emptyText = "No $type categories yet — add some in More → Categories"
            )

            Spacer(Modifier.height(MaterialTheme.spacing.smaller))
            AccountPicker(
                accounts = accounts,
                selectedId = selectedAccountId,
                onSelect = { selectedAccountId = it }
            )

            AddTransactionSplitSection(
                state = split,
                isExpense = type == "expense",
                people = people,
                amountText = amountText,
                currency = currency
            )

            if (title.isNotBlank()) {
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                SheetToggleCard(
                    icon = Icons.Rounded.Bookmark,
                    title = "Remember for “${title.trim()}”",
                    subtitle = "Reuse this category when the title matches",
                    checked = rememberMerchant,
                    onCheckedChange = { rememberMerchant = it }
                )
            }

            Spacer(Modifier.height(MaterialTheme.spacing.large))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    val categoryId = selectedCategoryId
                    val accountId = selectedAccountId
                    if (amount == null || amount <= 0.0 || categoryId == null || accountId == null) {
                        haptics.reject()
                        viewModel.showSnackbar("Enter a valid amount, category, and account")
                        return@Button
                    }
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        .format(cal.time)
                    val merchant = title.trim().takeIf { it.isNotBlank() }
                    val shares = split.shares(type == "expense", amount) ?: run {
                        haptics.reject()
                        viewModel.showSnackbar(split.problem(type == "expense", amount))
                        return@Button
                    }
                    haptics.confirm()
                    viewModel.addTransaction(
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        accountId = accountId,
                        note = note.trim(),
                        transactionDate = iso,
                        merchantName = merchant,
                        rememberMerchant = rememberMerchant && merchant != null,
                        debtShares = shares
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    text = "Save transaction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showDatePicker) {
        TransactionDatePickerDialog(
            year = year,
            month = month,
            day = day,
            onDismiss = { showDatePicker = false },
            onPicked = { y, m, d ->
                year = y
                month = m
                day = d
            }
        )
    }

    if (showTimePicker) {
        TransactionTimePickerDialog(
            hour = hour,
            minute = minute,
            onDismiss = { showTimePicker = false },
            onPicked = { h, m ->
                hour = h
                minute = m
            }
        )
    }
}
