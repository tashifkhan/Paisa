package sh.taf.billi.ui.app

import androidx.compose.runtime.Composable
import sh.taf.billi.core.AppTab

/**
 * Where the app is, as the web build writes it in the address bar:
 *
 * - `""` is Home, `analytics`, `budgets`, `accounts`, `debts` and `more` the other tabs
 * - `settings/backup`, `settings/ai` and the rest are the full-screen settings
 * - `transactions/42` is one transaction
 *
 * Android has no address bar, so there [RouteSync] does nothing.
 */
data class AppRoute(
    val tab: AppTab = AppTab.HOME,
    val screen: FullScreen? = null,
    val transactionId: Int? = null
) {
    /** The path under the app's base, without a leading slash. */
    val path: String
        get() = when {
            transactionId != null -> "transactions/$transactionId"
            screen != null -> "settings/${screen.slug}"
            tab == AppTab.HOME -> ""
            else -> tab.id
        }

    companion object {
        /** Reads a path like `settings/backup`. Null for one Billi doesn't know. */
        fun parse(path: String): AppRoute? {
            val parts = path.trim('/').split('/').filter { it.isNotEmpty() }
            return when {
                parts.isEmpty() -> AppRoute()
                parts.size == 2 && parts[0] == "transactions" ->
                    parts[1].toIntOrNull()?.let { AppRoute(transactionId = it) }
                parts.size == 2 && parts[0] == "settings" ->
                    FullScreen.entries.firstOrNull { it.slug == parts[1] }
                        ?.let { AppRoute(tab = AppTab.MORE, screen = it) }
                parts.size == 1 -> AppTab.fromId(parts[0])?.let { AppRoute(tab = it) }
                else -> null
            }
        }
    }
}

/** The URL name of each full-screen setting. Changing one breaks saved links. */
val FullScreen.slug: String
    get() = when (this) {
        FullScreen.SMS_SETUP -> "sms-import"
        FullScreen.NOTIFICATION_IMPORT -> "payment-notifications"
        FullScreen.APPEARANCE -> "appearance"
        FullScreen.AI -> "ai"
        FullScreen.STATEMENT_IMPORT -> "import-statement"
        FullScreen.MERCHANT_MAPPINGS -> "merchant-rules"
        FullScreen.CATEGORIES -> "categories"
        FullScreen.CURRENCY -> "currency"
        FullScreen.BACKUP -> "backup"
        FullScreen.ABOUT -> "about"
    }

/**
 * Keeps the browser's address bar in step with [route], and on first load
 * opens the route the address bar asked for through [onNavigate].
 */
@Composable
expect fun RouteSync(route: AppRoute, onNavigate: (AppRoute) -> Unit)
