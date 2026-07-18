package codes.tashif.paisa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.data.PaisaViewModel
import codes.tashif.paisa.ui.components.AccountVisuals
import codes.tashif.paisa.ui.components.EmptyState
import codes.tashif.paisa.ui.components.GroupPosition
import codes.tashif.paisa.ui.components.PaisaTopBar
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.components.groupShape
import codes.tashif.paisa.ui.haptics.rememberHaptics
import kotlin.math.roundToInt
import codes.tashif.paisa.ui.theme.spacing

@Composable
fun AccountsScreen(viewModel: PaisaViewModel) {
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val summary by viewModel.homeSummary.collectAsState()
    var accountToRename by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var accountForActions by remember { mutableStateOf<Account?>(null) }
    var listView by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var showMergeDialog by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    Column(modifier = Modifier.fillMaxSize()) {
        PaisaTopBar(
            title = "Accounts",
            actions = {
                IconButton(onClick = {
                    haptics.toggle(on = !listView)
                    listView = !listView
                    selectedIds = emptySet()
                }) {
                    Icon(
                        imageVector = if (listView) {
                            Icons.Rounded.ViewCarousel
                        } else {
                            Icons.AutoMirrored.Rounded.ViewList
                        },
                        contentDescription = if (listView) "Card view" else "List view"
                    )
                }
            }
        )

        if (accounts.isEmpty()) {
            EmptyState(
                title = "No accounts",
                subtitle = "Accounts appear here after your first SMS scan or import"
            )
            return@Column
        }

        if (listView) {
            AccountListView(
                accounts = accounts,
                currency = summary.currency,
                selectedIds = selectedIds,
                onToggleSelect = { id ->
                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                },
                onOpenActions = { accountForActions = it },
                onReorder = { viewModel.reorderAccounts(it) },
                onClearSelection = { selectedIds = emptySet() },
                onMergeClick = { showMergeDialog = true },
                onAutoMerge = { viewModel.autoMergeDuplicateAccounts() }
            )
        } else {
            AccountCardsView(
                accounts = accounts,
                transactions = transactions,
                summary = summary,
                viewModel = viewModel,
                onRename = { accountToRename = it }
            )
        }
    }

    accountToRename?.let { account ->
        RenameAccountDialog(
            account = account,
            onDismiss = { accountToRename = null },
            onSave = { newName ->
                viewModel.renameAccount(account, newName)
                accountToRename = null
            }
        )
    }

    accountForActions?.let { account ->
        AccountActionsSheet(
            account = account,
            onDismiss = { accountForActions = null },
            onRename = {
                accountForActions = null
                accountToRename = account
            },
            onSetDefault = {
                accountForActions = null
                viewModel.setDefaultAccount(account)
            },
            onDelete = {
                accountForActions = null
                accountToDelete = account
            }
        )
    }

    accountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete ${account.name}?") },
            text = {
                Text(
                    "This permanently deletes the account and every transaction " +
                        "recorded against it. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    haptics.reject()
                    viewModel.deleteAccountCascade(account)
                    accountToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMergeDialog && selectedIds.size >= 2) {
        MergeAccountsDialog(
            accounts = accounts.filter { it.id in selectedIds },
            currency = summary.currency,
            onDismiss = { showMergeDialog = false },
            onMerge = { targetId ->
                viewModel.mergeAccounts(
                    targetId = targetId,
                    sourceIds = selectedIds.filter { it != targetId }
                )
                selectedIds = emptySet()
                showMergeDialog = false
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AccountListView(
    accounts: List<Account>,
    currency: String,
    selectedIds: Set<Int>,
    onToggleSelect: (Int) -> Unit,
    onOpenActions: (Account) -> Unit,
    onReorder: (List<Int>) -> Unit,
    onClearSelection: () -> Unit,
    onMergeClick: () -> Unit,
    onAutoMerge: () -> Unit
) {
    val selectionMode = selectedIds.isNotEmpty()
    val haptics = rememberHaptics()

    // Local visual order during a drag; re-synced from the DB order between drags.
    val localOrder = remember { accounts.map { it.id }.toMutableStateList() }
    val dragState = remember { AccountDragState() }
    // Assigned every composition so the drag always mutates the live list,
    // never a stale capture from when the state object was first created.
    dragState.onMove = { from, to ->
        if (from in localOrder.indices && to in localOrder.indices) {
            haptics.tick()
            localOrder.add(to, localOrder.removeAt(from))
        }
    }
    LaunchedEffect(accounts) {
        if (dragState.draggingIndex == null) {
            localOrder.clear()
            localOrder.addAll(accounts.map { it.id })
        }
    }
    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    val displayAccounts = localOrder.mapNotNull { accountsById[it] } +
        accounts.filter { it.id !in localOrder }

    val listState = rememberLazyListState()
    val count = displayAccounts.size

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.spacing.medium + MaterialTheme.spacing.small,
                    end = MaterialTheme.spacing.medium,
                    top = MaterialTheme.spacing.medium,
                    bottom = MaterialTheme.spacing.smaller
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectionMode) {
                    "${selectedIds.size} selected"
                } else {
                    "Your accounts"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            when {
                selectedIds.size >= 2 -> {
                    TextButton(onClick = onClearSelection) { Text("Cancel") }
                    Button(onClick = {
                        haptics.click()
                        onMergeClick()
                    }) {
                        Text("Merge ${selectedIds.size}")
                    }
                }
                selectionMode -> {
                    TextButton(onClick = onClearSelection) { Text("Cancel") }
                }
                else -> {
                    TextButton(onClick = {
                        haptics.click()
                        onAutoMerge()
                    }) {
                        Text("Auto-merge")
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                top = MaterialTheme.spacing.extraSmall,
                bottom = MaterialTheme.spacing.extraLarge
            ),
            // 2dp gaps match SettingsGroup / Pixel settings stacks
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(displayAccounts, key = { _, account -> account.id }) { index, account ->
                val isDragging = dragState.draggingIndex == index
                AccountListRow(
                    account = account,
                    currency = currency,
                    position = groupPositionOf(index, count),
                    selectionMode = selectionMode,
                    selected = account.id in selectedIds,
                    isDragging = isDragging,
                    dragOffset = if (isDragging) dragState.offset else 0f,
                    onClick = {
                        if (selectionMode) {
                            haptics.toggle(on = account.id !in selectedIds)
                            onToggleSelect(account.id)
                        } else {
                            haptics.click()
                            onOpenActions(account)
                        }
                    },
                    // combinedClickable already plays the system long-press haptic
                    onLongClick = { onToggleSelect(account.id) },
                    onDragStart = {
                        dragState.start(
                            displayAccounts.indexOfFirst { it.id == account.id },
                            listState
                        )
                    },
                    onDrag = { dy ->
                        dragState.drag(dy, displayAccounts.size)
                    },
                    onDragEnd = {
                        dragState.end()
                        onReorder(localOrder.toList())
                    },
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .then(if (isDragging) Modifier else Modifier.animateItem())
                )
            }
        }
    }
}

/**
 * Tracks the actively dragged row. Positions are computed arithmetically from
 * the accumulated drag distance and a fixed slot height captured at drag start,
 * so item placement animations can't feed back into the math.
 */
private class AccountDragState {
    /** Reassigned on every composition so it always sees the current list. */
    var onMove: (Int, Int) -> Unit = { _, _ -> }

    var draggingIndex by androidx.compose.runtime.mutableStateOf<Int?>(null)
        private set
    private var startIndex = 0
    private var slotPx = 0f
    private var rawDelta by androidx.compose.runtime.mutableFloatStateOf(0f)

    /** Visual translation for the dragged row relative to its current slot. */
    val offset: Float
        get() {
            val current = draggingIndex ?: return 0f
            return rawDelta - (current - startIndex) * slotPx
        }

    fun start(index: Int, listState: LazyListState) {
        if (index < 0) return
        val items = listState.layoutInfo.visibleItemsInfo
        // Slot height = distance between consecutive item offsets (incl. spacing)
        slotPx = items.zipWithNext { a, b -> (b.offset - a.offset).toFloat() }
            .firstOrNull { it > 0f }
            ?: items.firstOrNull()?.size?.toFloat()
            ?: return
        startIndex = index
        draggingIndex = index
        rawDelta = 0f
    }

    fun drag(dy: Float, itemCount: Int) {
        val current = draggingIndex ?: return
        rawDelta += dy
        if (slotPx <= 0f) return
        val target = (startIndex + (rawDelta / slotPx).roundToInt())
            .coerceIn(0, itemCount - 1)
        if (target != current) {
            onMove(current, target)
            draggingIndex = target
        }
    }

    fun end() {
        draggingIndex = null
        rawDelta = 0f
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AccountListRow(
    account: Account,
    currency: String,
    position: GroupPosition,
    selectionMode: Boolean,
    selected: Boolean,
    isDragging: Boolean,
    dragOffset: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pixel-settings surface: large corners on group ends, 6dp between rows.
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = dragOffset
                shadowElevation = if (isDragging) 8.dp.toPx() else 0f
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = groupShape(position),
        color = when {
            isDragging -> MaterialTheme.colorScheme.surfaceContainerHigh
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.small + 2.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                val haptics = rememberHaptics()
                Checkbox(
                    checked = selected,
                    onCheckedChange = {
                        haptics.toggle(on = !selected)
                        onLongClick()
                    }
                )
            } else {
                val accent = AccountVisuals.color(account.color)
                    ?: MaterialTheme.colorScheme.primaryContainer
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AccountVisuals.icon(account.type, account.icon),
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.size(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (account.isDefault) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = "Default account",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(16.dp)
                        )
                    }
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = listOfNotNull(
                        account.type,
                        account.bankName,
                        account.accountLast4?.let { "••$it" }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.size(MaterialTheme.spacing.smaller))
            Text(
                text = formatMoney(currency, account.currentBalance),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (!selectionMode) {
                Icon(
                    imageVector = Icons.Rounded.DragIndicator,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .padding(start = MaterialTheme.spacing.extraSmall)
                        .size(22.dp)
                        .pointerInput(account.id) {
                            detectDragGestures(
                                onDragStart = { onDragStart() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.y)
                                },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() }
                            )
                        }
                )
            }
        }
    }
}

private val accountSeedColors = listOf(
    "#4CAF50", "#2196F3", "#FF9800", "#9C27B0",
    "#E91E63", "#00BCD4", "#FFC107", "#795548"
)

private val accountTypes = listOf(
    "Bank Account", "Cash", "UPI", "Credit Card", "Debit Card", "Wallet", "Other"
)

/** Add-account bottom sheet wired to the ViewModel; opened from the Accounts FAB. */
@Composable
fun AddAccountSheetHost(
    viewModel: PaisaViewModel,
    onDismiss: () -> Unit
) {
    AddAccountSheet(
        onDismiss = onDismiss,
        onAdd = { name, type, openingBalance ->
            viewModel.addAccount(
                name = name,
                type = type,
                openingBalance = openingBalance,
                icon = if (type == "Credit Card" || type == "Debit Card") {
                    "credit_card"
                } else {
                    "account_balance"
                },
                color = accountSeedColors[
                    kotlin.math.abs(name.hashCode()) % accountSeedColors.size
                ]
            )
            onDismiss()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, type: String, openingBalance: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(accountTypes.first()) }
    var balanceText by remember { mutableStateOf("") }
    val haptics = rememberHaptics()

    val typeOptions = remember {
        accountTypes.map { option ->
            codes.tashif.paisa.ui.components.ChoiceOption(
                id = option,
                label = option,
                icon = codes.tashif.paisa.ui.components.AccountVisuals.icon(option)
            )
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = "Add account",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Name it and pick how you spend from it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Account name") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            codes.tashif.paisa.ui.components.SheetSectionLabel("Type")
            codes.tashif.paisa.ui.components.OptionChipGrid(
                options = typeOptions,
                selectedId = type,
                onSelect = { type = it }
            )
            OutlinedTextField(
                value = balanceText,
                onValueChange = { text ->
                    if (text.isEmpty() || text.toDoubleOrNull() != null || text == "-") {
                        balanceText = text
                    }
                },
                label = { Text("Opening balance") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    haptics.confirm()
                    onAdd(name, type, balanceText.toDoubleOrNull() ?: 0.0)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text("Add account", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountActionsSheet(
    account: Account,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit
) {
    val actionCount = if (account.isDefault) 2 else 3
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.large)
        ) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    start = MaterialTheme.spacing.small,
                    bottom = MaterialTheme.spacing.smaller
                )
            )
            SettingsGroup {
                SettingsItem(
                    title = "Rename",
                    subtitle = null,
                    icon = Icons.Rounded.Edit,
                    position = groupPositionOf(0, actionCount),
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onRename
                )
                if (!account.isDefault) {
                    SettingsItem(
                        title = "Set as default",
                        subtitle = "Preselected when adding transactions",
                        icon = Icons.Rounded.Star,
                        position = groupPositionOf(1, actionCount),
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = onSetDefault
                    )
                }
                SettingsItem(
                    title = "Delete",
                    subtitle = "Removes the account and all its transactions",
                    icon = Icons.Rounded.Delete,
                    position = groupPositionOf(actionCount - 1, actionCount),
                    iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                    iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = onDelete
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountCardsView(
    accounts: List<Account>,
    transactions: List<codes.tashif.paisa.data.TransactionWithDetails>,
    summary: codes.tashif.paisa.data.HomeSummary,
    viewModel: PaisaViewModel,
    onRename: (Account) -> Unit
) {
    // M3 multi-browse carousel — same layout as m3.material.io / Compose samples:
    // large focal item + smaller peeks that morph size on scroll, with maskClip.
    val carouselState = rememberCarouselState { accounts.size }
    val currentItem = carouselState.currentItem
    val currentAccount = accounts.getOrNull(currentItem)
    val accountTx = transactions.filter { it.accountId == currentAccount?.id }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 280.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(top = MaterialTheme.spacing.small, bottom = MaterialTheme.spacing.small),
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { page ->
            val account = accounts[page]
            AccountCarouselItem(
                account = account,
                currency = summary.currency,
                onEdit = { onRename(account) },
                modifier = Modifier
                    .height(205.dp)
                    .maskClip(MaterialTheme.shapes.extraLarge)
            )
        }

        Text(
            text = currentAccount?.let { "${it.name} · Transactions" } ?: "Transactions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.extraSmall
            )
        )

        if (accountTx.isEmpty()) {
            EmptyState(
                title = "Nothing here yet",
                subtitle = "Transactions for this account will appear here"
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.medium,
                    end = MaterialTheme.spacing.medium,
                    bottom = MaterialTheme.spacing.extraLarge
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                items(accountTx, key = { it.id }) { tx ->
                    TransactionRow(
                        tx = tx,
                        currency = summary.currency,
                        onClick = { viewModel.openTransaction(tx.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MergeAccountsDialog(
    accounts: List<Account>,
    currency: String,
    onDismiss: () -> Unit,
    onMerge: (targetId: Int) -> Unit
) {
    var targetId by remember {
        // Default to the account with the most complete metadata / highest balance
        mutableIntStateOf(
            accounts.maxByOrNull { (if (it.accountLast4 != null) 1 else 0) * 1_000_000 + it.currentBalance }
                ?.id ?: accounts.first().id
        )
    }

    val haptics = rememberHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge accounts") },
        text = {
            Column {
                Text(
                    text = "All transactions move into the account you keep. " +
                        "Its balance becomes the latest SMS-reported balance. " +
                        "The other accounts are deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                accounts.forEach { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (targetId != account.id) haptics.tick()
                                targetId = account.id
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = targetId == account.id,
                            onClick = {
                                if (targetId != account.id) haptics.tick()
                                targetId = account.id
                            }
                        )
                        Column {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = listOfNotNull(
                                    account.bankName,
                                    account.accountLast4?.let { "••$it" },
                                    formatMoney(currency, account.currentBalance)
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                haptics.confirm()
                onMerge(targetId)
            }) {
                Text("Merge into selected")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Account surface for M3 multi-browse carousel.
 * Shape/size are owned by [Modifier.maskClip] from [CarouselItemScope] so items
 * morph and clip exactly like the m3.material.io demos.
 */
@Composable
private fun AccountCarouselItem(
    account: Account,
    currency: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = runCatching { Color(android.graphics.Color.parseColor(account.color)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)
    val cardColor = baseColor.copy(alpha = 0.9f)
        .compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh)
    val gradient = Brush.linearGradient(
        colors = listOf(
            cardColor,
            cardColor.copy(alpha = 0.7f).compositeOver(Color.Black.copy(alpha = 0.4f))
        )
    )
    val onCard = Color.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(gradient)
    ) {
        // Soft vignette so small peeks still read as rich visuals
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.25f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.type.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = onCard.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onCard,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Rename account",
                        tint = onCard.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = listOfNotNull(
                    account.bankName,
                    account.accountLast4?.let { "•••• $it" }
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = onCard.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            Text(
                text = formatMoney(currency, account.currentBalance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = onCard,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            account.creditLimit?.let { limit ->
                Text(
                    text = "Limit ${formatMoney(currency, limit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = onCard.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RenameAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember(account.id) { mutableStateOf(account.name) }
    val haptics = rememberHaptics()

    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename account") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Account name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    haptics.confirm()
                    onSave(name)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
