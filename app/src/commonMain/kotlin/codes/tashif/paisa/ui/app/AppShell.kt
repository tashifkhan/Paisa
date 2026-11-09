package codes.tashif.paisa.ui.app

import codes.tashif.paisa.ui.platform.PlatformBackHandler
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.AppTab
import codes.tashif.paisa.ui.accounts.AccountsScreen
import codes.tashif.paisa.ui.analytics.AnalysisScreen
import codes.tashif.paisa.ui.budgets.BudgetsScreen
import codes.tashif.paisa.ui.debts.DebtsScreen
import codes.tashif.paisa.ui.debts.DebtsViewModel
import codes.tashif.paisa.ui.home.HomeSummaryPane
import codes.tashif.paisa.ui.home.RecordsScreen
import codes.tashif.paisa.ui.platform.PageKeys
import codes.tashif.paisa.ui.platform.isWebPlatform
import codes.tashif.paisa.ui.platform.setWindowTitle
import codes.tashif.paisa.ui.settings.MoreScreen
import codes.tashif.paisa.ui.sms.SmsViewModel
import codes.tashif.paisa.ui.transaction.TransactionDetailScreen
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf

/** The unlocked app: tabs, nav bar, FABs, snackbars and the screens they open. */
@Composable
fun AppShell(
    viewModel: AppViewModel,
    smsViewModel: SmsViewModel = koinViewModel(),
    debtsViewModel: DebtsViewModel = koinViewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedTxId by viewModel.selectedTransactionId.collectAsState()
    val scanProgress by smsViewModel.smsScanProgress.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val navTabs by viewModel.navTabs.collectAsState()
    val openDebtPersonId by debtsViewModel.openDebtPersonId.collectAsState()
    val state = rememberShellState()
    // Each tab keeps its saved state (scroll positions, pager pages) while
    // it's off screen: on a phone a transaction or setting replaces the tabs,
    // and coming back shouldn't jump the list to the top.
    val tabStates = rememberSaveableStateHolder()
    val snackbarHostState = remember { SnackbarHostState() }
    var showShortcuts by remember { mutableStateOf(false) }

    // Widget deep links open the matching destination once, then clear so a
    // config change or back press doesn't reopen it.
    val widgetDestination by viewModel.pendingWidgetDestination.collectAsState()
    LaunchedEffect(widgetDestination) {
        when (widgetDestination) {
            AppDestinations.ADD_TRANSACTION -> state.open(AddSheet.TRANSACTION)
            AppDestinations.STATEMENT_IMPORT -> state.open(FullScreen.STATEMENT_IMPORT)
            AppDestinations.SMS_SETUP -> state.open(FullScreen.SMS_SETUP)
            AppDestinations.ACCOUNTS -> viewModel.selectTab(AppTab.ACCOUNTS)
            AppDestinations.HOME -> viewModel.selectTab(AppTab.HOME)
        }
        if (widgetDestination != null) viewModel.consumeWidgetDestination()
    }

    val width = windowWidth()
    // Wide windows get a side rail, and laptops and desktops a full drawer.
    // From expanded up the navigation never goes away: transactions and
    // settings open in a pane next to the list they came from.
    val useRail = width != WindowWidth.COMPACT
    // In a browser the add and rescan buttons sit at the top of the rail or
    // drawer, near the mouse. On Android, tablets included, they stay
    // floating at the bottom corner, within reach of a thumb.
    val navButtons = useRail && isWebPlatform
    val wide = width >= WindowWidth.EXPANDED
    val large = width == WindowWidth.LARGE
    val detailPane = wide && selectedTxId != null
    val topScreen = state.topScreen

    LaunchedEffect(currentTab, topScreen) {
        setWindowTitle(
            listOfNotNull(topScreen?.title ?: currentTab.label, "Paisa").joinToString(" · ")
        )
    }

    // Narrower windows: secondary destinations cover the tabs, as on a phone.
    if (!wide) {
        if (selectedTxId != null) {
            CenteredContent {
                TransactionDetailScreen(onBack = viewModel::closeTransaction)
            }
            return
        }
        if (topScreen != null) {
            CenteredContent {
                FullScreenHost(
                    screen = topScreen,
                    state = state,
                    onExternalActivityLaunched = viewModel::onExternalActivityLaunched
                )
            }
            return
        }
    }

    // Switching tabs with a detail pane or setting open closes it, since it
    // belongs to the list that was showing.
    val selectTab: (AppTab) -> Unit = { tab ->
        if (selectedTxId != null) viewModel.closeTransaction()
        if (wide && tab != currentTab) topScreen?.let { state.close(it) }
        viewModel.selectTab(tab)
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                duration = if (event.actionLabel != null) {
                    SnackbarDuration.Long
                } else {
                    SnackbarDuration.Short
                }
            )
            if (result == SnackbarResult.ActionPerformed) {
                event.onAction?.invoke()
            }
        }
    }

    // A screen opened from More (its tab is hidden from the bar) goes back to More.
    PlatformBackHandler(enabled = currentTab !in navTabs && !large) {
        viewModel.selectTab(AppTab.MORE)
    }

    val navActions = NavActions(
        scanRunning = scanProgress.isRunning,
        onRescanSms = { smsViewModel.startSmsScan() },
        onOpenSmsSetup = { state.open(FullScreen.SMS_SETUP) },
        onAdd = { sheet -> state.open(sheet) },
        onShowShortcuts = { showShortcuts = true }
    )

    // Keyboard shortcuts for windows with a hardware keyboard nearby. The
    // shell holds focus whenever nothing else does, so keys reach it.
    val shellFocus = remember { FocusRequester() }
    val shortcutTabs = if (large) AppTab.entries else navTabs
    val keys = Modifier
        .onFocusChanged { focus ->
            if (!focus.hasFocus) runCatching { shellFocus.requestFocus() }
        }
        .focusRequester(shellFocus)
        // A focus target with no semantics, so TalkBack still sees each button.
        .focusTarget()
        .onKeyEvent { event ->
            handleShortcut(
                event = event,
                // Text fields consume the characters typed into them, so a
                // key only gets here when nobody is typing.
                typing = false,
                tabs = shortcutTabs,
                onTab = selectTab,
                onAdd = { state.open(addSheetFor(currentTab)) },
                onSearch = {
                    selectTab(AppTab.HOME)
                    state.requestSearchFocus()
                },
                onShowShortcuts = { showShortcuts = true }
            )
        }
    if (useRail) {
        LaunchedEffect(Unit) { runCatching { shellFocus.requestFocus() } }
        PageKeys { key, command ->
            handlePageShortcut(
                key = key,
                command = command,
                tabs = shortcutTabs,
                onTab = selectTab,
                onAdd = { state.open(addSheetFor(currentTab)) },
                onSearch = {
                    selectTab(AppTab.HOME)
                    state.requestSearchFocus()
                },
                onShowShortcuts = { showShortcuts = true }
            )
        }
    }

    CompositionLocalProvider(
        LocalTabStates provides tabStates,
        LocalWindowWidth provides width,
        LocalShellState provides state
    ) {
        val focusManager = LocalFocusManager.current
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(if (useRail) keys else Modifier)
                // Clicking empty space leaves a text field, as on a desktop.
                .then(
                    if (useRail) {
                        Modifier.pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
                    } else {
                        Modifier
                    }
                )
        ) {
            when {
                large -> AppNavDrawer(
                    currentTab = currentTab,
                    actions = navActions.takeIf { navButtons },
                    onShowShortcuts = { showShortcuts = true },
                    onSelect = selectTab
                )
                useRail -> AppNavRail(
                    navTabs = navTabs,
                    currentTab = currentTab,
                    showLabels = settings?.navBarLabels != false,
                    actions = navActions.takeIf { navButtons },
                    onSelect = selectTab
                )
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                // The rail already pads for the system bars on its side.
                contentWindowInsets = if (useRail) {
                    WindowInsets.systemBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.End)
                } else {
                    WindowInsets.systemBars.union(WindowInsets.displayCutout)
                },
                // With a bottom bar the snackbar sits on top of it and the
                // FABs move up out of its way, instead of it floating over
                // the list above the FABs.
                snackbarHost = { if (useRail) SnackbarHost(hostState = snackbarHostState) },
                floatingActionButtonPosition = FabPosition.End,
                floatingActionButton = {
                    // The detail pane has its own save button where the FAB sits.
                    if (!navButtons && !detailPane) {
                        AppFab(
                            currentTab = currentTab,
                            scanRunning = scanProgress.isRunning,
                            onRescanSms = { smsViewModel.startSmsScan() },
                            onOpenSmsSetup = { state.open(FullScreen.SMS_SETUP) },
                            onAdd = { sheet -> state.open(sheet) },
                            // Side by side, the list of people is still showing.
                            debtPersonOpen = openDebtPersonId != null && !wide
                        )
                    }
                },
                bottomBar = {
                    if (!useRail) {
                        Column(Modifier.animateContentSize()) {
                            SnackbarHost(hostState = snackbarHostState)
                            AppNavBar(
                            navTabs = navTabs,
                            currentTab = currentTab,
                            floating = settings?.floatingNavBar == true,
                            showLabels = settings?.navBarLabels != false,
                            onSelect = selectTab
                        )
                        }
                    }
                }
            ) { innerPadding ->
                val layoutDirection = LocalLayoutDirection.current
                Row(
                    modifier = Modifier
                        .padding(
                            start = innerPadding.calculateStartPadding(layoutDirection),
                            end = innerPadding.calculateEndPadding(layoutDirection),
                            bottom = innerPadding.calculateBottomPadding()
                        )
                        .fillMaxSize()
                ) {
                    if (wide && (topScreen != null || currentTab == AppTab.MORE)) {
                        SettingsPanes(
                            topScreen = topScreen,
                            showMoreList = currentTab == AppTab.MORE,
                            state = state,
                            navTabs = if (large) AppTab.entries else navTabs,
                            viewModel = viewModel
                        )
                    } else {
                        // Two equal panes on expanded, so on a book-style
                        // foldable the split lands on the hinge.
                        Box(modifier = Modifier.weight(1f)) {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "tab_content"
                            ) { page ->
                                if (wide) {
                                    TabContent(page, navTabs, state, viewModel)
                                } else {
                                    CenteredContent {
                                        TabContent(page, navTabs, state, viewModel)
                                    }
                                }
                            }
                        }
                        when {
                            detailPane -> {
                                VerticalDivider()
                                Box(
                                    modifier = Modifier
                                        .then(if (large) Modifier.width(480.dp) else Modifier.weight(1f))
                                        .fillMaxHeight()
                                        // The row already pads for the gesture bar; the
                                        // detail screen would pad for it again.
                                        .consumeWindowInsets(WindowInsets.navigationBars)
                                ) {
                                    TransactionDetailScreen(onBack = viewModel::closeTransaction)
                                }
                            }
                            large && currentTab == AppTab.HOME -> {
                                HomeSummaryPane(modifier = Modifier.width(400.dp).fillMaxHeight())
                            }
                        }
                    }
                }
            }
        }

        AddSheetsHost(state = state)
        if (showShortcuts) {
            ShortcutsDialog(tabs = shortcutTabs, onDismiss = { showShortcuts = false })
        }
    }
}

