package codes.tashif.paisa

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import codes.tashif.paisa.data.PaisaViewModel
import codes.tashif.paisa.security.BiometricAuth
import codes.tashif.paisa.ui.screens.AboutScreen
import codes.tashif.paisa.ui.screens.AccountsScreen
import codes.tashif.paisa.ui.screens.AddAccountSheetHost
import codes.tashif.paisa.ui.screens.AddBudgetSheet
import codes.tashif.paisa.ui.screens.AddTransactionSheet
import codes.tashif.paisa.ui.screens.AiSettingsScreen
import codes.tashif.paisa.ui.screens.AnalysisScreen
import codes.tashif.paisa.ui.screens.AppLockScreen
import codes.tashif.paisa.ui.screens.AppearanceScreen
import codes.tashif.paisa.ui.screens.BudgetsScreen
import codes.tashif.paisa.ui.screens.CategoriesScreen
import codes.tashif.paisa.ui.screens.CurrencyScreen
import codes.tashif.paisa.ui.screens.MerchantMappingsScreen
import codes.tashif.paisa.ui.screens.MoreScreen
import codes.tashif.paisa.ui.screens.OnboardingScreen
import codes.tashif.paisa.ui.screens.RecordsScreen
import codes.tashif.paisa.ui.screens.SmsSetupScreen
import codes.tashif.paisa.ui.screens.StatementImportScreen
import codes.tashif.paisa.ui.screens.TransactionDetailScreen
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.PaisaPalette
import codes.tashif.paisa.ui.theme.PaisaTheme
import codes.tashif.paisa.widget.WidgetDeepLink
import codes.tashif.paisa.ui.theme.spacing

class MainActivity : FragmentActivity() {

