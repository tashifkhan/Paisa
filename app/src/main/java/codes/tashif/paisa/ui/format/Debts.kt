package codes.tashif.paisa.ui.format

import codes.tashif.paisa.data.DebtPersonBalance

/** Below this, a balance counts as settled (floating point dust). */
const val DEBT_SETTLED = 0.005

/** "Owes you", "You owe" or "Settled" for a balance. */
fun debtStatus(balance: Double): String = when {
    balance >= DEBT_SETTLED -> "Owes you"
    balance <= -DEBT_SETTLED -> "You owe"
    else -> "Settled"
}

/** Totals across everyone: what's owed to you, and what you owe (both positive). */
fun debtTotals(people: List<DebtPersonBalance>): Pair<Double, Double> =
    people.sumOf { it.balance.coerceAtLeast(0.0) } to people.sumOf { (-it.balance).coerceAtLeast(0.0) }
