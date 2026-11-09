package codes.tashif.paisa.ui.format

import codes.tashif.paisa.platform.formatGrouped

/** Currency symbol and amount with grouping and two decimals, like "₹1,234.50". */
fun formatMoneyAmount(currency: String, amount: Double): String {
    return currency + formatGrouped(amount, 2)
}

/** @deprecated Prefer [formatMoneyAmount] / [MoneyText]; kept for call sites outside Home. */
fun formatMoney(
    currency: String,
    amount: Double,
    hidden: Boolean = false
): String {
    if (hidden) return "••••••"
    return formatMoneyAmount(currency, amount)
}