    private val viewModel: PaisaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleWidgetIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsState()
            val isLocked by viewModel.isLocked.collectAsState()
            val isDark = when (settings?.themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            PaisaTheme(
                darkTheme = isDark,
                palette = PaisaPalette.fromId(settings?.colorPalette),
                expressive = settings?.expressiveUi ?: true,
                amoledDark = settings?.amoledDark ?: false,
                highContrast = settings?.highContrast ?: false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Wait for settings so we don't flash main content before onboarding.
                    if (settings == null) {
                        Box(Modifier.fillMaxSize())
                        return@Surface
                    }

                    val needsOnboarding = !settings!!.onboardingCompleted
                    val appLockOn = settings!!.biometricEnabled
                    when {
                        needsOnboarding -> {
                            OnboardingScreen(
                                onFinished = { viewModel.completeOnboarding() }
                            )
                        }
                        appLockOn && isLocked -> {
                            var lockError by remember { mutableStateOf<String?>(null) }
                            val biometricAvailable = remember {
                                BiometricAuth.isAvailable(this@MainActivity)
                            }
                            AppLockScreen(
                                errorMessage = lockError,
                                biometricAvailable = biometricAvailable,
                                onUnlockClick = {
                                    BiometricAuth.authenticate(
                                        activity = this@MainActivity,
                                        onSuccess = {
                                            lockError = null
                                            viewModel.unlockApp()
                                        },
                                        onError = { lockError = it },
                                        onFailed = {
                                            lockError = "Authentication failed — try again"
                                        }
                                    )
                                }
                            )
                        }
                        else -> MainAppContent(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // launchMode is singleTop, so a widget tap on an already-running app arrives
    // here rather than through onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        viewModel.requestWidgetDestination(
            intent?.getStringExtra(WidgetDeepLink.EXTRA_DESTINATION)
        )
    }

    override fun onStart() {
        super.onStart()
        // Applies a lock deferred for a picker/export detour that ran too long.
        viewModel.onReturnToForeground()
    }

    override fun onStop() {
        super.onStop()
        // Lock when leaving the app so the next resume requires biometrics.
        // No-op while an in-app picker or export sheet is on screen.
        viewModel.lockApp()
    }
}

private data class TabItem(
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    TabItem("Home", Icons.Rounded.Home),
    TabItem("Analytics", Icons.Rounded.BarChart),
    TabItem("Budgets", Icons.Rounded.DonutLarge),
    TabItem("Accounts", Icons.Rounded.AccountBalance),
    TabItem("More", Icons.Rounded.MoreHoriz)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainAppContent(viewModel: PaisaViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedTxId by viewModel.selectedTransactionId.collectAsState()
    val scanProgress by viewModel.smsScanProgress.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var showAddBudget by remember { mutableStateOf(false) }
    var showAddAccount by remember { mutableStateOf(false) }
    var showSmsSetup by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }
    var showAiSettings by remember { mutableStateOf(false) }
    var showStatementImport by remember { mutableStateOf(false) }
    var showMerchantMappings by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }
    var showCurrency by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = rememberHaptics()

    // Widget deep links open the matching destination once, then clear so a
    // config change or back press doesn't reopen it.
    val widgetDestination by viewModel.pendingWidgetDestination.collectAsState()
    LaunchedEffect(widgetDestination) {
        when (widgetDestination) {
            WidgetDeepLink.DEST_ADD_TRANSACTION -> showAddSheet = true
            WidgetDeepLink.DEST_STATEMENT_IMPORT -> showStatementImport = true
            WidgetDeepLink.DEST_SMS_SETUP -> showSmsSetup = true
            WidgetDeepLink.DEST_ACCOUNTS -> viewModel.selectTab(3)
            WidgetDeepLink.DEST_HOME -> viewModel.selectTab(0)
        }
        if (widgetDestination != null) viewModel.consumeWidgetDestination()
    }

    // Full-screen secondary destinations (take priority over tabs)
    when {
        selectedTxId != null -> {
            TransactionDetailScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeTransaction() }
            )
            return
        }
        showSmsSetup -> {
            SmsSetupScreen(
                viewModel = viewModel,
                onBack = { showSmsSetup = false }
            )
            return
        }
        showAppearance -> {
            AppearanceScreen(
                viewModel = viewModel,
                onBack = { showAppearance = false }
            )
            return
        }
        showAiSettings -> {
            AiSettingsScreen(
                viewModel = viewModel,
                onBack = { showAiSettings = false }
            )
            return
        }
        showStatementImport -> {
            StatementImportScreen(
                viewModel = viewModel,
                onBack = { showStatementImport = false },
                onOpenAiSettings = {
                    showStatementImport = false
                    showAiSettings = true
                }
            )
            return
        }
        showMerchantMappings -> {
            MerchantMappingsScreen(
                viewModel = viewModel,
                onBack = { showMerchantMappings = false }
            )
            return
        }
        showCategories -> {
            CategoriesScreen(
                viewModel = viewModel,
                onBack = { showCategories = false }
            )
            return
        }
        showCurrency -> {
            CurrencyScreen(
                viewModel = viewModel,
                onBack = { showCurrency = false }
            )
            return
        }
        showAbout -> {
            AboutScreen(
                onBack = { showAbout = false },
                onReplayOnboarding = {
                    showAbout = false
                    viewModel.resetOnboarding()
                }
            )
            return
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { event ->
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            AnimatedVisibility(
                visible = currentTab == 0 || currentTab == 2 || currentTab == 3,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    if (currentTab == 0) {
                        SmallFloatingActionButton(
                            onClick = {
                                if (!scanProgress.isRunning) {
                                    haptics.click()
                                    viewModel.startSmsScan()
                                }
                            }
                        ) {
                            if (scanProgress.isRunning) {
                                LoadingIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                Icon(
                                    Icons.Rounded.Sync,
                                    contentDescription = "Rescan SMS"
                                )
                            }
                        }
                    }
                    FloatingActionButton(
                        onClick = {
                            haptics.click()
                            when (currentTab) {
                                2 -> showAddBudget = true
                                3 -> showAddAccount = true
                                else -> showAddSheet = true
                            }
                        }
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = when (currentTab) {
                                2 -> "Add budget"
                                3 -> "Add account"
                                else -> "Add transaction"
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = {
                            if (currentTab != index) haptics.tick()
                            viewModel.selectTab(index)
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "tab_content"
            ) { page ->
                when (page) {
                    0 -> RecordsScreen(viewModel)
                    1 -> AnalysisScreen(viewModel)
                    2 -> BudgetsScreen(viewModel)
                    3 -> AccountsScreen(viewModel)
                    else -> MoreScreen(
                        viewModel = viewModel,
                        onOpenSmsSetup = { showSmsSetup = true },
                        onOpenAppearance = { showAppearance = true },
                        onOpenAiSettings = { showAiSettings = true },
                        onOpenStatementImport = { showStatementImport = true },
                        onOpenMerchantMappings = { showMerchantMappings = true },
                        onOpenCategories = { showCategories = true },
                        onOpenCurrency = { showCurrency = true },
                        onOpenAbout = { showAbout = true }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddTransactionSheet(
            viewModel = viewModel,
            onDismiss = { showAddSheet = false }
        )
    }
    if (showAddBudget) {
        AddBudgetSheet(
            viewModel = viewModel,
            onDismiss = { showAddBudget = false }
        )
    }
    if (showAddAccount) {
        AddAccountSheetHost(
            viewModel = viewModel,
            onDismiss = { showAddAccount = false }
        )
    }
}
