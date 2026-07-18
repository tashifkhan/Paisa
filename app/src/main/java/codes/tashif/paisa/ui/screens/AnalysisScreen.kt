package codes.tashif.paisa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.PaisaViewModel
import codes.tashif.paisa.ui.components.BarChart
import codes.tashif.paisa.ui.components.CalendarHeatmap
import codes.tashif.paisa.ui.components.ChartEntry
import codes.tashif.paisa.ui.components.DonutChart
import codes.tashif.paisa.ui.components.EmptyState
import codes.tashif.paisa.ui.components.GroupedBarChart
import codes.tashif.paisa.ui.components.PaisaTopBar
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen
import codes.tashif.paisa.ui.theme.spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class CategorySpend(
    val name: String,
    val color: Color?,
    val total: Double,
    val share: Float
)

// Distinct fallback colors for categories whose stored color fails to parse.
private val fallbackChartColors = listOf(
    Color(0xFF66BB6A), Color(0xFF42A5F5), Color(0xFFFFA726), Color(0xFFAB47BC),
    Color(0xFFEC407A), Color(0xFF26C6DA), Color(0xFFFFCA28), Color(0xFF8D6E63)
)

@Composable
fun AnalysisScreen(viewModel: PaisaViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currency ?: "₹"

    // 0 = current month, 1 = last month, ...
    var monthOffset by remember { mutableIntStateOf(0) }
    val monthCal = remember(monthOffset) {
        Calendar.getInstance().apply { add(Calendar.MONTH, -monthOffset) }
    }
    val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(monthCal.time)
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCal.time)
    val monthShort = SimpleDateFormat("MMM", Locale.getDefault()).format(monthCal.time)

    val monthTx = transactions.filter { it.transactionDate.startsWith(monthPrefix) }
    val monthExpenses = monthTx.filter { it.type == "expense" }
    val monthIncome = monthTx.filter { it.type == "income" }.sumOf { it.amount }
    val totalExpense = monthExpenses.sumOf { it.amount }

    val breakdown = monthExpenses
        .groupBy { it.categoryName }
        .toList()
        .mapIndexed { index, (name, txs) ->
            CategorySpend(
                name = name,
                color = txs.firstOrNull()?.categoryColor?.let { hex ->
                    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
                } ?: fallbackChartColors[index % fallbackChartColors.size],
                total = txs.sumOf { it.amount },
                share = if (totalExpense > 0) {
                    (txs.sumOf { it.amount } / totalExpense).toFloat()
                } else {
                    0f
                }
            )
        }
        .sortedByDescending { it.total }

    // Daily expense totals across the selected month.
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dailyTotals = (1..daysInMonth).map { day ->
        val dayPrefix = "%s-%02d".format(monthPrefix, day)
        ChartEntry(
            label = day.toString(),
            value = monthExpenses
                .filter { it.transactionDate.startsWith(dayPrefix) }
                .sumOf { it.amount },
            color = Color.Unspecified
        )
    }

    // Income vs expense over the last 6 months (ending at selected month).
    val trendMonths = (5 downTo 0).map { back ->
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -monthOffset - back) }
        val prefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
        val label = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
        val txs = transactions.filter { it.transactionDate.startsWith(prefix) }
        Triple(
            label,
            txs.filter { it.type == "income" }.sumOf { it.amount },
            txs.filter { it.type == "expense" }.sumOf { it.amount }
        )
    }

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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                bottom = MaterialTheme.spacing.extraLarge
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
        ) {
            item {
                // Month selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val haptics = rememberHaptics()
                    IconButton(onClick = {
                        haptics.tick()
                        monthOffset++
                    }) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month")
                    }
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    IconButton(
                        onClick = {
                            haptics.tick()
                            monthOffset--
                        },
                        enabled = monthOffset > 0
                    ) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
                ) {
                    MonthTotalCard(
                        label = "Income",
                        amount = formatMoney(currency, monthIncome),
                        color = IncomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MonthTotalCard(
                        label = "Expense",
                        amount = formatMoney(currency, totalExpense),
                        color = ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )
                    MonthTotalCard(
                        label = "Saved",
                        amount = formatMoney(currency, monthIncome - totalExpense),
                        color = if (monthIncome - totalExpense >= 0) IncomeGreen else ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (breakdown.isEmpty()) {
                item {
                    Spacer(Modifier.height(MaterialTheme.spacing.medium))
                    EmptyState(
                        title = "No expenses in $monthLabel",
                        subtitle = "Spending charts will show up here"
                    )
                }
            } else {
                item {
                    ChartCard(
                        title = "Spending by category",
                        subtitle = "Tap a slice for details"
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DonutChart(
                                entries = breakdown.map {
                                    ChartEntry(it.name, it.total, it.color ?: Color.Gray)
                                },
                                centerTitle = formatMoney(currency, totalExpense),
                                centerSubtitle = "total spent",
                                formatValue = { formatMoney(currency, it) }
                            )
                            Spacer(Modifier.height(MaterialTheme.spacing.small))
                            // M3 legend chips
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                breakdown.take(6).forEach { spend ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(spend.color ?: Color.Gray)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = spend.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = formatMoney(currency, spend.total),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "${(spend.share * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (breakdown.size > 6) {
                                    Text(
                                        text = "+${breakdown.size - 6} more in list below",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    ChartCard(
                        title = "Daily spending",
                        subtitle = "Tap a bar for the day total"
                    ) {
                        BarChart(
                            entries = dailyTotals,
                            barColor = MaterialTheme.colorScheme.primary,
                            highlightEvery = 5,
                            formatValue = { formatMoney(currency, it) },
                            tooltipLabel = { "${it.label} $monthShort" }
                        )
                    }
                }

                item {
                    ChartCard(
                        title = "Spending heatmap",
                        subtitle = "Tap a day for details"
                    ) {
                        val firstDayOffset = remember(monthPrefix) {
                            (monthCal.clone() as Calendar).apply {
                                set(Calendar.DAY_OF_MONTH, 1)
                            }.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
                        }
                        CalendarHeatmap(
                            values = dailyTotals.map { it.value },
                            firstDayOffset = firstDayOffset,
                            color = MaterialTheme.colorScheme.primary,
                            formatValue = { formatMoney(currency, it) },
                            dayLabel = { day -> "$day $monthShort" }
                        )
                    }
                }
            }

            item {
                ChartCard(
                    title = "Income vs expense",
                    subtitle = "Last 6 months · tap a group"
                ) {
                    GroupedBarChart(
                        labels = trendMonths.map { it.first },
                        seriesA = trendMonths.map { it.second },
                        seriesB = trendMonths.map { it.third },
                        colorA = IncomeGreen,
                        colorB = ExpenseRed,
                        nameA = "Income",
                        nameB = "Expense",
                        formatValue = { formatMoney(currency, it) }
                    )
                    Spacer(Modifier.height(MaterialTheme.spacing.small))
                    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
                        LegendDot(color = IncomeGreen, label = "Income")
                        LegendDot(color = ExpenseRed, label = "Expense")
                    }
                }
            }

            if (breakdown.isNotEmpty()) {
                item {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = MaterialTheme.spacing.smaller)
                    )
                }
                items(count = breakdown.size) { index ->
                    CategorySpendRow(
                        spend = breakdown[index],
                        currency = currency
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            content()
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonthTotalCard(
    label: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CategorySpendRow(spend: CategorySpend, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(spend.color ?: MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.smaller))
                Text(
                    text = spend.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatMoney(currency, spend.total),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.smaller))
            LinearWavyProgressIndicator(
                progress = { spend.share },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            Text(
                text = "${(spend.share * 100).toInt()}% of monthly spending",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
