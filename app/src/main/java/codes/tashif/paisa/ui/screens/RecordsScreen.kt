package codes.tashif.paisa.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.PaisaViewModel
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.data.TxFilters
import codes.tashif.paisa.ui.components.AccountVisuals
import codes.tashif.paisa.ui.components.CategoryBadge
import codes.tashif.paisa.ui.components.CategoryVisuals
import codes.tashif.paisa.ui.components.EmptyState
import codes.tashif.paisa.ui.components.HiddenMoneyMask
import codes.tashif.paisa.ui.components.MoneyText
import codes.tashif.paisa.ui.components.PaisaTopBar
import codes.tashif.paisa.ui.components.SearchLeadingIcon
import codes.tashif.paisa.ui.components.formatMoneyAmount
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen
import codes.tashif.paisa.ui.theme.spacing
import java.util.Locale

@Composable
fun RecordsScreen(viewModel: PaisaViewModel) {
    val summary by viewModel.homeSummary.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filters by viewModel.txFilters.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
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

private data class FilterMenuOption(
    val id: Int,
    val name: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchAndFilters(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filters: TxFilters,
    onFiltersChange: (TxFilters) -> Unit,
    onClearAll: () -> Unit,
    categories: List<FilterMenuOption>,
    accounts: List<FilterMenuOption>
) {
    val haptics = rememberHaptics()
    Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium)) {
        // M3 search bar — collapsed input that filters the list in place.
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = onSearchChange,
                    onSearch = { onSearchChange(it) },
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text("Search transactions…") },
                    leadingIcon = {
                        SearchLeadingIcon(
                            query = searchQuery,
                            onClear = { onSearchChange("") }
                        )
                    },
                    trailingIcon = null
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier.fillMaxWidth(),
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shadowElevation = 0.dp,
            // Nested under PaisaTopBar — don't re-apply status-bar insets (causes a huge top gap).
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {}

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = MaterialTheme.spacing.smaller),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
        ) {
            FilterChip(
                selected = filters.type == "expense",
                onClick = {
                    haptics.toggle(on = filters.type != "expense")
                    onFiltersChange(
                        filters.copy(type = if (filters.type == "expense") null else "expense")
                    )
                },
                label = { Text("Expenses") }
            )
            FilterChip(
                selected = filters.type == "income",
                onClick = {
                    haptics.toggle(on = filters.type != "income")
                    onFiltersChange(
                        filters.copy(type = if (filters.type == "income") null else "income")
                    )
                },
                label = { Text("Income") }
            )
            MultiSelectFilterChip(
                emptyLabel = "Category",
                selectedIds = filters.categoryIds,
                options = categories,
                clearIcon = Icons.Rounded.Category,
                onSelectionChange = { onFiltersChange(filters.copy(categoryIds = it)) }
            )
            MultiSelectFilterChip(
                emptyLabel = "Account",
                selectedIds = filters.accountIds,
                options = accounts,
                clearIcon = Icons.Rounded.AccountBalanceWallet,
                onSelectionChange = { onFiltersChange(filters.copy(accountIds = it)) }
            )
            FilterChip(
                selected = filters.source == "sms",
                onClick = {
                    haptics.toggle(on = filters.source != "sms")
                    onFiltersChange(
                        filters.copy(source = if (filters.source == "sms") null else "sms")
                    )
                },
                label = { Text("From SMS") }
            )
            if (filters.isActive || searchQuery.isNotBlank()) {
                FilterChip(
                    selected = false,
                    onClick = {
                        haptics.click()
                        onClearAll()
                    },
                    label = { Text("Clear") },
                    trailingIcon = {
                        Icon(
                            Icons.Rounded.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MultiSelectFilterChip(
    emptyLabel: String,
    selectedIds: Set<Int>,
    options: List<FilterMenuOption>,
    clearIcon: ImageVector,
    onSelectionChange: (Set<Int>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()
    val hasSelection = selectedIds.isNotEmpty()
    // +1 when a "Clear all" row is shown so item shapes stay correct.
    val itemCount = options.size + if (hasSelection) 1 else 0
    val menuScroll = rememberScrollState()
    // Cap height so long category/account lists scroll instead of overflowing the screen.
    val maxMenuHeight = (LocalConfiguration.current.screenHeightDp * 0.55f)
        .coerceIn(240f, 420f)
        .dp

    val chipLabel = when {
        selectedIds.isEmpty() -> emptyLabel
        selectedIds.size == 1 ->
            options.firstOrNull { it.id in selectedIds }?.name ?: emptyLabel
        else -> "$emptyLabel · ${selectedIds.size}"
    }

    Box {
        FilterChip(
            selected = hasSelection,
            onClick = {
                haptics.click()
                expanded = true
            },
            label = { Text(chipLabel) }
        )
        // Multi-select menu: toggle items without dismissing; clear all resets.
        // Extra-round bottom corners so the popup feels like an M3 cookie surface.
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = maxMenuHeight),
            scrollState = menuScroll,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 28.dp,
                bottomEnd = 28.dp
            )
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1)
            ) {
                var index = 0
                if (hasSelection) {
                    DropdownMenuItem(
                        selected = false,
                        onClick = {
                            haptics.tick()
                            onSelectionChange(emptySet())
                        },
                        text = { Text("Clear all") },
                        shapes = MenuDefaults.itemShape(index = index, count = itemCount),
                        leadingIcon = {
                            Icon(clearIcon, contentDescription = null)
                        }
                    )
                    index++
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            horizontal = MenuDefaults.HorizontalDividerPadding
                        )
                    )
                }
                options.forEach { option ->
                    val itemIndex = index
                    val isSelected = option.id in selectedIds
                    DropdownMenuItem(
                        selected = isSelected,
                        onClick = {
                            haptics.tick()
                            val next = if (isSelected) {
                                selectedIds - option.id
                            } else {
                                selectedIds + option.id
                            }
                            onSelectionChange(next)
                            // Keep menu open for multi-select
                        },
                        text = { Text(option.name) },
                        shapes = MenuDefaults.itemShape(index = itemIndex, count = itemCount),
                        leadingIcon = {
                            Icon(option.icon, contentDescription = null)
                        },
                        checkedLeadingIcon = {
                            Icon(Icons.Rounded.Check, contentDescription = null)
                        }
                    )
                    index++
                }
            }
        }
    }
}

