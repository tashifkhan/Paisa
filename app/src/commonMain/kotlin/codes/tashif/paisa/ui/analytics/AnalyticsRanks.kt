package codes.tashif.paisa.ui.analytics

import codes.tashif.paisa.ui.platform.parseColor
import codes.tashif.paisa.platform.formatFixed
import androidx.compose.ui.graphics.Color
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.ui.format.formatMoney
import kotlin.math.abs
import kotlin.math.roundToInt

// Distinct fallback colors for categories whose stored color fails to parse.
internal val fallbackChartColors = listOf(
    Color(0xFF66BB6A), Color(0xFF42A5F5), Color(0xFFFFA726), Color(0xFFAB47BC),
    Color(0xFFEC407A), Color(0xFF26C6DA), Color(0xFFFFCA28), Color(0xFF8D6E63)
)

internal const val LEGEND_ROWS = 5

/** One row in a ranked list: a category, merchant or account and its total. */
internal data class RankItem(
    val group: AnalyticsGroup,
    val label: String,
    val amount: Double,
    val count: Int,
    val share: Float,
    val color: Color,
    val iconName: String? = null,
    val colorHex: String? = null,
    /** Second line, such as "Rent & Housing · 1 payment". */
    val subtitle: String? = null,
    /** A plain icon (accounts), drawn on a Material You tone picked by [tone]. */
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val tone: Int = 0
)

/** "1 payment", "3 payments". */
internal fun countLabel(count: Int, noun: String = "payment"): String =
    if (count == 1) "1 $noun" else "$count ${noun}s"

/**
 * A place's look comes from where most of its money went: its main
 * category's icon and colour, with that category named in the subtitle.
 */
internal fun placeRanks(expenses: List<TransactionWithDetails>, totalExpense: Double): List<RankItem> =
    expenses.groupBy { merchantKey(it) }
        .map { (name, list) ->
            val sum = list.sumOf { it.amount }
            val main = list.groupBy { it.categoryName }.maxByOrNull { (_, l) -> l.sumOf { it.amount } }!!.value.first()
            RankItem(
                group = AnalyticsGroup(AnalyticsGroup.Kind.MERCHANT, name),
                label = name,
                amount = sum,
                count = list.size,
                share = if (totalExpense > 0) (sum / totalExpense).toFloat() else 0f,
                color = rankColor(main.categoryColor) ?: Color.Unspecified,
                iconName = main.categoryIcon,
                colorHex = main.categoryColor,
                subtitle = "${main.categoryName} · ${countLabel(list.size)}"
            )
        }
        .sortedByDescending { it.amount }

/** Accounts ranked by what they paid, each with its own icon. */
internal fun accountRanks(expenses: List<TransactionWithDetails>, totalExpense: Double): List<RankItem> =
    expenses.groupBy { it.accountId }
        .map { (id, list) ->
            val sum = list.sumOf { it.amount }
            val first = list.first()
            RankItem(
                group = AnalyticsGroup(AnalyticsGroup.Kind.ACCOUNT, id.toString()),
                label = first.accountName,
                amount = sum,
                count = list.size,
                share = if (totalExpense > 0) (sum / totalExpense).toFloat() else 0f,
                color = Color.Unspecified,
                icon = codes.tashif.paisa.ui.components.AccountVisuals.icon(null, first.accountIcon),
                subtitle = countLabel(list.size)
            )
        }
        .sortedByDescending { it.amount }
        .mapIndexed { index, item -> item.copy(tone = index) }

internal fun merchantKey(tx: TransactionWithDetails): String =
    tx.merchantName?.trim()?.takeIf { it.isNotEmpty() }
        ?: tx.note.trim().takeIf { it.isNotEmpty() }
        ?: "Unknown"

internal fun AnalyticsGroup.matches(tx: TransactionWithDetails): Boolean = when (kind) {
    AnalyticsGroup.Kind.CATEGORY -> tx.categoryName.equals(key, ignoreCase = true)
    AnalyticsGroup.Kind.MERCHANT -> merchantKey(tx) == key
    AnalyticsGroup.Kind.ACCOUNT -> tx.accountId.toString() == key
}

private fun rankColor(hex: String?): Color? =
    hex?.let { runCatching { parseColor(it) }.getOrNull() }

internal fun categoryRanks(txs: List<TransactionWithDetails>): List<RankItem> {
    val total = txs.sumOf { it.amount }
    return txs.groupBy { it.categoryName }
        .toList()
        .sortedByDescending { (_, list) -> list.sumOf { it.amount } }
        .mapIndexed { index, (name, list) ->
            val first = list.first()
            val sum = list.sumOf { it.amount }
            RankItem(
                group = AnalyticsGroup(AnalyticsGroup.Kind.CATEGORY, name),
                label = name,
                amount = sum,
                count = list.size,
                share = if (total > 0) (sum / total).toFloat() else 0f,
                color = rankColor(first.categoryColor) ?: fallbackChartColors[index % fallbackChartColors.size],
                iconName = first.categoryIcon,
                colorHex = first.categoryColor,
                subtitle = countLabel(list.size, "transaction")
            )
        }
}

internal fun signedPercent(now: Double, before: Double): String? {
    if (before <= 0.0) return null
    val pct = ((now - before) / before * 100).roundToInt()
    if (pct >= 1000) return "▲ ${(now / before).roundToInt()}×"
    return when {
        pct > 0 -> "▲ $pct%"
        pct < 0 -> "▼ ${abs(pct)}%"
        else -> "Same"
    }
}

/** Short amounts for the small summary cards: ₹20.42L, ₹1.2Cr. Under a lakh stays exact. */
internal fun compactMoney(currency: String, amount: Double): String {
    val a = abs(amount)
    val sign = if (amount < 0) "-" else ""
    return when {
        a >= 1e7 -> "$sign$currency${formatFixed(a / 1e7, 2)}Cr"
        a >= 1e5 -> "$sign$currency${formatFixed(a / 1e5, 2)}L"
        else -> formatMoney(currency, amount)
    }
}
