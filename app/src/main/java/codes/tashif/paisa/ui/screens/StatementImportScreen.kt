package codes.tashif.paisa.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ai.ExtractedTransaction
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.data.PaisaViewModel
import codes.tashif.paisa.data.StatementUiState
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen
import codes.tashif.paisa.ui.theme.spacing
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementImportScreen(
    viewModel: PaisaViewModel,
    onBack: () -> Unit,
    onOpenAiSettings: () -> Unit
) {
    val state by viewModel.statementUiState.collectAsState()
    val credentials by viewModel.aiCredentials.collectAsState()
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
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.extractStatement(it) }
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
            StatementUiState.Idle -> IdleContent(
                providerLabel = if (credentials.isConfigured) {
                    "${credentials.provider.label} · ${credentials.model}"
                } else {
                    null
                },
                onOpenAiSettings = onOpenAiSettings,
                onChooseFile = {
                    picker.launch(
                        arrayOf(
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
                    )
                },
                canChooseFile = credentials.isConfigured
            )

            is StatementUiState.Extracting -> ExtractingContent(
                completed = s.completedChunks,
                total = s.totalChunks,
                onContinueInBackground = onBack
            )

            is StatementUiState.Preview -> {
                val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }
                    ?: accounts.firstOrNull()
                val selectedCount = s.rows.count { it.selected }
                val regularRows = s.rows.withIndex().filterNot { it.value.likelyDuplicate }
                val matchingRows = s.rows.withIndex().filter { it.value.likelyDuplicate }
                val allRegularRowsSelected = regularRows.isNotEmpty() &&
                    regularRows.all { it.value.selected }

                Column(modifier = Modifier.fillMaxSize()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MaterialTheme.spacing.medium),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                            Text(
                                text = "$selectedCount of ${s.rows.size} selected",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (s.chunksProcessed > 1) {
                                    "Parsed from ${s.chunksProcessed} statement chunks"
                                } else {
                                    "Review rows, then import into an account"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (matchingRows.isNotEmpty()) {
                                Text(
                                    text = "${matchingRows.size} possible " +
                                        "${if (matchingRows.size == 1) "match was" else "matches were"} " +
                                        "set aside",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        haptics.toggle(on = !allRegularRowsSelected)
                                        val updated = s.rows.map { row ->
                                            if (row.likelyDuplicate) row
                                            else row.copy(selected = !allRegularRowsSelected)
                                        }
                                        viewModel.updateStatementPreview(updated)
                                    }
                                ) {
                                    Text(if (allRegularRowsSelected) "Clear shown" else "Select shown")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(MaterialTheme.spacing.small))

                    ExposedDropdownMenuBox(
                        expanded = accountMenuExpanded,
                        onExpandedChange = { accountMenuExpanded = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MaterialTheme.spacing.medium)
                    ) {
                        OutlinedTextField(
                            value = selectedAccount?.name ?: "Select account",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Import into account") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = accountMenuExpanded
                                )
                            },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = accountMenuExpanded,
                            onDismissRequest = { accountMenuExpanded = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.name) },
                                    onClick = {
                                        haptics.tick()
                                        selectedAccountId = acc.id
                                        accountMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = codes.tashif.paisa.ui.components.AccountVisuals
                                                .icon(acc.type, acc.icon),
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
                    ) {
                        itemsIndexed(regularRows, key = { _, indexedRow ->
                            val row = indexedRow.value
                            "${row.date}-${row.amount}-${row.merchant}-${indexedRow.index}"
                        }) { _, indexedRow ->
                            val row = indexedRow.value
                            PreviewRow(
                                row = row,
                                currency = currency,
                                onToggle = {
                                    haptics.toggle(on = !row.selected)
                                    val updated = s.rows.toMutableList()
                                    updated[indexedRow.index] = row.copy(selected = !row.selected)
                                    viewModel.updateStatementPreview(updated)
                                }
                            )
                        }

                        if (matchingRows.isNotEmpty()) {
                            item {
                                TextButton(
                                    onClick = {
                                        haptics.tick()
                                        showStatementMatches = !showStatementMatches
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (showStatementMatches) {
                                            "Show less"
                                        } else {
                                            "Show more (${matchingRows.size} " +
                                                if (matchingRows.size == 1) "match)" else "matches)"
                                        }
                                    )
                                }
                            }
                        }

                        if (showStatementMatches) {
                            itemsIndexed(matchingRows, key = { _, indexedRow ->
                                val row = indexedRow.value
                                "match-${row.date}-${row.amount}-${row.merchant}-${indexedRow.index}"
                            }) { _, indexedRow ->
                                val row = indexedRow.value
                                PreviewRow(
                                    row = row,
                                    currency = currency,
                                    onToggle = {
                                        haptics.toggle(on = !row.selected)
                                        val updated = s.rows.toMutableList()
                                        updated[indexedRow.index] =
                                            row.copy(selected = !row.selected)
                                        viewModel.updateStatementPreview(updated)
                                    }
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.padding(MaterialTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
                    ) {
                        Button(
                            onClick = {
                                haptics.confirm()
                                viewModel.commitStatementImport(selectedAccount?.id)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedCount > 0
                        ) {
                            Text("Import selected ($selectedCount)")
                        }
                        OutlinedButton(
                            onClick = {
                                haptics.reject()
                                viewModel.resetStatementUi()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }

            is StatementUiState.Success -> SuccessContent(
                imported = s.imported,
                duplicates = s.duplicates,
                skipped = s.skipped,
                onDone = {
                    viewModel.resetStatementUi()
                    onBack()
                },
                onImportAnother = { viewModel.resetStatementUi() }
            )

            is StatementUiState.Error -> ErrorContent(
                message = s.message,
                showAiSettings = !credentials.isConfigured,
                onRetry = { viewModel.resetStatementUi() },
                onOpenAiSettings = onOpenAiSettings
            )
        }
    }
}

@Composable
private fun IdleContent(
    providerLabel: String?,
    onOpenAiSettings: () -> Unit,
    onChooseFile: () -> Unit,
    canChooseFile: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.medium)
            .padding(bottom = MaterialTheme.spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.UploadFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Upload a bank or UPI statement (PDF, CSV, or image). Paisa sends the " +
                        "statement content only to the AI endpoint you configure — your API " +
                        "key never leaves the device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                Text(
                    "AI provider",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))
                Text(
                    providerLabel ?: "No API key configured yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (providerLabel == null) {
                    Spacer(Modifier.height(MaterialTheme.spacing.smaller))
                    OutlinedButton(
                        onClick = onOpenAiSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Set up AI provider")
                    }
                } else {
                    TextButton(onClick = onOpenAiSettings) {
                        Text("Change provider")
                    }
                }
            }
        }

        Text(
            "Supported formats",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = MaterialTheme.spacing.smaller)
        )
        Text(
            "Text PDFs, CSV, plain-text exports, JPG, PNG, and WebP images up to 5 MB. " +
                "Image import requires a vision-capable AI model. Scanned/image-only PDFs " +
                "are not supported yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(MaterialTheme.spacing.smaller))
        val haptics = rememberHaptics()
        Button(
            onClick = {
                haptics.click()
                onChooseFile()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canChooseFile
        ) {
            Icon(Icons.Rounded.Description, contentDescription = null)
            Spacer(Modifier.width(MaterialTheme.spacing.smaller))
            Text("Choose file")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExtractingContent(
    completed: Int,
    total: Int,
    onContinueInBackground: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // M3 loading indicator — preferred replacement for indeterminate circular progress.
        LoadingIndicator()
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        Text(
            "Extracting transactions…",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        if (total > 0) {
            LinearWavyProgressIndicator(
                progress = { completed.toFloat() / total.toFloat() },
                modifier = Modifier.fillMaxWidth(0.75f)
            )
            Spacer(Modifier.height(MaterialTheme.spacing.smaller))
            Text(
                text = "Chunk $completed of $total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(0.75f))
            Spacer(Modifier.height(MaterialTheme.spacing.smaller))
            Text(
                text = "Reading file…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        Text(
            text = "You can leave this screen — we'll notify you when it's ready.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        OutlinedButton(onClick = onContinueInBackground) {
            Text("Continue in background")
        }
    }
}

@Composable
private fun SuccessContent(
    imported: Int,
    duplicates: Int,
    skipped: Int,
    onDone: () -> Unit,
    onImportAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column {
                    Text(
                        "Import complete",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Transactions are in your ledger and account balances were updated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        StatLine(label = "Imported", value = imported.toString())
        StatLine(label = "Duplicates skipped", value = duplicates.toString())
        StatLine(label = "Unselected skipped", value = skipped.toString())

        Spacer(Modifier.height(MaterialTheme.spacing.smaller))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
        OutlinedButton(onClick = onImportAnother, modifier = Modifier.fillMaxWidth()) {
            Text("Import another")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    showAiSettings: Boolean,
    onRetry: () -> Unit,
    onOpenAiSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Column {
                    Text(
                        "Could not import",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Try again")
        }
        if (showAiSettings) {
            OutlinedButton(onClick = onOpenAiSettings, modifier = Modifier.fillMaxWidth()) {
                Text("AI settings")
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PreviewRow(
    row: ExtractedTransaction,
    currency: String,
    onToggle: () -> Unit
) {
    val isIncome = row.type == "income"
    val initial = row.merchant.take(1).uppercase(Locale.getDefault()).ifBlank { "?" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = row.selected, onCheckedChange = { onToggle() })
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (row.likelyDuplicate) "${row.merchant} (Match)" else row.merchant,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = listOfNotNull(
                        row.date,
                        row.categoryName,
                        row.reference?.takeIf { it.isNotBlank() }?.let { "ref $it" }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (row.likelyDuplicate) {
                    Text(
                        text = row.duplicateNote ?: "Possibly already logged",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                text = (if (isIncome) "+" else "−") + formatMoney(currency, row.amount),
                style = MaterialTheme.typography.titleSmall,
                color = if (isIncome) IncomeGreen else ExpenseRed,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