/**
 * Credit cards as a "fuel gauge": the arc shows how much of the combined limit
 * is burned, colored green → amber → red, with available-to-spend as the
 * headline (the number you actually decide with) and per-card rows below.
 */
@Composable
private fun CreditCardPanel(
    summary: codes.tashif.paisa.data.HomeSummary,
    balancesHidden: Boolean = false
) {
    val debt = summary.creditCardDebt
    val limit = summary.creditCardLimit
    val utilization = if (limit > 0) (debt / limit).toFloat().coerceIn(0f, 1f) else null
    val gaugeColor = when {
        utilization == null -> MaterialTheme.colorScheme.primary
        utilization < 0.3f -> IncomeGreen
        utilization < 0.7f -> Color(0xFFFFB74D)
        else -> ExpenseRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Credit cards",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (limit > 0) {
                    MoneyText(
                        currency = summary.currency,
                        amount = (limit - debt).coerceAtLeast(0.0),
                        hidden = balancesHidden,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = gaugeColor,
                        maskCount = 5
                    )
                    Text(
                        text = "available to spend",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                if (balancesHidden) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Owed ",
                            style = if (limit > 0) {
                                MaterialTheme.typography.bodySmall
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                            fontWeight = if (limit > 0) FontWeight.Normal else FontWeight.Bold,
                            color = if (limit > 0) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                ExpenseRed
                            }
                        )
                        HiddenMoneyMask(count = 5, size = if (limit > 0) 7.dp else 9.dp)
                    }
                } else {
                    Text(
                        text = "Owed ${formatMoney(summary.currency, debt)}",
                        style = if (limit > 0) {
                            MaterialTheme.typography.bodySmall
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        fontWeight = if (limit > 0) FontWeight.Normal else FontWeight.Bold,
                        color = if (limit > 0) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            ExpenseRed
                        }
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                summary.creditCards.take(3).forEach { card ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = listOfNotNull(
                                card.bankName ?: card.name,
                                card.accountLast4?.let { "••$it" }
                            ).joinToString(" "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        MoneyText(
                            currency = summary.currency,
                            amount = -card.currentBalance,
                            hidden = balancesHidden,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maskCount = 4
                        )
                    }
                }
            }
            if (utilization != null) {
                CreditGauge(
                    utilization = if (balancesHidden) 0f else utilization,
                    color = gaugeColor,
                    label = if (balancesHidden) null else "${(utilization * 100).toInt()}%",
                    hideLabelAsMask = balancesHidden
                )
            }
        }
    }
}

