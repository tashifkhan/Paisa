package codes.tashif.paisa.ui.analytics

import codes.tashif.paisa.ui.platform.PlatformBackHandler
import codes.tashif.paisa.platform.nowMillis
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.platform.VerticalScrollbarFor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import codes.tashif.paisa.ui.components.EmptyState
import codes.tashif.paisa.ui.components.PaisaTopBar
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.theme.spacing

@Composable
fun AnalysisScreen(viewModel: AnalyticsViewModel = koinViewModel()) {
    val transactions by viewModel.transactions.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val periodKind by viewModel.analyticsPeriod.collectAsState()
    val offset by viewModel.analyticsOffset.collectAsState()
    val openGroup by viewModel.analyticsGroup.collectAsState()
    val balancesHidden by viewModel.balancesHidden.collectAsState()
    val currency = settings?.currency ?: "₹"
    val money: (Double) -> String = { formatMoney(currency, it) }

    val custom by viewModel.analyticsCustom.collectAsState()
    // Ticks every minute and on new transactions, so "so far" figures keep up with the clock.
    var nowMinute by remember { mutableLongStateOf(nowMillis() / 60_000L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L - nowMillis() % 60_000L)
            nowMinute = nowMillis() / 60_000L
        }
    }
    val period = remember(periodKind, offset, custom, nowMinute, transactions) {
        Period(periodKind, offset, custom)
    }
    val groupAllTime by viewModel.groupAllTime.collectAsState()

    openGroup?.let { group ->
        PlatformBackHandler { viewModel.openAnalyticsGroup(null) }
        AnalyticsGroupScreen(
            group = group,
            transactions = transactions,
            period = period,
            allTime = groupAllTime,
            onAllTimeChange = viewModel::setGroupAllTime,
            currency = currency,
            balancesHidden = balancesHidden,
            onBack = { viewModel.openAnalyticsGroup(null) },
            onOpenTransaction = { viewModel.openTransaction(it) }
        )
        return
    }

    val periodTx = remember(transactions, period) {
        transactions.filter { period.contains(it.transactionDate) }
    }
    val prevTx = remember(transactions, period) {
        transactions.filter { period.inPrevious(it.transactionDate) }
    }
    val expenses = periodTx.filter { it.type == "expense" }
    val incomes = periodTx.filter { it.type == "income" }
    val totalExpense = expenses.sumOf { it.amount }
    val totalIncome = incomes.sumOf { it.amount }
    val prevExpense = prevTx.filter { it.type == "expense" }.sumOf { it.amount }
    val prevIncome = prevTx.filter { it.type == "income" }.sumOf { it.amount }

    var breakdownIncome by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<Pair<String, List<RankItem>>?>(null) }
    val open: (AnalyticsGroup) -> Unit = {
        sheet = null
        viewModel.openAnalyticsGroup(it)
    }
    val seeAll: (String, List<RankItem>) -> Unit = { title, ranks -> sheet = title to ranks }

    Column(modifier = Modifier.fillMaxSize()) {
        PaisaTopBar(title = "Analytics")

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.spacing.medium),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "No analytics yet",
                    subtitle = "Add a few transactions to see spending insights"
                )
            }
            return
        }

        val header: List<@Composable () -> Unit> = listOf(
            {
                PeriodPicker(
                    kind = periodKind,
                    label = period.label,
                    canGoForward = if (periodKind == AnalyticsPeriod.CUSTOM) {
                        custom.second < nowMillis()
                    } else {
                        offset > 0
                    },
                    custom = custom,
                    onKind = viewModel::setAnalyticsPeriod,
                    onShift = viewModel::shiftAnalytics,
                    onCustom = viewModel::setAnalyticsCustomRange
                )
            },
            {
                SummaryCards(
                    periodKind = periodKind,
                    period = period,
                    currency = currency,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    prevIncome = prevIncome,
                    prevExpense = prevExpense
                )
            }
        )
        val cards: List<@Composable () -> Unit> = buildList {
            add {
                AnalyticsQuickStats(
                    period = period,
                    periodTx = periodTx,
                    expenses = expenses,
                    totalExpense = totalExpense,
                    prevExpense = prevExpense,
                    money = money,
                    onOpenTransaction = { viewModel.openTransaction(it) }
                )
            }
            add {
                ByCategoryCard(
                    breakdownIncome = breakdownIncome,
                    onBreakdownChange = { breakdownIncome = it },
                    expenses = expenses,
                    incomes = incomes,
                    totalExpense = totalExpense,
                    totalIncome = totalIncome,
                    period = period,
                    currency = currency,
                    money = money,
                    onOpen = open,
                    onSeeAll = seeAll
                )
            }
            if (expenses.isNotEmpty()) {
                add {
                    RunningTotalCard(
                        periodKind = periodKind,
                        period = period,
                        expenses = expenses,
                        prevExpenses = prevTx.filter { it.type == "expense" },
                        money = money
                    )
                }
                add { SpendingBarsCard(period = period, expenses = expenses, money = money) }
                if (periodKind == AnalyticsPeriod.MONTH) {
                    add { SpendingHeatmapCard(period = period, expenses = expenses, money = money) }
                }
                add { DayOfWeekCard(period = period, expenses = expenses, money = money) }
                add {
                    TopPlacesCard(
                        expenses = expenses,
                        totalExpense = totalExpense,
                        currency = currency,
                        onOpen = open,
                        onSeeAll = seeAll
                    )
                }
                add {
                    ByAccountCard(
                        expenses = expenses,
                        totalExpense = totalExpense,
                        currency = currency,
                        onOpen = open
                    )
                }
            }
            add {
                IncomeVsExpenseCard(
                    periodKind = periodKind,
                    period = period,
                    transactions = transactions,
                    money = money
                )
            }
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val columns = if (maxWidth >= 760.dp) 2 else 1
            val gap = MaterialTheme.spacing.smaller
            if (columns == 1) {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = MaterialTheme.spacing.medium,
                        end = MaterialTheme.spacing.medium,
                        bottom = MaterialTheme.spacing.extraLarge
                    ),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    (header + cards).forEach { card -> item { card() } }
                }
                VerticalScrollbarFor(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
            } else {
                // A dashboard: the period and totals across the top, cards in
                // two columns under it, dealt left and right so they stay even.
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(horizontal = MaterialTheme.spacing.medium)
                        .padding(bottom = MaterialTheme.spacing.extraLarge),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    header.forEach { it() }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(columns) { col ->
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(gap)
                            ) {
                                cards.filterIndexed { i, _ -> i % columns == col }.forEach { it() }
                            }
                        }
                    }
                }
                VerticalScrollbarFor(scroll, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
            }
        }
    }

    sheet?.let { (title, ranks) ->
        SeeAllSheet(
            title = title,
            ranks = ranks,
            periodLabel = period.label,
            currency = currency,
            onDismiss = { sheet = null },
            onOpen = open
        )
    }
}
