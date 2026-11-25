package sh.taf.billi.ui.transaction

import sh.taf.billi.platform.formatDate
import sh.taf.billi.platform.BilliCalendar
import sh.taf.billi.ui.platform.PlatformBackHandler
import sh.taf.billi.platform.formatFixed
import org.koin.compose.viewmodel.koinViewModel
import sh.taf.billi.notifications.UNASSIGNED_ACCOUNT_TYPE
import sh.taf.billi.ui.notifications.AccountApprovalViewModel
import sh.taf.billi.ui.notifications.AwaitingAccountCard
import sh.taf.billi.ui.notifications.ChooseAccountSheet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import sh.taf.billi.ui.components.AccountPicker
import sh.taf.billi.ui.components.CategoryPicker
import sh.taf.billi.ui.components.CircleIconButton
import sh.taf.billi.ui.components.DetailHeader
import sh.taf.billi.ui.components.markdown.Markdown
import sh.taf.billi.ui.components.markdown.MarkdownField
import sh.taf.billi.ui.components.markdown.MarkdownText
import sh.taf.billi.ui.debts.DebtsViewModel
import sh.taf.billi.ui.haptics.rememberHaptics
import sh.taf.billi.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onBack: () -> Unit,
    viewModel: TransactionViewModel = koinViewModel(),
    debtsViewModel: DebtsViewModel = koinViewModel(),
    approvalViewModel: AccountApprovalViewModel = koinViewModel()
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
    val awaiting by approvalViewModel.awaiting.collectAsState()
    val accountChoices by approvalViewModel.choices.collectAsState()
    var choosingAccount by remember { mutableStateOf(false) }


    val current = transactions.firstOrNull { it.id == selectedId }
    // Only bail out once data has loaded and the id is genuinely gone
    // (e.g. deleted elsewhere) — not while the first emission is pending.
    LaunchedEffect(current, transactions) {
        if (current == null && transactions.isNotEmpty()) onBack()
    }
    if (current == null) {
        return
    }

    // Rows an assistant added are typed-in entries too, so they edit the same way.
    val isManual = current.source == "manual" || current.source == "assistant"
    // A notification keeps its text in smsBody too, so it shows and behaves like an SMS.
    val isSms = current.source == "sms" || current.source == "notification"
    // A UPI or bank app may not name the account, so its rows can move to another one.
    val accountEditable = isManual || current.source == "notification"

    var selectedCategoryId by remember(current.id, current.categoryId) {
        mutableIntStateOf(current.categoryId)
    }
    var selectedAccountId by remember(current.id, current.accountId) {
        mutableIntStateOf(current.accountId)
    }
    // Rules saved with the edit: the corrected name, and the category for that name.
    var saveRename by remember(current.id) { mutableStateOf(false) }
    var saveCategoryRule by remember(current.id) { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editing by remember(current.id) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var smsExpanded by remember(current.id) { mutableStateOf(false) }
    var title by remember(current.id) { mutableStateOf(current.merchantName.orEmpty()) }
    var note by remember(current.id) { mutableStateOf(current.note) }
    val initialAmountText = remember(current.id, current.amount) {
        if (current.amount % 1.0 == 0.0) {
            current.amount.toInt().toString()
        } else {
            formatFixed(current.amount, 2, machine = true)
        }
    }
    var amountText by remember(current.id) { mutableStateOf(initialAmountText) }

    val initialCal = remember(current.id, current.transactionDate) {
        parseToCalendar(current.transactionDate)
    }
    var year by remember(current.id) { mutableIntStateOf(initialCal.get(BilliCalendar.YEAR)) }
    var month by remember(current.id) { mutableIntStateOf(initialCal.get(BilliCalendar.MONTH)) }
    var day by remember(current.id) { mutableIntStateOf(initialCal.get(BilliCalendar.DAY_OF_MONTH)) }
    var hour by remember(current.id) { mutableIntStateOf(initialCal.get(BilliCalendar.HOUR_OF_DAY)) }
    var minute by remember(current.id) { mutableIntStateOf(initialCal.get(BilliCalendar.MINUTE)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val isIncome = current.type == "income"

    val filteredCategories = categories.filter { it.type == current.type }
        .ifEmpty { categories }

    val displayDate = remember(year, month, day, hour, minute) {
        val cal = BilliCalendar.getInstance().apply {
            setDate(year, month, day)
            set(BilliCalendar.HOUR_OF_DAY, hour)
            set(BilliCalendar.MINUTE, minute)
            set(BilliCalendar.SECOND, 0)
        }
        formatDate(cal.timeInMillis, "EEE, d MMM yyyy · h:mm a")
    }

    val sourceLabel = when (current.source) {
        "sms" -> "SMS"
        "notification" -> "Notification"
        "statement" -> "Statement"
        "assistant" -> "Assistant"
        else -> "Manual"
    }

    // Build detail rows: only real data, SMS-only fields never for manual
    val detailRows = transactionDetailRows(current, displayDate, sourceLabel, currency)

    val originalIso = remember(year, month, day, hour, minute) {
        formatDate(
            BilliCalendar.getInstance().apply {
                setDate(year, month, day)
                set(BilliCalendar.HOUR_OF_DAY, hour)
                set(BilliCalendar.MINUTE, minute)
                set(BilliCalendar.SECOND, 0)
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
        (accountEditable && selectedAccountId != current.accountId)
    val categoryChanged = selectedCategoryId != current.categoryId
    val merchantForMapping = title.trim()
    val selectedCategoryName = categories.firstOrNull { it.id == selectedCategoryId }?.name
    // A name the user corrected ("SwiggyLTD" → "Swiggy") can become a rule.
    val renameFrom = current.merchantName?.trim()
        ?.takeIf { it.isNotEmpty() && merchantForMapping.isNotEmpty() && it != merchantForMapping }
    val rememberRename = saveRename && renameFrom != null
    val rememberCategory = saveCategoryRule && merchantForMapping.isNotEmpty() && selectedCategoryName != null
    val canSave = detailsChanged || categoryChanged || rememberRename || rememberCategory

    fun resetEdits() {
        title = current.merchantName.orEmpty()
        note = current.note
        amountText = initialAmountText
        selectedCategoryId = current.categoryId
        selectedAccountId = current.accountId
        saveRename = false
        saveCategoryRule = false
        year = initialCal.get(BilliCalendar.YEAR)
        month = initialCal.get(BilliCalendar.MONTH)
        day = initialCal.get(BilliCalendar.DAY_OF_MONTH)
        hour = initialCal.get(BilliCalendar.HOUR_OF_DAY)
        minute = initialCal.get(BilliCalendar.MINUTE)
    }
    val startEditing = {
        haptics.click()
        resetEdits()
        editing = true
    }
    val stopEditing = {
        focusManager.clearFocus()
        resetEdits()
        editing = false
    }

    // Back leaves the edit page first, then the transaction.
    PlatformBackHandler(onBack = { if (editing) stopEditing() else onBack() })

    if (!editing) {
        // --- THE TRANSACTION, READ-ONLY. Tap a card or the button to edit. ---
        Box(modifier = Modifier.fillMaxSize()) {
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = MaterialTheme.spacing.medium)
                        // Room for the Edit button, so it never covers the last card.
                        .padding(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    TransactionHeroCard(
                        categoryIcon = current.categoryIcon,
                        categoryName = current.categoryName,
                        categoryColor = current.categoryColor,
                        displayTitle = current.merchantName?.takeIf { it.isNotBlank() }
                            ?: Markdown.plainText(current.note).ifBlank { current.categoryName },
                        isIncome = isIncome,
                        sourceLabel = sourceLabel,
                        currency = currency,
                        amount = current.amount
                    )

                    awaiting.firstOrNull { it.transaction.id == current.id }?.let { item ->
                        AwaitingAccountCard(item = item, onChoose = { choosingAccount = true })
                        if (choosingAccount) {
                            ChooseAccountSheet(
                                item = item,
                                accounts = accountChoices,
                                currency = currency,
                                onChoose = { account, always ->
                                    approvalViewModel.assign(item, account, always)
                                    choosingAccount = false
                                },
                                onDismiss = { choosingAccount = false }
                            )
                        }
                    }

                    DescriptionCard(note = current.note, onClick = startEditing)

                    TransactionDetailsCard(rows = detailRows, onClick = startEditing)

                    if (isSms && !current.smsBody.isNullOrBlank()) {
                        SmsMessageCard(
                            body = current.smsBody,
                            sender = if (current.source == "notification") current.bankName else current.smsSender,
                            isNotification = current.source == "notification",
                            expanded = smsExpanded,
                            onToggle = { smsExpanded = !smsExpanded }
                        )
                    }

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
                }
            }
            ExtendedFloatingActionButton(
                onClick = startEditing,
                icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                text = { Text("Edit") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(MaterialTheme.spacing.medium)
            )
        }
    } else {
        // --- THE EDIT PAGE ---
        Column(modifier = Modifier.fillMaxSize()) {
            DetailHeader(title = "Edit", onBack = stopEditing)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = MaterialTheme.spacing.medium)
                        .padding(bottom = MaterialTheme.spacing.large),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    EditTextFields(
                        isManual = isManual,
                        currency = currency,
                        amountText = amountText,
                        onAmountChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        title = title,
                        onTitleChange = { title = it },
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    )

                    CategoryPicker(
                        categories = filteredCategories,
                        selectedId = selectedCategoryId,
                        onSelect = { selectedCategoryId = it },
                        type = current.type
                    )

                    MarkdownField(
                        value = note,
                        onValueChange = { note = it },
                        label = "Description",
                        placeholder = "Add a description",
                        alwaysOpen = true
                    )

                    if (isManual) {
                        EditDateTimeRow(
                            year = year,
                            month = month,
                            day = day,
                            hour = hour,
                            minute = minute,
                            onPickDate = { showDatePicker = true },
                            onPickTime = { showTimePicker = true }
                        )
                    }
                    if (accountEditable) {
                        AccountPicker(
                            accounts = accounts.filter { it.type != UNASSIGNED_ACCOUNT_TYPE || it.id == current.accountId },
                            selectedId = selectedAccountId,
                            onSelect = { selectedAccountId = it }
                        )
                    }

                    MerchantRuleToggles(
                        merchant = merchantForMapping,
                        renameFrom = renameFrom,
                        categoryName = selectedCategoryName,
                        renameChecked = saveRename,
                        onRenameChange = { saveRename = it },
                        categoryChecked = saveCategoryRule,
                        onCategoryChange = { saveCategoryRule = it }
                    )
                }

                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = MaterialTheme.spacing.medium,
                                vertical = MaterialTheme.spacing.small
                            ),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptics.click()
                                stopEditing()
                            }
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (isManual && (parsedAmount == null || parsedAmount <= 0.0)) {
                                    haptics.reject()
                                    viewModel.showSnackbar("Enter a valid amount")
                                    return@Button
                                }
                                haptics.confirm()
                                focusManager.clearFocus()
                                viewModel.saveTransactionEdits(
                                    transactionId = current.id,
                                    title = title,
                                    note = note,
                                    categoryId = selectedCategoryId,
                                    rememberMerchant = rememberCategory,
                                    amount = if (isManual) parsedAmount else null,
                                    transactionDate = if (isManual) originalIso else null,
                                    accountId = if (accountEditable) selectedAccountId else null,
                                    rememberRename = rememberRename
                                )
                                // Back to the transaction, which now shows what was saved.
                                editing = false
                            },
                            modifier = Modifier.weight(1f),
                            enabled = canSave
                        ) {
                            Text("Save changes")
                        }
                    }
                }
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

/** The description, rendered. Blank notes get a one-line prompt instead. */
@Composable
private fun DescriptionCard(note: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
        ) {
            Text(
                text = "Description",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (note.isBlank()) {
                Text(
                    text = "Add a description",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                MarkdownText(source = note)
            }
        }
    }
}