@Composable
private fun CreditGauge(
    utilization: Float,
    color: Color,
    label: String?,
    hideLabelAsMask: Boolean = false
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        modifier = Modifier.size(92.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(92.dp)) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2f
            val arcSize = androidx.compose.ui.geometry.Size(
                size.width - stroke,
                size.height - stroke
            )
            // 270° gauge open at the bottom, like a fuel dial
            drawArc(
                color = track,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * utilization,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (hideLabelAsMask) {
                HiddenMoneyMask(count = 3, size = 8.dp)
            } else if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Text(
                text = "used",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DayHeader(
    day: String,
    currency: String,
    spent: Double,
    balancesHidden: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = MaterialTheme.spacing.small,
                bottom = MaterialTheme.spacing.extraSmall
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayLabel(day),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (spent > 0) {
            MoneyText(
                currency = currency,
                amount = spent,
                hidden = balancesHidden,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                prefix = "−",
                maskCount = 5
            )
        }
    }
}

internal fun dayLabel(isoDay: String): String {
    val parser = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = runCatching { parser.parse(isoDay) }.getOrNull() ?: return isoDay
    val today = parser.format(java.util.Date())
    val yesterday = parser.format(java.util.Date(System.currentTimeMillis() - 86_400_000L))
    return when (isoDay) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> java.text.SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(date)
    }
}

/**
 * M3 multi-shape balance cluster — distinct corner radii per tile (shape scale:
 * extra-large hero, medium cookie eye, large income, asymmetric expense),
 * matching Material You / Expressive "varied shapes" guidance from m3.material.io.
 */
@Composable
private fun BalanceCard(
    currency: String,
    totalBalance: Double,
    income: Double,
    expense: Double,
    accounts: List<codes.tashif.paisa.data.Account> = emptyList(),
    balancesHidden: Boolean = false,
    onToggleBalancesHidden: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    // Shape scale — deliberately mixed, not one uniform radius
    val heroShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 28.dp,
        bottomStart = 12.dp,
        bottomEnd = 12.dp
    )
    val eyeShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 8.dp,
        bottomStart = 8.dp,
        bottomEnd = 18.dp
    )
    val incomeShape = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 8.dp,
        bottomStart = 28.dp,
        bottomEnd = 12.dp
    )
    val expenseShape = RoundedCornerShape(
        topStart = 8.dp,
        topEnd = 12.dp,
        bottomStart = 12.dp,
        bottomEnd = 28.dp
    )
    val breakdownShape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ── Hero: total balance ──────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = heroShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = onToggleBalancesHidden,
                        modifier = Modifier.size(40.dp),
                        shape = eyeShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (balancesHidden) {
                                Icons.Rounded.VisibilityOff
                            } else {
                                Icons.Rounded.Visibility
                            },
                            contentDescription = if (balancesHidden) {
                                "Show balances"
                            } else {
                                "Hide balances"
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (expanded) {
                                    Icons.Rounded.ExpandLess
                                } else {
                                    Icons.Rounded.ExpandMore
                                },
                                contentDescription = if (expanded) {
                                    "Hide breakdown"
                                } else {
                                    "Show breakdown"
                                },
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                MoneyText(
                    currency = currency,
                    amount = totalBalance,
                    hidden = balancesHidden,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maskCount = 7
                )

                AnimatedVisibility(visible = expanded) {
                    val (creditCards, bankAccounts) = accounts.partition {
                        it.type == "Credit Card"
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MaterialTheme.spacing.small),
                        shape = breakdownShape,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.spacing.small,
                                vertical = MaterialTheme.spacing.smaller
                            )
                        ) {
                            bankAccounts.forEach { account ->
                                BreakdownRow(
                                    label = listOfNotNull(
                                        account.name,
                                        account.accountLast4?.takeIf { it != "WALLET" }
                                            ?.let { "••$it" }
                                    ).joinToString(" "),
                                    currency = currency,
                                    amount = account.currentBalance,
                                    hidden = balancesHidden
                                )
                            }
                            if (creditCards.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                                Text(
                                    text = "Credit cards (not in total)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                                creditCards.forEach { card ->
                                    BreakdownRow(
                                        label = listOfNotNull(
                                            card.name,
                                            card.accountLast4?.let { "••$it" }
                                        ).joinToString(" "),
                                        currency = currency,
                                        amount = card.currentBalance,
                                        hidden = balancesHidden
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Income + Expense companion tiles (distinct shapes) ───────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatTile(
                modifier = Modifier.weight(1f),
                shape = incomeShape,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                label = "Income",
                currency = currency,
                amount = income,
                hidden = balancesHidden,
                icon = Icons.AutoMirrored.Rounded.TrendingUp
            )
            StatTile(
                modifier = Modifier.weight(1f),
                shape = expenseShape,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                label = "Expense",
                currency = currency,
                amount = expense,
                hidden = balancesHidden,
                icon = Icons.AutoMirrored.Rounded.TrendingDown
            )
        }
    }
}

@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    label: String,
    currency: String,
    amount: Double,
    hidden: Boolean,
    icon: ImageVector
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor.copy(alpha = 0.85f)
                )
            }
            Spacer(Modifier.height(6.dp))
            MoneyText(
                currency = currency,
                amount = amount,
                hidden = hidden,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maskCount = 5
            )
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    currency: String,
    amount: Double,
    hidden: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        MoneyText(
            currency = currency,
            amount = amount,
            hidden = hidden,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary,
            maskCount = 5
        )
    }
}

