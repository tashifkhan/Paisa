package codes.tashif.paisa.ui.home

import codes.tashif.paisa.ui.platform.PlatformBackHandler

import codes.tashif.paisa.ui.app.LocalShellState

import androidx.compose.ui.unit.dp

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.DisposableEffect

import androidx.compose.foundation.clickable

import androidx.compose.animation.slideOutVertically

import androidx.compose.animation.slideInVertically

import androidx.compose.animation.fadeOut

import androidx.compose.animation.fadeIn


import codes.tashif.paisa.ui.components.TransactionGroup
import androidx.compose.foundation.layout.Arrangement
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import codes.tashif.paisa.data.AppTab
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.ui.app.CenteredContent
import codes.tashif.paisa.ui.app.LocalWindowWidth
import codes.tashif.paisa.ui.app.WindowWidth
import codes.tashif.paisa.ui.components.AccountVisuals
import codes.tashif.paisa.ui.components.CategoryVisuals
import codes.tashif.paisa.ui.components.EmptyState
import codes.tashif.paisa.ui.components.PaisaTopBar
import codes.tashif.paisa.ui.components.TransactionRow
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.platform.VerticalScrollbarFor
import codes.tashif.paisa.ui.theme.spacing
import codes.tashif.paisa.ui.transaction.DeleteTransactionDialog
import codes.tashif.paisa.ui.transaction.TransactionViewModel
import kotlin.math.abs

