package sh.taf.billi.ui.transaction

import sh.taf.billi.ui.components.AdaptiveSheet
import sh.taf.billi.platform.formatDate
import sh.taf.billi.platform.BilliCalendar
import androidx.compose.foundation.layout.Column
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import sh.taf.billi.ui.debts.split.plain
import sh.taf.billi.ui.components.AccountPicker
import sh.taf.billi.ui.components.CategoryPicker
import sh.taf.billi.ui.components.PillTabs
import sh.taf.billi.ui.components.swipeToSwitch
import sh.taf.billi.ui.components.SheetToggleCard
import sh.taf.billi.ui.statement.rememberPhotoPicker
import sh.taf.billi.ui.statement.rememberStatementPicker
import androidx.compose.runtime.saveable.rememberSaveable
import sh.taf.billi.ui.haptics.rememberHaptics
import sh.taf.billi.ui.theme.spacing
import sh.taf.billi.ui.quickadd.AiNoteStep
import sh.taf.billi.ui.quickadd.AiReviewStep
import sh.taf.billi.ui.quickadd.AiAddViewModel
import sh.taf.billi.ui.ai.LocalAiEnabled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    onDismiss: () -> Unit,
    onOpenAiSettings: () -> Unit = {},
    viewModel: TransactionViewModel = koinViewModel(),
    aiViewModel: AiAddViewModel = koinViewModel()
) {
    val aiState by aiViewModel.state.collectAsState()
    // Come back to the AI side when a draft is running or waiting for review.
    var aiTab by rememberSaveable { mutableStateOf(aiState.inProgress || aiState.shareArrived) }
    // With AI turned off in More the sheet is the manual form alone.
    val aiEnabled = LocalAiEnabled.current
    val withAi = aiEnabled && aiTab
    // A share from another app lands on the AI side, even with the sheet already open.
    LaunchedEffect(aiState.shareArrived) {
        if (aiState.shareArrived) {
            aiTab = true
            aiViewModel.consumeShareArrived()
        }
    }
    val aiReady = remember(withAi) { aiViewModel.canRun() }
    val aiCanHear = remember(withAi) { aiViewModel.canHear() }
    val openPhotos = rememberPhotoPicker { aiViewModel.addFiles(it) }
    val openFiles = rememberStatementPicker { aiViewModel.addFiles(it) }
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

    val now = remember { BilliCalendar.getInstance() }
    var year by remember { mutableStateOf(now.get(BilliCalendar.YEAR)) }
    var month by remember { mutableStateOf(now.get(BilliCalendar.MONTH)) }
    var day by remember { mutableStateOf(now.get(BilliCalendar.DAY_OF_MONTH)) }
    var hour by remember { mutableStateOf(now.get(BilliCalendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(now.get(BilliCalendar.MINUTE)) }

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
        val cal = BilliCalendar.getInstance().apply {
            setDate(year, month, day)
        }
        formatDate(cal.timeInMillis, "EEE, d MMM yyyy")
    }
    val timeLabel = remember(hour, minute) {
        val cal = BilliCalendar.getInstance().apply {
            set(BilliCalendar.HOUR_OF_DAY, hour)
            set(BilliCalendar.MINUTE, minute)
        }
        formatDate(cal.timeInMillis, "h:mm a")
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    AdaptiveSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Swipe sideways to switch between Manual and With AI.
                .swipeToSwitch(
                    enabled = aiEnabled && !(withAi && aiState.reviewing),
                    onPrevious = { aiTab = false },
                    onNext = { aiTab = true }
                )
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.large)
                .navigationBarsPadding()
        ) {
            Text(
                text = "New transaction",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    !withAi -> "Log a manual expense or income"
                    aiState.reviewing -> "Edit anything that looks off, then save"
                    else -> "Describe it, or attach receipts, bills, PDFs and spreadsheets"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(MaterialTheme.spacing.medium))

            if (aiEnabled && !(withAi && aiState.reviewing)) {
                PillTabs(
                    options = listOf("Manual" to false, "With AI" to true),
                    selected = withAi,
                    onSelect = { aiTab = it }
                )
                Spacer(Modifier.height(MaterialTheme.spacing.medium))
            }

            if (withAi) {
                if (aiState.reviewing) {
                    AiReviewStep(
                        state = aiState,
                        categories = categories,
                        accounts = accounts,
                        currency = currency,
                        onBack = aiViewModel::backToNote,
                        onUpdate = aiViewModel::updateRow,
                        onRemove = aiViewModel::removeRow,
                        onSelectAll = aiViewModel::setAllSelected,
                        onRefine = aiViewModel::refine,
                        onSave = {
                            haptics.confirm()
                            aiViewModel.save(onSaved = onDismiss)
                        }
                    )
                } else {
                    AiNoteStep(
                        state = aiState,
                        aiReady = aiReady,
                        canHear = aiCanHear,
                        onNoteChange = aiViewModel::setNote,
                        onAddPhotos = {
                            aiViewModel.onExternalActivityLaunched()
                            openPhotos()
                        },
                        onAddFiles = {
                            aiViewModel.onExternalActivityLaunched()
                            openFiles()
                        },
                        onRemoveFile = aiViewModel::removeFile,
                        onVoiceRecorded = aiViewModel::setVoice,
                        onRemoveVoice = aiViewModel::removeVoice,
                        onBeforeMicPrompt = aiViewModel::onExternalActivityLaunched,
                        onDraft = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            aiViewModel.draft()
                        },
                        onCancel = aiViewModel::cancel,
                        onOpenAiSettings = onOpenAiSettings
                    )
                }
            } else {
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
                onTitleNext = { focusManager.moveFocus(FocusDirection.Down) }
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
                emptyText = "No $type categories yet",
                type = type
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
                currency = currency,
                onScannedTotal = { amountText = plain(it) }
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
                    val cal = BilliCalendar.getInstance().apply {
                        setDate(year, month, day)
                        set(BilliCalendar.HOUR_OF_DAY, hour)
                        set(BilliCalendar.MINUTE, minute)
                        set(BilliCalendar.SECOND, 0)
                        set(BilliCalendar.MILLISECOND, 0)
                    }
                    val iso = formatDate(cal.timeInMillis, "yyyy-MM-dd'T'HH:mm:ss")
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