/**
 * Settings on a wide window: the More list on the left and the open setting
 * on the right, the way desktop settings apps work. A setting opened from
 * elsewhere, like SMS setup from Home, fills the content area instead.
 */
@Composable
private fun SettingsPanes(
    topScreen: FullScreen?,
    showMoreList: Boolean,
    state: ShellState,
    navTabs: List<AppTab>,
    viewModel: AppViewModel
) {
    Row(Modifier.fillMaxSize()) {
        if (showMoreList) {
            Box(Modifier.width(ListPaneWidth).fillMaxHeight()) {
                TabContent(AppTab.MORE, navTabs, state, viewModel, selected = topScreen)
            }
            VerticalDivider()
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            if (topScreen != null) {
                CenteredContent {
                    FullScreenHost(
                        screen = topScreen,
                        state = state,
                        onExternalActivityLaunched = viewModel::onExternalActivityLaunched
                    )
                }
            } else {
                PanePlaceholder(
                    title = "Settings",
                    subtitle = "Pick something on the left"
                )
            }
        }
    }
}

@Composable
private fun TabContent(
    page: AppTab,
    navTabs: List<AppTab>,
    state: ShellState,
    viewModel: AppViewModel,
    selected: FullScreen? = null
) {
    val wide = LocalWindowWidth.current >= WindowWidth.EXPANDED
    // In the side-by-side layout one setting shows at a time.
    val open: (FullScreen) -> Unit = { if (wide) state.show(it) else state.open(it) }
    LocalTabStates.current.SaveableStateProvider(page.name) {
    when (page) {
        AppTab.HOME -> RecordsScreen()
        AppTab.ANALYTICS -> AnalysisScreen()
        AppTab.BUDGETS -> BudgetsScreen()
        AppTab.ACCOUNTS -> AccountsScreen()
        AppTab.DEBTS -> DebtsScreen()
        AppTab.MORE -> MoreScreen(
            hiddenTabs = AppTab.entries.filter { it !in navTabs },
            onOpenTab = viewModel::selectTab,
            onOpenSmsSetup = { open(FullScreen.SMS_SETUP) },
            onOpenAppearance = { open(FullScreen.APPEARANCE) },
            onOpenAiSettings = { open(FullScreen.AI) },
            onOpenStatementImport = { open(FullScreen.STATEMENT_IMPORT) },
            onOpenMerchantMappings = { open(FullScreen.MERCHANT_MAPPINGS) },
            onOpenCategories = { open(FullScreen.CATEGORIES) },
            onOpenCurrency = { open(FullScreen.CURRENCY) },
            onOpenBackup = { open(FullScreen.BACKUP) },
            onOpenAbout = { open(FullScreen.ABOUT) },
            onExternalActivityLaunched = viewModel::onExternalActivityLaunched,
            selected = selected?.title
        )
    }
    }
}

/** Holds each tab's saved state while the tab is off screen. Set by [AppShell]. */
private val LocalTabStates = staticCompositionLocalOf<SaveableStateHolder> {
    error("TabContent needs AppShell's SaveableStateHolder")
}
