package codes.tashif.paisa.ui.home

import codes.tashif.paisa.data.Account

data class HomeSummary(
    /** Sum across all non-credit-card accounts. */
    val totalBalance: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val currency: String = "₹",
    /** Credit card outstanding (positive number = amount owed). */
    val creditCardDebt: Double = 0.0,
    /** Combined limit across credit cards, 0 when unknown. */
    val creditCardLimit: Double = 0.0,
    val creditCards: List<Account> = emptyList()
)

/** Date windows relative to today. Bounds are worked out when the filter runs, so "This month" stays current. */
enum class DatePreset(val label: String) {
    TODAY("Today"),
    LAST_7_DAYS("Last 7 days"),
    THIS_MONTH("This month"),
    LAST_MONTH("Last month"),
    LAST_3_MONTHS("Last 3 months"),
    THIS_YEAR("This year")
}

/** Where a transaction came from, as stored in `transactions.source`. */
enum class TxSource(val id: String, val label: String) {
    SMS("sms", "SMS"),
    MANUAL("manual", "Manual"),
    STATEMENT("statement", "Statement")
}

data class TxFilters(
    val type: String? = null, // income | expense
    /** Empty = any category; non-empty = match any selected (OR). */
    val categoryIds: Set<Int> = emptySet(),
    /** Empty = any account; non-empty = match any selected (OR). */
    val accountIds: Set<Int> = emptySet(),
    /** Empty = any source; non-empty = match any of these `source` ids (OR). */
    val sources: Set<String> = emptySet(),
    val datePreset: DatePreset? = null,
    /** A custom range, `yyyy-MM-dd`, both ends inclusive. Used when [datePreset] is null. */
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null
) {
    val hasDate: Boolean get() = datePreset != null || dateFrom != null
    val hasAmount: Boolean get() = minAmount != null || maxAmount != null

    /** How many filter groups are on, for the badge on the Filters chip. */
    val activeCount: Int
        get() = listOf(
            type != null, categoryIds.isNotEmpty(), accountIds.isNotEmpty(),
            sources.isNotEmpty(), hasDate, hasAmount
        ).count { it }

    val isActive: Boolean get() = activeCount > 0

    fun withoutDate() = copy(datePreset = null, dateFrom = null, dateTo = null)
    fun withoutAmount() = copy(minAmount = null, maxAmount = null)
}
