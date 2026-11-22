package sh.taf.billi.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.ui.graphics.vector.ImageVector
import sh.taf.billi.platform.formatGrouped

/** The filter groups, in the order the chips and the sheet show them. */
internal enum class FilterSection(val title: String, val icon: ImageVector) {
    DATE("Date", Icons.Rounded.CalendarMonth),
    TYPE("Type", Icons.Rounded.SwapVert),
    CATEGORY("Category", Icons.Rounded.Category),
    ACCOUNT("Account", Icons.Rounded.AccountBalanceWallet),
    AMOUNT("Amount", Icons.Rounded.Payments),
    SOURCE("Source", Icons.Rounded.Inbox)
}

internal val TxSource.icon: ImageVector
    get() = when (this) {
        TxSource.SMS -> Icons.Rounded.Sms
        TxSource.NOTIFICATION -> Icons.Rounded.Notifications
        TxSource.MANUAL -> Icons.Rounded.EditNote
        TxSource.STATEMENT -> Icons.Rounded.Description
        TxSource.ASSISTANT -> Icons.Rounded.AutoAwesome
    }

internal fun typeIcon(type: String?): ImageVector = when (type) {
    "income" -> Icons.AutoMirrored.Rounded.TrendingUp
    "expense" -> Icons.AutoMirrored.Rounded.TrendingDown
    else -> Icons.Rounded.SwapVert
}

/** What a section narrows the list to, in a few words, or null when it's off. */
internal fun FilterSection.summary(
    f: TxFilters,
    currency: String,
    categories: List<FilterMenuOption>,
    accounts: List<FilterMenuOption>
): String? = when (this) {
    FilterSection.DATE -> when {
        f.datePreset != null -> f.datePreset.label
        f.dateFrom != null -> customRangeLabel(f.dateFrom, f.dateTo)
        else -> null
    }
    FilterSection.TYPE -> f.type?.let { if (it == "income") "Income" else "Expenses" }
    FilterSection.CATEGORY -> pickedNames(f.categoryIds, categories)
    FilterSection.ACCOUNT -> pickedNames(f.accountIds, accounts)
    FilterSection.AMOUNT -> amountLabel(f.minAmount, f.maxAmount, currency)
    FilterSection.SOURCE -> f.sources.takeIf { it.isNotEmpty() }?.let { ids ->
        TxSource.entries.filter { it.id in ids }.joinToString(", ") { it.label }
    }
}

/** Turns the section off. */
internal fun FilterSection.clear(f: TxFilters): TxFilters = when (this) {
    FilterSection.DATE -> f.withoutDate()
    FilterSection.TYPE -> f.copy(type = null)
    FilterSection.CATEGORY -> f.copy(categoryIds = emptySet())
    FilterSection.ACCOUNT -> f.copy(accountIds = emptySet())
    FilterSection.AMOUNT -> f.withoutAmount()
    FilterSection.SOURCE -> f.copy(sources = emptySet())
}

/** "Food & Dining", or "Food & Dining +2" when more are picked. */
private fun pickedNames(ids: Set<Int>, options: List<FilterMenuOption>): String? {
    if (ids.isEmpty()) return null
    val first = options.firstOrNull { it.id in ids }?.name ?: return "${ids.size} selected"
    return if (ids.size == 1) first else "$first +${ids.size - 1}"
}

internal fun amountLabel(min: Double?, max: Double?, currency: String): String? {
    val lo = min?.let { shortMoney(currency, it) }
    val hi = max?.let { shortMoney(currency, it) }
    return when {
        lo != null && hi != null -> "$lo to $hi"
        lo != null -> "$lo or more"
        hi != null -> "Up to $hi"
        else -> null
    }
}

/** "₹500", or "₹99.50" when there are paise. */
internal fun shortMoney(currency: String, v: Double): String =
    currency + formatGrouped(v, if (v % 1.0 == 0.0) 0 else 2)

/** One-tap amount bands. The bounds are inclusive at the bottom, as the filter is. */
internal val AMOUNT_BANDS: List<Pair<Double?, Double?>> = listOf(
    null to 500.0,
    500.0 to 2_000.0,
    2_000.0 to 10_000.0,
    10_000.0 to null
)
