package codes.tashif.paisa.widget

import android.content.Context
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.ui.theme.PaisaPalette
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Snapshot the widgets render from. Read once per `provideGlance` pass — widgets
 * are recomposed by an explicit `updateAll`, not by observing flows.
 */
internal data class WidgetSnapshot(
    val currency: String,
    val totalBalance: Double,
    val monthIncome: Double,
    val monthExpense: Double,
    val accounts: List<Account>,
    val creditCardDebt: Double,
    val palette: PaisaPalette,
    val hideBalancesByDefault: Boolean
)

internal suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot {
    val db = AppDatabase.getDatabase(context)
    val settings = db.settingsDao().getSettingsDirect()
    val accounts = db.accountDao().getAllAccounts().first()
    val transactions = db.transactionDao().getAllTransactions().first()

    // Mirrors PaisaViewModel.homeSummary: cards are debt, not spendable balance.
    val (creditCards, bankAccounts) = accounts.partition { it.type == "Credit Card" }
    val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    val monthTx = transactions.filter { it.transactionDate.startsWith(monthPrefix) }

    return WidgetSnapshot(
        currency = settings?.currency ?: "₹",
        totalBalance = bankAccounts.sumOf { it.currentBalance },
        monthIncome = monthTx.filter { it.type == "income" }.sumOf { it.amount },
        monthExpense = monthTx.filter { it.type == "expense" }.sumOf { it.amount },
        accounts = bankAccounts,
        creditCardDebt = creditCards.sumOf { -it.currentBalance }.coerceAtLeast(0.0),
        palette = PaisaPalette.fromId(settings?.colorPalette),
        hideBalancesByDefault = settings?.hideBalancesByDefault ?: true
    )
}

/**
 * Money for a home screen widget, where horizontal room is scarce: whole rupees
 * above 4 digits, two decimals below so small balances stay exact.
 */
internal fun formatWidgetMoney(currency: String, amount: Double): String {
    val pattern = if (kotlin.math.abs(amount) >= 10_000) "%s%,.0f" else "%s%,.2f"
    return String.format(Locale.getDefault(), pattern, currency, amount)
}

/** Placeholder shown instead of an amount while balances are hidden (text fallback). */
internal const val MASKED_AMOUNT = "••••••"
