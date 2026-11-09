package codes.tashif.paisa.ui.transaction

import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.platform.PaisaCalendar
import codes.tashif.paisa.ui.platform.PlatformBackHandler
import codes.tashif.paisa.platform.formatFixed
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import codes.tashif.paisa.ui.components.AccountPicker
import codes.tashif.paisa.ui.components.CategoryPicker
import codes.tashif.paisa.ui.components.CircleIconButton
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.debts.DebtsViewModel
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onBack: () -> Unit,
    viewModel: TransactionViewModel = koinViewModel(),
    debtsViewModel: DebtsViewModel = koinViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val selectedId by viewModel.selectedTransactionId.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val debtPeople by viewModel.debtPeople.collectAsState()
    val split by viewModel.selectedSplit.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currency ?: "₹"
    val haptics = rememberHaptics()

    PlatformBackHandler(onBack = onBack)

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
                formatFixed(current.amount, 2, machine = true)
            }
        )
    }

    val initialCal = remember(current.id, current.transactionDate) {
        parseToCalendar(current.transactionDate)
    }
    var year by remember(current.id) { mutableIntStateOf(initialCal.get(PaisaCalendar.YEAR)) }
    var month by remember(current.id) { mutableIntStateOf(initialCal.get(PaisaCalendar.MONTH)) }
    var day by remember(current.id) { mutableIntStateOf(initialCal.get(PaisaCalendar.DAY_OF_MONTH)) }
    var hour by remember(current.id) { mutableIntStateOf(initialCal.get(PaisaCalendar.HOUR_OF_DAY)) }
    var minute by remember(current.id) { mutableIntStateOf(initialCal.get(PaisaCalendar.MINUTE)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val isIncome = current.type == "income"

    val filteredCategories = categories.filter { it.type == current.type }
        .ifEmpty { categories }

    val displayDate = remember(year, month, day, hour, minute) {
        val cal = PaisaCalendar.getInstance().apply {
            setDate(year, month, day)
            set(PaisaCalendar.HOUR_OF_DAY, hour)
            set(PaisaCalendar.MINUTE, minute)
            set(PaisaCalendar.SECOND, 0)
        }
        formatDate(cal.timeInMillis, "EEE, d MMM yyyy · h:mm a")
    }

    val sourceLabel = when (current.source) {
        "sms" -> "SMS"
        "statement" -> "Statement"
        else -> "Manual"
    }

    // Build detail rows: only real data, SMS-only fields never for manual
    val detailRows = transactionDetailRows(current, displayDate, sourceLabel, currency)

    val originalIso = remember(year, month, day, hour, minute) {
        formatDate(
            PaisaCalendar.getInstance().apply {
                setDate(year, month, day)
                set(PaisaCalendar.HOUR_OF_DAY, hour)
                set(PaisaCalendar.MINUTE, minute)
                set(PaisaCalendar.SECOND, 0)
            }.timeInMillis,
            "yyyy-MM-dd'T'HH:mm:ss"
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
            TransactionHeroCard(
                categoryIcon = current.categoryIcon,
                categoryName = current.categoryName,
                categoryColor = current.categoryColor,
                displayTitle = title.trim().ifBlank {
                    current.merchantName?.takeIf { it.isNotBlank() }
                        ?: note.trim().ifBlank { current.categoryName }
                },
                isIncome = isIncome,
                sourceLabel = sourceLabel,
                currency = currency,
                amount = parsedAmount ?: current.amount
            )

            // --- DETAILS (read-only snapshot) ---
            TransactionDetailsCard(rows = detailRows)

            // --- SMS (only when source is SMS and body exists) ---
            if (isSms && !current.smsBody.isNullOrBlank()) {
                SmsMessageCard(
                    body = current.smsBody,
                    sender = current.smsSender,
                    expanded = smsExpanded,
                    onToggle = { smsExpanded = !smsExpanded }
                )
            }

            // --- SPLIT (who owes for this, in Debts) ---
            TransactionSplitSection(
                split = split,
                people = debtPeople,
                total = current.amount,
                isExpense = current.type == "expense",
                currency = currency,
                onOpenPerson = { personId ->
                    debtsViewModel.openDebtPersonFromTransaction(personId, current.id)
                },
                onSave = { viewModel.saveTransactionSplit(current.id, it) },
                onRemove = { viewModel.removeTransactionSplit(current.id) }
            )

            // --- EDIT FIELDS ---
            Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))
            Text(
                text = "Edit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            EditTextFields(
                isManual = isManual,
                currency = currency,
                amountText = amountText,
                onAmountChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                title = title,
                onTitleChange = { title = it },
                note = note,
                onNoteChange = { note = it },
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            )

            if (isManual) {
                Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))
                EditDateTimeRow(
                    year = year,
                    month = month,
                    day = day,
                    hour = hour,
                    minute = minute,
                    onPickDate = { showDatePicker = true },
                    onPickTime = { showTimePicker = true }
                )

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

            DetailToggles(
                merchantForMapping = merchantForMapping,
                fromSmsOrImport = isSms || isStatement,
                rememberMerchant = rememberMerchant,
                onRememberMerchantChange = { rememberMerchant = it }
            )

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
        DeleteTransactionDialog(
            isManual = isManual,
            onConfirm = {
                haptics.reject()
                showDeleteConfirm = false
                viewModel.deleteTransaction(current.id)
                onBack()
            },
            onDismiss = { showDeleteConfirm = false }
        )
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