@Composable
internal fun TransactionRow(
    tx: TransactionWithDetails,
    currency: String,
    balancesHidden: Boolean = false,
    onClick: () -> Unit = {}
) {
    val isIncome = tx.type == "income"
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val sign = if (isIncome) "+" else "−"
    val sourceLabel = when (tx.source) {
        "sms" -> "SMS"
        "statement" -> "Statement"
        else -> null
    }
    val haptics = rememberHaptics()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.click()
                onClick()
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryBadge(
                iconName = tx.categoryIcon,
                categoryName = tx.categoryName,
                colorHex = tx.categoryColor,
                size = 42.dp
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.merchantName?.takeIf { it.isNotBlank() }
                        ?: tx.note.takeIf { it.isNotBlank() }
                        ?: tx.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = listOfNotNull(
                        tx.categoryName,
                        tx.accountName,
                        tx.bankName?.takeIf { tx.source != "manual" },
                        sourceLabel
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MoneyText(
                currency = currency,
                amount = tx.amount,
                hidden = balancesHidden,
                style = MaterialTheme.typography.titleSmall,
                color = amountColor,
                fontWeight = FontWeight.Bold,
                prefix = sign,
                maskCount = 5
            )
        }
    }
}

/** @deprecated Prefer [formatMoneyAmount] / [MoneyText]; kept for call sites outside Home. */
internal fun formatMoney(
    currency: String,
    amount: Double,
    hidden: Boolean = false
): String {
    if (hidden) return "••••••"
    return formatMoneyAmount(currency, amount)
}
