package codes.tashif.paisa.ui.home

import androidx.compose.foundation.layout.Arrangement
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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

    CenteredContent {
        Column(modifier = Modifier.fillMaxSize()) {
            PaisaTopBar(title = if (summaryInline) "Paisa" else "Transactions")

            SearchAndFilters(
                searchQuery = searchQuery,
                onSearchChange = viewModel::setSearchQuery,
                filters = filters,
                onFiltersChange = viewModel::setTxFilters,
                onClearAll = viewModel::clearTxFilters,
                categories = categories.map { cat ->
                    FilterMenuOption(
                        id = cat.id,
                        name = cat.name,
                        icon = CategoryVisuals.icon(cat.icon, cat.name)
                    )
                },
                accounts = accounts.map { acc ->
                    FilterMenuOption(
                        id = acc.id,
                        name = acc.name,
                        icon = AccountVisuals.icon(acc.type, acc.icon)
                    )
                }
            )

            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = MaterialTheme.spacing.medium,
                        end = MaterialTheme.spacing.medium,
                        bottom = MaterialTheme.spacing.extraLarge
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
                ) {
                    if (!isFiltering && summaryInline) {
                        item { HomeOverview(viewModel) }
                    } else if (isFiltering) {
                        item {
                            Text(
                                text = "${transactions.size} matching transactions",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = MaterialTheme.spacing.extraSmall)
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
                                DayHeader(
                                    day = day,
                                    currency = summary.currency,
                                    spent = dayTxs.filter { it.type == "expense" }.sumOf { it.amount },
                                    balancesHidden = balancesHidden
                                )
                            }
                            items(dayTxs, key = { it.id }) { tx ->
                                TransactionRow(
                                    tx = tx,
                                    currency = summary.currency,
                                    balancesHidden = balancesHidden,
                                    selected = tx.id == selectedId,
                                    onDelete = { confirmDelete = tx },
                                    onClick = { viewModel.openTransaction(tx.id) }
                                )
                            }
                        }
                    }
                }
                VerticalScrollbarFor(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
            }
        }
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
    Column {
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
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        if (summary.creditCards.isNotEmpty()) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.smaller))
            CreditCardPanel(summary = summary, balancesHidden = balancesHidden)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        }
        if (debtPeople.any { abs(it.balance) >= 0.005 }) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.smaller))
            DebtsPanel(
                people = debtPeople,
                currency = summary.currency,
                balancesHidden = balancesHidden,
                onOpen = {
                    haptics.click()
                    viewModel.selectTab(AppTab.DEBTS)
                }
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
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
