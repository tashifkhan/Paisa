package codes.tashif.paisa.ui.format

import java.util.Locale

/** Currency symbol and amount with grouping and two decimals, like "₹1,234.50". */
fun formatMoneyAmount(currency: String, amount: Double): String {
    return String.format(Locale.getDefault(), "%s%,.2f", currency, amount)
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
