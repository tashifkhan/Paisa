package codes.tashif.paisa.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import codes.tashif.paisa.data.AppTab
import codes.tashif.paisa.ui.components.PaisaTopBar
import codes.tashif.paisa.ui.theme.spacing
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The More tab. [onExternalActivityLaunched] runs right before the export file
 * picker opens, so the app lock doesn't trip on the resulting onStop.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onOpenSmsSetup: () -> Unit = {},
    onOpenAppearance: () -> Unit = {},
    onOpenAiSettings: () -> Unit = {},
    onOpenStatementImport: () -> Unit = {},
    onOpenMerchantMappings: () -> Unit = {},
    onOpenCategories: () -> Unit = {},
    onOpenCurrency: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    /** Screens not in the bottom bar; listed here so they stay reachable. */
    hiddenTabs: List<AppTab> = emptyList(),
    onOpenTab: (AppTab) -> Unit = {},
    onExternalActivityLaunched: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val categoryCount by viewModel.categoryCount.collectAsState()
    val scan by viewModel.smsScanProgress.collectAsState()
    val unrecognizedCount by viewModel.unrecognizedSmsCount.collectAsState()
    val merchantRuleCount by viewModel.merchantRuleCount.collectAsState()
    val ai by viewModel.aiSummary.collectAsState()
    val transactionCount by viewModel.transactionCount.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showExportOptions by remember { mutableStateOf(false) }
    val exportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val stamp = remember {
        SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val outStream = context.contentResolver.openOutputStream(uri)
            if (outStream != null) {
                viewModel.exportTransactionsToCsv(outStream) { success ->
                    if (success) {
                        viewModel.showSnackbar("Exported $transactionCount transactions to CSV")
                    } else {
                        viewModel.showSnackbar("Failed to export CSV")
                    }
                }
            } else {
                viewModel.showSnackbar("Failed to open CSV destination")
            }
        } catch (e: Exception) {
            viewModel.showSnackbar("CSV export error: ${e.message}")
        }
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val outStream = context.contentResolver.openOutputStream(uri)
            if (outStream != null) {
                viewModel.exportTransactionsToPdf(outStream) { success ->
                    if (success) {
                        viewModel.showSnackbar("Exported $transactionCount transactions to PDF")
                    } else {
                        viewModel.showSnackbar("Failed to export PDF")
                    }
                }
            } else {
                viewModel.showSnackbar("Failed to open PDF destination")
            }
        } catch (e: Exception) {
            viewModel.showSnackbar("PDF export error: ${e.message}")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PaisaTopBar(title = "More")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.large)
        ) {
            if (hiddenTabs.isNotEmpty()) {
                MoreScreensSection(hiddenTabs = hiddenTabs, onOpenTab = onOpenTab)
            }

            MoreImportSection(
                scan = scan,
                unrecognizedCount = unrecognizedCount,
                ai = ai,
                onOpenSmsSetup = onOpenSmsSetup,
                onOpenStatementImport = onOpenStatementImport
            )

            MoreDataSection(
                isExporting = isExporting,
                transactionCount = transactionCount,
                onOpenBackup = onOpenBackup,
                onExport = { if (!isExporting) showExportOptions = true }
            )

            MoreIntelligenceSection(
                ai = ai,
                merchantRuleCount = merchantRuleCount,
                onOpenAiSettings = onOpenAiSettings,
                onOpenMerchantMappings = onOpenMerchantMappings
            )

            MorePreferencesSection(
                categoryCount = categoryCount,
                settings = settings,
                onOpenCategories = onOpenCategories,
                onOpenCurrency = onOpenCurrency,
                onOpenAppearance = onOpenAppearance
            )

            PrivacySecuritySection(
                hideByDefault = settings?.hideBalancesByDefault ?: true,
                biometricEnabled = settings?.biometricEnabled ?: false,
                onSetHideBalancesByDefault = viewModel::setHideBalancesByDefault,
                onSetBiometricLockEnabled = viewModel::setBiometricLockEnabled,
                onShowMessage = { viewModel.showSnackbar(it) }
            )

            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            MoreAboutSection(onOpenAbout = onOpenAbout)
        }
    }

    if (showExportOptions) {
        ExportSheet(
            transactionCount = transactionCount,
            sheetState = exportSheetState,
            onDismiss = { showExportOptions = false },
            onPickCsv = {
                scope.launch {
                    exportSheetState.hide()
                    showExportOptions = false
                    onExternalActivityLaunched()
                    csvExportLauncher.launch("paisa_transactions_$stamp.csv")
                }
            },
            onPickPdf = {
                scope.launch {
                    exportSheetState.hide()
                    showExportOptions = false
                    onExternalActivityLaunched()
                    pdfExportLauncher.launch("paisa_transactions_$stamp.pdf")
                }
            }
        )
    }
}
