package codes.tashif.paisa.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.ui.components.EmptyState
import codes.tashif.paisa.ui.components.TransactionRow
import codes.tashif.paisa.ui.components.charts.GroupedBarChart
import codes.tashif.paisa.ui.format.dayLabel
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen
import codes.tashif.paisa.ui.theme.spacing
import java.util.Calendar
import kotlin.math.abs

/**
 * Every transaction in one category, place or account, money in and money out,
 * for the period on screen or for all time.
 */
@Composable
internal fun AnalyticsGroupScreen(
    group: AnalyticsGroup,
    transactions: List<TransactionWithDetails>,
    period: Period,
    allTime: Boolean,
    onAllTimeChange: (Boolean) -> Unit,
    currency: String,
    balancesHidden: Boolean,
    onBack: () -> Unit,
    onOpenTransaction: (Int) -> Unit
) {
    val all = remember(transactions, group) { transactions.filter { group.matches(it) } }
    val shown = if (allTime) all else all.filter { period.contains(it.transactionDate) }
    val spent = shown.filter { it.type == "expense" }
    val received = shown.filter { it.type == "income" }
    val first = all.firstOrNull()
    val title = when (group.kind) {
        AnalyticsGroup.Kind.ACCOUNT -> first?.accountName ?: "Account"
        else -> group.key
    }
    val kindLabel = when (group.kind) {
        AnalyticsGroup.Kind.CATEGORY -> "Category"
        AnalyticsGroup.Kind.MERCHANT -> "Place"
        AnalyticsGroup.Kind.ACCOUNT -> "Account"
    }
    val months = (5 downTo 0).map { back ->
        (period.lastMonth.clone() as Calendar).apply { add(Calendar.MONTH, -back) }
    }
    val trend = months.map { cal ->
        val prefix = Period.fmt("yyyy-MM", cal)
        val txs = all.filter { it.transactionDate.startsWith(prefix) }
        Triple(
            Period.fmt("MMM", cal),
            txs.filter { it.type == "income" }.sumOf { it.amount },
            txs.filter { it.type == "expense" }.sumOf { it.amount }
        )
    }
    val byDay = shown.groupBy { it.transactionDate.take(10) }.toSortedMap(compareByDescending { it })

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(4.dp))
            GroupAvatar(
                label = title,
                iconName = if (group.kind == AnalyticsGroup.Kind.CATEGORY) first?.categoryIcon else null,
                colorHex = if (group.kind == AnalyticsGroup.Kind.CATEGORY) first?.categoryColor else null,
                accent = MaterialTheme.colorScheme.primary,
                size = 40.dp
            )
            Spacer(Modifier.width(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = kindLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
                    FilterChip(selected = !allTime, onClick = { onAllTimeChange(false) }, label = { Text(period.label) })
                    FilterChip(selected = allTime, onClick = { onAllTimeChange(true) }, label = { Text("All time") })
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
                ) {
                    MonthTotalCard(
                        label = "Spent · ${spent.size}",
                        amount = formatMoney(currency, spent.sumOf { it.amount }, balancesHidden),
                        color = ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )
                    MonthTotalCard(
                        label = "Received · ${received.size}",
                        amount = formatMoney(currency, received.sumOf { it.amount }, balancesHidden),
                        color = IncomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    val net = received.sumOf { it.amount } - spent.sumOf { it.amount }
                    MonthTotalCard(
                        label = "Net",
                        amount = formatMoney(currency, net, balancesHidden),
                        color = if (net >= 0) IncomeGreen else ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (trend.any { it.second > 0 || it.third > 0 }) {
                item {
                    ChartCard(title = "Last 6 months", subtitle = "Money in and out · tap a group") {
                        GroupedBarChart(
                            labels = trend.map { it.first },
                            seriesA = trend.map { it.second },
                            seriesB = trend.map { it.third },
                            colorA = IncomeGreen,
                            colorB = ExpenseRed,
                            nameA = "In",
                            nameB = "Out",
                            formatValue = { formatMoney(currency, it) }
                        )
                        Spacer(Modifier.height(MaterialTheme.spacing.small))
                        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
                            LegendDot(color = IncomeGreen, label = "In")
                            LegendDot(color = ExpenseRed, label = "Out")
                        }
                    }
                }
            }
            if (shown.isEmpty()) {
                item {
                    Spacer(Modifier.height(MaterialTheme.spacing.large))
                    EmptyState(
                        title = "Nothing in ${period.label}",
                        subtitle = "Switch to All time to see older transactions"
                    )
                }
            }
            byDay.forEach { (day, txs) ->
                item(key = "h$day") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MaterialTheme.spacing.small, start = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dayLabel(day),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        val net = txs.sumOf { if (it.type == "income") it.amount else if (it.type == "expense") -it.amount else 0.0 }
                        Text(
                            text = (if (net >= 0) "+" else "−") + formatMoney(currency, abs(net), balancesHidden),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(txs, key = { "t${it.id}" }) { tx ->
                    TransactionRow(
                        tx = tx,
                        currency = currency,
                        balancesHidden = balancesHidden,
                        onClick = { onOpenTransaction(tx.id) }
                    )
                }
            }
        }
    }
}
