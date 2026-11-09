package codes.tashif.paisa.ui.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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
import codes.tashif.paisa.widget.WidgetDeepLink

/** The unlocked app: tabs, nav bar, FABs, snackbars and the screens they open. */
@Composable
fun AppShell(
    viewModel: AppViewModel,
    smsViewModel: SmsViewModel = hiltViewModel(),
    debtsViewModel: DebtsViewModel = hiltViewModel()
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
            WidgetDeepLink.DEST_ADD_TRANSACTION -> state.open(AddSheet.TRANSACTION)
            WidgetDeepLink.DEST_STATEMENT_IMPORT -> state.open(FullScreen.STATEMENT_IMPORT)
            WidgetDeepLink.DEST_SMS_SETUP -> state.open(FullScreen.SMS_SETUP)
            WidgetDeepLink.DEST_ACCOUNTS -> viewModel.selectTab(AppTab.ACCOUNTS)
            WidgetDeepLink.DEST_HOME -> viewModel.selectTab(AppTab.HOME)
        }
        if (widgetDestination != null) viewModel.consumeWidgetDestination()
    }

    // Full-screen secondary destinations take priority over tabs.
    if (selectedTxId != null) {
        TransactionDetailScreen(onBack = viewModel::closeTransaction)
        return
    }
    val topScreen = state.topScreen
    if (topScreen != null) {
        FullScreenHost(
            screen = topScreen,
            state = state,
            onExternalActivityLaunched = viewModel::onExternalActivityLaunched
        )
        return
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
    BackHandler(enabled = currentTab !in navTabs) {
        viewModel.selectTab(AppTab.MORE)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            AppFab(
                currentTab = currentTab,
                scanRunning = scanProgress.isRunning,
                onRescanSms = { smsViewModel.startSmsScan() },
                onOpenSmsSetup = { state.open(FullScreen.SMS_SETUP) },
                onAdd = { sheet -> state.open(sheet) },
                debtPersonOpen = openDebtPersonId != null
            )
        },
        bottomBar = {
            AppNavBar(
                navTabs = navTabs,
                currentTab = currentTab,
                floating = settings?.floatingNavBar == true,
                showLabels = settings?.navBarLabels != false,
                onSelect = viewModel::selectTab
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_content"
            ) { page ->
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
        }
    }

    AddSheetsHost(state = state)
}
