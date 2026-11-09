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

data class TxFilters(
    val type: String? = null, // income | expense
    /** Empty = any category; non-empty = match any selected (OR). */
    val categoryIds: Set<Int> = emptySet(),
    /** Empty = any account; non-empty = match any selected (OR). */
    val accountIds: Set<Int> = emptySet(),
    val source: String? = null // sms | manual | statement
) {
    val isActive: Boolean
        get() = type != null || categoryIds.isNotEmpty() || accountIds.isNotEmpty() || source != null
}