@Composable
fun RecordsScreen(viewModel: HomeViewModel = koinViewModel()) {
    val summary by viewModel.homeSummary.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filters by viewModel.txFilters.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val balancesHidden by viewModel.balancesHidden.collectAsState()
    val selectedId by viewModel.selectedTransactionId.collectAsState()
    val transactionViewModel: TransactionViewModel = koinViewModel()
    var confirmDelete by remember { mutableStateOf<TransactionWithDetails?>(null) }

    // On large windows the balances live in a pane beside the list.
    val summaryInline = LocalWindowWidth.current != WindowWidth.LARGE
    val isFiltering = searchQuery.isNotBlank() || filters.isActive
    val listState = rememberLazyListState()

    // ── Multi-select ──
    var selection by remember { mutableStateOf(emptySet<Int>()) }
    // The last row touched, so a long-press while selecting can grab a range.
    var anchorId by remember { mutableStateOf<Int?>(null) }
    var bulkSheet by remember { mutableStateOf<BulkSheet?>(null) }
    val selecting = selection.isNotEmpty()
    val shell = LocalShellState.current
    LaunchedEffect(selecting) { shell?.selectionMode = selecting }
    DisposableEffect(Unit) { onDispose { shell?.selectionMode = false } }
    PlatformBackHandler(enabled = selecting) { selection = emptySet() }
    // Rows that left the list (deleted, filtered out) leave the selection too.
    LaunchedEffect(transactions) {
        val visible = transactions.mapTo(HashSet()) { it.id }
        if (selection.any { it !in visible }) selection = selection.filterTo(HashSet()) { it in visible }
    }
    fun toggle(id: Int) {
        selection = if (id in selection) selection - id else selection + id
        anchorId = id
    }
    fun selectRange(id: Int) {
        val order = transactions.map { it.id }
        val a = order.indexOf(anchorId ?: id)
        val b = order.indexOf(id)
        if (a < 0 || b < 0) return toggle(id)
        selection = selection + order.subList(minOf(a, b), maxOf(a, b) + 1)
        anchorId = id
    }
    val selectedTxs = transactions.filter { it.id in selection }

    CenteredContent {
        Column(modifier = Modifier.fillMaxSize()) {
            PaisaTopBar(title = if (summaryInline) "Paisa" else "Transactions")

            if (selecting) {
                SelectionBar(
                    count = selection.size,
                    total = transactions.size,
                    onClose = { selection = emptySet() },
                    onToggleAll = {
                        selection = if (selection.size >= transactions.size) emptySet()
                        else transactions.mapTo(HashSet()) { it.id }
                    }
                )
            } else SearchAndFilters(
                searchQuery = searchQuery,
                onSearchChange = viewModel::setSearchQuery,
                filters = filters,
                onFiltersChange = viewModel::setTxFilters,
                onClearAll = viewModel::clearTxFilters,
                categories = categories.map { cat ->
                    FilterMenuOption(
                        id = cat.id,
                        name = cat.name,
                        icon = CategoryVisuals.icon(cat.icon, cat.name),
                        type = cat.type
                    )
                },
                accounts = accounts.map { acc ->
                    FilterMenuOption(
                        id = acc.id,
                        name = acc.name,
                        icon = AccountVisuals.icon(acc.type, acc.icon)
                    )
                },
                currency = summary.currency,
                resultCount = transactions.size
            )

            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = MaterialTheme.spacing.medium,
                        end = MaterialTheme.spacing.medium,
                        // Room for the floating toolbar while selecting.
                        bottom = if (selecting) 112.dp else MaterialTheme.spacing.extraLarge
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
                ) {
                    if (!isFiltering && summaryInline) {
                        item { HomeOverview(viewModel) }
                    } else if (isFiltering) {
                        item {
                            FilterResultsSummary(
                                transactions = transactions,
                                currency = summary.currency,
                                hidden = balancesHidden
                            )
                        }
                    }

                    if (transactions.isEmpty()) {
                        item {
                            EmptyState(
                                title = if (isFiltering) "No matches" else "No transactions yet",
                                subtitle = if (isFiltering) {
                                    "Try a different search or clear filters"
                                } else {
                                    "Tap + to record your first expense or income"
                                }
                            )
                        }
                    } else {
                        // Transactions arrive sorted newest-first; group them into day sections.
                        val byDay = transactions.groupBy { it.transactionDate.take(10) }
                        byDay.forEach { (day, dayTxs) ->
                            item(key = "day_$day") {
                                val dayIds = dayTxs.map { it.id }
                                Box(
                                    Modifier.then(
                                        if (selecting) Modifier.clickable {
                                            selection = if (selection.containsAll(dayIds)) selection - dayIds.toSet()
                                            else selection + dayIds
                                        } else Modifier
                                    )
                                ) {
                                DayHeader(
                                    day = day,
                                    currency = summary.currency,
                                    net = dayTxs.dayNet(),
                                    balancesHidden = balancesHidden
                                )
                                }
                            }
                            item(key = "group_$day") {
                                TransactionGroup(dayTxs) { tx, position ->
                                    TransactionRow(
                                        tx = tx,
                                        currency = summary.currency,
                                        balancesHidden = balancesHidden,
                                        selected = !selecting && tx.id == selectedId,
                                        onDelete = { confirmDelete = tx },
                                        position = position,
                                        checked = if (selecting) tx.id in selection else null,
                                        onLongClick = { if (selecting) selectRange(tx.id) else toggle(tx.id) },
                                        onClick = { if (selecting) toggle(tx.id) else viewModel.openTransaction(tx.id) }
                                    )
                                }
                            }
                        }
                    }
                }
                VerticalScrollbarFor(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())

                androidx.compose.animation.AnimatedVisibility(
                    visible = selecting,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = MaterialTheme.spacing.medium)
                ) {
                    BulkToolbar(
                        enabled = selecting,
                        onRename = { bulkSheet = BulkSheet.RENAME },
                        onCategory = { bulkSheet = BulkSheet.CATEGORY },
                        onAccount = { bulkSheet = BulkSheet.ACCOUNT },
                        onDelete = { bulkSheet = BulkSheet.DELETE }
                    )
                }
            }
        }
    }

    val ids = selection
    fun done() {
        bulkSheet = null
        selection = emptySet()
    }
    when (bulkSheet) {
        BulkSheet.RENAME -> BulkRenameSheet(
            items = selectedTxs,
            onApply = { names ->
                transactionViewModel.renameTransactions(names)
                done()
            },
            onDismiss = { bulkSheet = null }
        )
        BulkSheet.CATEGORY -> BulkPickSheet(
            title = "Move ${ids.size} to category",
            sections = listOf("expense" to "Expense", "income" to "Income").map { (type, label) ->
                label to categories.filter { it.type == type }.map {
                    PickOption(it.id, it.name, null, CategoryVisuals.icon(it.icon, it.name))
                }
            },
            onPick = {
                transactionViewModel.setCategoryFor(ids, it.id)
                done()
            },
            onDismiss = { bulkSheet = null }
        )
        BulkSheet.ACCOUNT -> BulkPickSheet(
            title = "Move ${ids.size} to account",
            sections = listOf(null to accounts.map {
                PickOption(it.id, it.name, it.type, AccountVisuals.icon(it.type, it.icon))
            }),
            onPick = {
                transactionViewModel.setAccountFor(ids, it.id, it.name)
                done()
            },
            onDismiss = { bulkSheet = null }
        )
        BulkSheet.DELETE -> BulkDeleteDialog(
            count = ids.size,
            fromSms = selectedTxs.count { it.source == "sms" },
            onConfirm = {
                transactionViewModel.deleteTransactions(ids)
                done()
            },
            onDismiss = { bulkSheet = null }
        )
        null -> Unit
    }

    confirmDelete?.let { tx ->
        DeleteTransactionDialog(
            isManual = tx.source == "manual",
            onConfirm = {
                confirmDelete = null
                transactionViewModel.deleteTransaction(tx.id)
            },
            onDismiss = { confirmDelete = null }
        )
    }
}

