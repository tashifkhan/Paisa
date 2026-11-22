package sh.taf.billi.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

/** Shell services that differ per platform. */
interface AppPlatform {
    /** The app's version name, shown on About. */
    val versionName: String

    /** Redraws home screen widgets after data changes. Nothing on the web. */
    suspend fun refreshWidgets()

    /**
     * Follows the "Use AI" switch outside the app, in the share sheet and
     * home screen widgets. Nothing on the web.
     */
    suspend fun applyAiEnabled(enabled: Boolean) = Unit
}

/** Where a widget tap or deep link asks the app to go. Same strings as WidgetDeepLink. */
object AppDestinations {
    const val HOME = "home"
    const val ADD_TRANSACTION = "add_transaction"
    /** The add sheet, open on its "With AI" side. */
    const val ADD_WITH_AI = "add_with_ai"
    const val BUDGETS = "budgets"
    const val ANALYTICS = "analytics"
    const val STATEMENT_IMPORT = "statement_import"
    const val SMS_SETUP = "sms_setup"
    const val ACCOUNTS = "accounts"
    const val AI_SETTINGS = "ai_settings"
    const val BACKUP = "backup"
}

/** Width of the app's window in dp, as Android's `Configuration.screenWidthDp` reports it. */
@Composable
expect fun windowWidthDp(): Int

/** The Billi launcher icon, for About. */
@Composable
expect fun appIconPainter(): Painter
