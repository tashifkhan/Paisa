package codes.tashif.paisa.ui.settings

import codes.tashif.paisa.backup.BackupNames
import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.platform.nowMillis
import org.koin.compose.viewmodel.koinViewModel
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import codes.tashif.paisa.ui.components.LocalSelectedSetting
import codes.tashif.paisa.ui.platform.VerticalScrollbarFor
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import codes.tashif.paisa.data.AppTab
import codes.tashif.paisa.ui.components.PaisaTopBar
import codes.tashif.paisa.ui.theme.spacing
import kotlinx.coroutines.launch

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
    /** Title of the setting open beside this list on wide windows. */
    selected: String? = null,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val categoryCount by viewModel.categoryCount.collectAsState()
    val scan by viewModel.smsScanProgress.collectAsState()
    val unrecognizedCount by viewModel.unrecognizedSmsCount.collectAsState()
    val merchantRuleCount by viewModel.merchantRuleCount.collectAsState()
    val ai by viewModel.aiSummary.collectAsState()
    val transactionCount by viewModel.transactionCount.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    val scope = rememberCoroutineScope()
    var showExportOptions by remember { mutableStateOf(false) }
    val exportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // A dialog has no sheet to slide away first.
    val sheetDialogs = codes.tashif.paisa.ui.components.sheetsAsDialogs()

    val stamp = remember {
        formatDate(nowMillis(), "yyyyMMdd")
    }

    fun exportHandler(kind: String, export: (ExportSink, (Boolean) -> Unit) -> Unit) =
        { sink: ExportSink?, error: String? ->
            when {
                sink != null -> export(sink) { success ->
                    if (success) {
                        viewModel.showSnackbar("Exported $transactionCount transactions to $kind")
                    } else {
                        viewModel.showSnackbar("Failed to export $kind")
                    }
                }
                error == "Failed to open destination" -> viewModel.showSnackbar("Failed to open $kind destination")
                error != null -> viewModel.showSnackbar("$kind export error: $error")
            }
        }

    val csvExportLauncher = rememberCreateDocument(
        "text/csv",
        exportHandler("CSV", viewModel::exportTransactionsToCsv)
    )

    val pdfExportLauncher = rememberCreateDocument(
        "application/pdf",
        exportHandler("PDF", viewModel::exportTransactionsToPdf)
    )

    val backupExportLauncher = rememberCreateDocument(BackupNames.MIME) { sink, error ->
        when {
            sink != null -> viewModel.exportBackupFile(sink) { success ->
                viewModel.showSnackbar(
                    if (success) "Saved a Paisa backup file" else "Couldn't save the backup file"
                )
            }
            error != null -> viewModel.showSnackbar("Backup file error: $error")
        }
    }

    val scroll = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize()) {
    CompositionLocalProvider(LocalSelectedSetting provides selected) {
    Column(modifier = Modifier.fillMaxSize()) {
        PaisaTopBar(title = "More")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
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
    }
        VerticalScrollbarFor(scroll, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }

    if (showExportOptions) {
        ExportSheet(
            transactionCount = transactionCount,
            sheetState = exportSheetState,
            onDismiss = { showExportOptions = false },
            onPickCsv = {
                scope.launch {
                    if (!sheetDialogs) exportSheetState.hide()
                    showExportOptions = false
                    onExternalActivityLaunched()
                    csvExportLauncher("paisa_transactions_$stamp.csv")
                }
            },
            onPickPdf = {
                scope.launch {
                    if (!sheetDialogs) exportSheetState.hide()
                    showExportOptions = false
                    onExternalActivityLaunched()
                    pdfExportLauncher("paisa_transactions_$stamp.pdf")
                }
            },
            backupEncrypted = viewModel.backupEncrypted,
            onPickBackup = {
                scope.launch {
                    if (!sheetDialogs) exportSheetState.hide()
                    showExportOptions = false
                    onExternalActivityLaunched()
                    backupExportLauncher(BackupNames.forTime(nowMillis()))
                }
            }
        )
    }
}