/** Balance, credit cards and debts: the top of Home on a phone, a side pane on a desktop. */
@Composable
private fun HomeOverview(viewModel: HomeViewModel) {
    val summary by viewModel.homeSummary.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val debtPeople by viewModel.debtPeople.collectAsState()
    val balancesHidden by viewModel.balancesHidden.collectAsState()
    val haptics = rememberHaptics()
    // Same gap between every cluster; the list's own spacing takes over after.
    Column(
        modifier = Modifier.padding(bottom = MaterialTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        BalanceCard(
            currency = summary.currency,
            totalBalance = summary.totalBalance,
            income = summary.monthIncome,
            expense = summary.monthExpense,
            accounts = accounts,
            balancesHidden = balancesHidden,
            onToggleBalancesHidden = {
                haptics.toggle(on = balancesHidden)
                viewModel.toggleBalancesHidden()
            }
        )
        if (summary.creditCards.isNotEmpty()) {
            CreditCardPanel(summary = summary, balancesHidden = balancesHidden)
        }
        if (debtPeople.any { abs(it.balance) >= 0.005 }) {
            DebtsPanel(
                people = debtPeople,
                currency = summary.currency,
                balancesHidden = balancesHidden,
                onOpen = {
                    haptics.click()
                    viewModel.selectTab(AppTab.DEBTS)
                }
            )
        }
    }
}

/** The overview beside the transaction list on large windows. */
@Composable
fun HomeSummaryPane(modifier: Modifier = Modifier, viewModel: HomeViewModel = koinViewModel()) {
    val scroll = rememberScrollState()
    Box(modifier) {
        Column(Modifier.fillMaxSize()) {
            PaisaTopBar(title = "Overview")
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(start = MaterialTheme.spacing.small, end = MaterialTheme.spacing.large)
                    .padding(bottom = MaterialTheme.spacing.large)
            ) {
                HomeOverview(viewModel)
            }
        }
        VerticalScrollbarFor(scroll, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

/** "12 transactions · ₹4,210.00 out · ₹200.00 in" above a filtered list. */
@Composable
private fun FilterResultsSummary(
    transactions: List<TransactionWithDetails>,
    currency: String,
    hidden: Boolean
) {
    val out = transactions.filter { it.type == "expense" }.sumOf { it.amount }
    val inc = transactions.filter { it.type == "income" }.sumOf { it.amount }
    val n = transactions.size
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(vertical = MaterialTheme.spacing.extraSmall),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = "$n transaction${if (n == 1) "" else "s"}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (out > 0) {
            Text("  ·  ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
            codes.tashif.paisa.ui.components.MoneyText(
                currency = currency, amount = out, hidden = hidden,
                style = MaterialTheme.typography.labelLarge,
                color = codes.tashif.paisa.ui.theme.ExpenseRed, maskCount = 4
            )
            Text(" out", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (inc > 0) {
            Text("  ·  ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
            codes.tashif.paisa.ui.components.MoneyText(
                currency = currency, amount = inc, hidden = hidden,
                style = MaterialTheme.typography.labelLarge,
                color = codes.tashif.paisa.ui.theme.IncomeGreen, maskCount = 4
            )
            Text(" in", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private enum class BulkSheet { RENAME, CATEGORY, ACCOUNT, DELETE }
