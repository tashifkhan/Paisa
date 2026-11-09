package codes.tashif.paisa.ui.statement

import org.koin.compose.viewmodel.koinViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.haptics.rememberHaptics

@Composable
fun StatementImportScreen(
    onBack: () -> Unit,
    onOpenAiSettings: () -> Unit,
    viewModel: StatementImportViewModel = koinViewModel()
) {
    val state by viewModel.statementUiState.collectAsState()
    val ai by viewModel.aiSummary.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currency ?: "₹"

    var selectedAccountId by remember { mutableStateOf<Int?>(null) }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var showStatementMatches by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    LaunchedEffect(state is StatementUiState.Preview) {
        if (state is StatementUiState.Preview) {
            showStatementMatches = false
        }
    }

    // Extraction finishes asynchronously — announce the outcome physically too.
    LaunchedEffect(state is StatementUiState.Success, state is StatementUiState.Error) {
        when (state) {
            is StatementUiState.Success -> haptics.confirm()
            is StatementUiState.Error -> haptics.reject()
            else -> Unit
        }
    }

    BackHandler {
        when (state) {
            is StatementUiState.Preview, is StatementUiState.Success, is StatementUiState.Error -> {
                viewModel.resetStatementUi()
            }
            // Extraction keeps running in the ViewModel; a notification fires when done.
            is StatementUiState.Extracting -> onBack()
            else -> onBack()
        }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.extractStatements(uris)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        DetailHeader(
            title = "Import statement",
            onBack = {
                viewModel.resetStatementUi()
                onBack()
            }
        )

        when (val s = state) {
            StatementUiState.Idle -> StatementIdleContent(
                providerLabel = if (ai.ready) {
                    "${ai.providerName} · ${ai.modelName}"
                } else {
                    null
                },
                onOpenAiSettings = onOpenAiSettings,
                onChooseFile = {
                    viewModel.onExternalActivityLaunched()
                    picker.launch(STATEMENT_MIME_TYPES)
                },
                canChooseFile = ai.ready
            )

            is StatementUiState.Extracting -> StatementExtractingContent(
                completed = s.completedChunks,
                total = s.totalChunks,
                currentFile = s.currentFile,
                totalFiles = s.totalFiles,
                onContinueInBackground = onBack
            )

            is StatementUiState.Preview -> StatementPreviewContent(
                preview = s,
                accounts = accounts,
                currency = currency,
                selectedAccountId = selectedAccountId,
                onSelectAccount = { selectedAccountId = it },
                accountMenuExpanded = accountMenuExpanded,
                onAccountMenuExpandedChange = { accountMenuExpanded = it },
                showMatches = showStatementMatches,
                onToggleShowMatches = { showStatementMatches = !showStatementMatches },
                onUpdateRows = { viewModel.updateStatementPreview(it) },
                onImport = { viewModel.commitStatementImport(it) },
                onCancel = { viewModel.resetStatementUi() }
            )

            is StatementUiState.Success -> StatementSuccessContent(
                imported = s.imported,
                duplicates = s.duplicates,
                skipped = s.skipped,
                onDone = {
                    viewModel.resetStatementUi()
                    onBack()
                },
                onImportAnother = { viewModel.resetStatementUi() }
            )

            is StatementUiState.Error -> StatementErrorContent(
                message = s.message,
                showAiSettings = !ai.ready,
                onRetry = { viewModel.resetStatementUi() },
                onOpenAiSettings = onOpenAiSettings
            )
        }
    }
}

private val STATEMENT_MIME_TYPES = arrayOf(
    "application/pdf",
    "text/csv",
    "text/plain",
    "text/*",
    "application/csv",
    "image/jpeg",
    "image/png",
    "image/webp",
    "*/*"
)
