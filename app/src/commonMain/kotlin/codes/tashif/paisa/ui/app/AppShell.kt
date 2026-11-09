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
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import codes.tashif.paisa.data.AppTab
import codes.tashif.paisa.ui.accounts.AccountsScreen
import codes.tashif.paisa.ui.analytics.AnalysisScreen
import codes.tashif.paisa.ui.budgets.BudgetsScreen
import codes.tashif.paisa.ui.debts.DebtsScreen
import codes.tashif.paisa.ui.debts.DebtsViewModel
import codes.tashif.paisa.ui.home.RecordsScreen
import codes.tashif.paisa.ui.settings.MoreScreen
import codes.tashif.paisa.ui.sms.SmsViewModel
import codes.tashif.paisa.ui.transaction.TransactionDetailScreen

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
    val snackbarHostState = remember { SnackbarHostState() }

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
    // Wide windows get a side rail. Expanded ones also show a transaction next
    // to the list it came from instead of covering it.
    val useRail = width != WindowWidth.COMPACT
    val detailPane = width == WindowWidth.EXPANDED && selectedTxId != null

    // Full-screen secondary destinations take priority over tabs.
    if (selectedTxId != null && !detailPane) {
        CenteredContent {
            TransactionDetailScreen(onBack = viewModel::closeTransaction)
        }
        return
    }
    val topScreen = state.topScreen
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

    // Switching tabs with a detail pane open closes it, since it belongs to
    // the list that was showing.
    val selectTab: (AppTab) -> Unit = { tab ->
        if (selectedTxId != null) viewModel.closeTransaction()
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
    PlatformBackHandler(enabled = currentTab !in navTabs) {
        viewModel.selectTab(AppTab.MORE)
    }

    Row(modifier = Modifier.fillMaxSize()) {
        if (useRail) {
            AppNavRail(
                navTabs = navTabs,
                currentTab = currentTab,
                showLabels = settings?.navBarLabels != false,
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
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                // The detail pane has its own save button where the FAB sits.
                if (!detailPane) {
                    AppFab(
                        currentTab = currentTab,
                        scanRunning = scanProgress.isRunning,
                        onRescanSms = { smsViewModel.startSmsScan() },
                        onOpenSmsSetup = { state.open(FullScreen.SMS_SETUP) },
                        onAdd = { sheet -> state.open(sheet) },
                        debtPersonOpen = openDebtPersonId != null
                    )
                }
            },
            bottomBar = {
                if (!useRail) {
                    AppNavBar(
                        navTabs = navTabs,
                        currentTab = currentTab,
                        floating = settings?.floatingNavBar == true,
                        showLabels = settings?.navBarLabels != false,
                        onSelect = selectTab
                    )
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
                // Two equal panes, so on a book-style foldable the split lands
                // on the hinge.
                CenteredContent(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tab_content"
                    ) { page ->
                        TabContent(
                            page = page,
                            navTabs = navTabs,
                            state = state,
                            viewModel = viewModel
                        )
                    }
                }
                if (detailPane) {
                    VerticalDivider()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            // The row already pads for the gesture bar; the
                            // detail screen would pad for it again.
                            .consumeWindowInsets(WindowInsets.navigationBars)
                    ) {
                        TransactionDetailScreen(onBack = viewModel::closeTransaction)
                    }
                }
            }
        }
    }

    AddSheetsHost(state = state)
}

@Composable
private fun TabContent(
    page: AppTab,
    navTabs: List<AppTab>,
    state: ShellState,
    viewModel: AppViewModel
) {
    when (page) {
        AppTab.HOME -> RecordsScreen()
        AppTab.ANALYTICS -> AnalysisScreen()
        AppTab.BUDGETS -> BudgetsScreen()
        AppTab.ACCOUNTS -> AccountsScreen()
        AppTab.DEBTS -> DebtsScreen()
        AppTab.MORE -> MoreScreen(
            hiddenTabs = AppTab.entries.filter { it !in navTabs },
            onOpenTab = viewModel::selectTab,
            onOpenSmsSetup = { state.open(FullScreen.SMS_SETUP) },
            onOpenAppearance = { state.open(FullScreen.APPEARANCE) },
            onOpenAiSettings = { state.open(FullScreen.AI) },
            onOpenStatementImport = { state.open(FullScreen.STATEMENT_IMPORT) },
            onOpenMerchantMappings = { state.open(FullScreen.MERCHANT_MAPPINGS) },
            onOpenCategories = { state.open(FullScreen.CATEGORIES) },
            onOpenCurrency = { state.open(FullScreen.CURRENCY) },
            onOpenBackup = { state.open(FullScreen.BACKUP) },
            onOpenAbout = { state.open(FullScreen.ABOUT) },
            onExternalActivityLaunched = viewModel::onExternalActivityLaunched
        )
    }
}
