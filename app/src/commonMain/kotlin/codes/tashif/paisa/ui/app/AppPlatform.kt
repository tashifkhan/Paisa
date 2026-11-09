package codes.tashif.paisa.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

/** Shell services that differ per platform. */
interface AppPlatform {
    /** The app's version name, shown on About. */
    val versionName: String

    /** Redraws home screen widgets after data changes. Nothing on the web. */
    suspend fun refreshWidgets()
}

/** Where a widget tap or deep link asks the app to go. Same strings as WidgetDeepLink. */
object AppDestinations {
    const val HOME = "home"
    const val ADD_TRANSACTION = "add_transaction"
    const val STATEMENT_IMPORT = "statement_import"
    const val SMS_SETUP = "sms_setup"
    const val ACCOUNTS = "accounts"
}

/** Width of the app's window in dp, as Android's `Configuration.screenWidthDp` reports it. */
@Composable
expect fun windowWidthDp(): Int

/** The Paisa launcher icon, for About. */
@Composable
expect fun appIconPainter(): Painter
