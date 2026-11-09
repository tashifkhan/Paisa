package codes.tashif.paisa.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import codes.tashif.paisa.data.AppTab
import codes.tashif.paisa.ui.components.AccountVisuals
import codes.tashif.paisa.ui.components.CategoryVisuals
import codes.tashif.paisa.ui.components.EmptyState
import codes.tashif.paisa.ui.components.PaisaTopBar
import codes.tashif.paisa.ui.components.TransactionRow
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing
import kotlin.math.abs

@Composable
fun RecordsScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val summary by viewModel.homeSummary.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filters by viewModel.txFilters.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val debtPeople by viewModel.debtPeople.collectAsState()
    val balancesHidden by viewModel.balancesHidden.collectAsState()
    val haptics = rememberHaptics()

    val isFiltering = searchQuery.isNotBlank() || filters.isActive

    Column(modifier = Modifier.fillMaxSize()) {
        PaisaTopBar(title = "Paisa")

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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                bottom = MaterialTheme.spacing.extraLarge
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
        ) {
            if (!isFiltering) {
                item {
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
                }
                if (summary.creditCards.isNotEmpty()) {
                    item {
                        CreditCardPanel(
                            summary = summary,
                            balancesHidden = balancesHidden
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                    }
                }
                if (debtPeople.any { abs(it.balance) >= 0.005 }) {
                    item {
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
            } else {
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
                            onClick = { viewModel.openTransaction(tx.id) }
                        )
                    }
                }
            }
        }
    }
}
