package codes.tashif.paisa.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/** Secondary screens that cover the tabs. Earlier entries win when several are open. */
enum class FullScreen {
    SMS_SETUP, APPEARANCE, AI, STATEMENT_IMPORT, MERCHANT_MAPPINGS,
    CATEGORIES, CURRENCY, BACKUP, ABOUT
}

/** The bottom sheets the FAB and widgets open over the tabs. */
enum class AddSheet { TRANSACTION, BUDGET, ACCOUNT, DEBT_PERSON }

/**
 * Which full-screen destinations and sheets are open. Each is a separate flag,
 * as before, so opening one while another is up keeps the old priority order.
 */
@Stable
class ShellState {
    private val openScreens = FullScreen.entries.associateWith { mutableStateOf(false) }
    private val openSheets = AddSheet.entries.associateWith { mutableStateOf(false) }

    /** The full-screen destination on top, or null for the tabs. */
    val topScreen: FullScreen?
        get() = FullScreen.entries.firstOrNull { openScreens.getValue(it).value }

    fun open(screen: FullScreen) {
        openScreens.getValue(screen).value = true
    }

    fun close(screen: FullScreen) {
        openScreens.getValue(screen).value = false
    }

    fun isOpen(sheet: AddSheet): Boolean = openSheets.getValue(sheet).value

    fun open(sheet: AddSheet) {
        openSheets.getValue(sheet).value = true
    }

    fun close(sheet: AddSheet) {
        openSheets.getValue(sheet).value = false
    }
}

@Composable
fun rememberShellState(): ShellState = remember { ShellState() }
