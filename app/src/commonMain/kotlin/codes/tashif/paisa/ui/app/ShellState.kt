package codes.tashif.paisa.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember

/** Secondary screens that cover the tabs. Earlier entries win when several are open. */
enum class FullScreen(val title: String) {
    SMS_SETUP("SMS import"),
    APPEARANCE("Appearance"),
    AI("AI"),
    STATEMENT_IMPORT("Import statement"),
    MERCHANT_MAPPINGS("Merchant rules"),
    CATEGORIES("Categories"),
    CURRENCY("Currency"),
    BACKUP("Backup"),
    ABOUT("About")
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

    /** Opens [screen] alone, for the side pane where only one fits at a time. */
    fun show(screen: FullScreen) {
        FullScreen.entries.forEach { openScreens.getValue(it).value = it == screen }
    }

    /** True while Home's list is in multi-select; the FABs step aside for its toolbar. */
    var selectionMode by mutableStateOf(false)

    /** Set when a shortcut asks Home's search box to take focus, until it does. */
    var searchFocusPending by mutableStateOf(false)
        private set

    fun requestSearchFocus() {
        searchFocusPending = true
    }

    fun searchFocused() {
        searchFocusPending = false
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

/** The shell's state, for screens that react to shortcuts. Null outside the shell. */
val LocalShellState = staticCompositionLocalOf<ShellState?> { null }
