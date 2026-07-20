package codes.tashif.paisa.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import codes.tashif.paisa.data.PaisaViewModel
import codes.tashif.paisa.security.BiometricAuth
import codes.tashif.paisa.security.BiometricStatus
import codes.tashif.paisa.ui.components.PaisaTopBar
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.PaisaPalette
import codes.tashif.paisa.ui.theme.spacing
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: PaisaViewModel,
    onOpenSmsSetup: () -> Unit = {},
    onOpenAppearance: () -> Unit = {},
    onOpenAiSettings: () -> Unit = {},
    onOpenStatementImport: () -> Unit = {},
    onOpenMerchantMappings: () -> Unit = {},
    onOpenCategories: () -> Unit = {},
    onOpenCurrency: () -> Unit = {},
    onOpenAbout: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val scan by viewModel.smsScanProgress.collectAsState()
    val unrecognizedCount by viewModel.unrecognizedSmsCount.collectAsState()
    val mappings by viewModel.merchantMappings.collectAsState()
    val ai by viewModel.aiCredentials.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    val context = LocalContext.current
    val haptics = rememberHaptics()
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
                        viewModel.showSnackbar("Exported ${transactions.size} transactions to CSV")
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
                        viewModel.showSnackbar("Exported ${transactions.size} transactions to PDF")
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
            SettingsSectionLabel("Import")
            SettingsGroup {
                SettingsItem(
                    title = "SMS import",
                    subtitle = when {
                        scan.isRunning -> "Scanning… ${scan.processed}/${scan.total}"
                        unrecognizedCount > 0 ->
                            "$unrecognizedCount unrecognized · tap to manage"
                        else -> "Import bank transactions from SMS"
                    },
                    icon = Icons.Rounded.Sms,
                    position = groupPositionOf(0, 2),
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onOpenSmsSetup
                )
                SettingsItem(
                    title = "Import statement",
                    subtitle = if (ai.isConfigured) {
                        "PDF/CSV via ${ai.provider.label}"
                    } else {
                        "Upload bank statement with your AI key"
                    },
                    icon = Icons.Rounded.UploadFile,
                    position = groupPositionOf(1, 2),
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onOpenStatementImport
                )
            }

            SettingsSectionLabel("Data")
            SettingsGroup {
                SettingsItem(
                    title = "Export data",
                    subtitle = when {
                        isExporting -> "Exporting…"
                        transactions.isEmpty() -> "No transactions to export yet"
                        else ->
                            "${transactions.size} transaction${if (transactions.size == 1) "" else "s"} · CSV or PDF"
                    },
                    icon = Icons.Rounded.Share,
                    position = groupPositionOf(0, 1),
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = {
                        if (!isExporting) showExportOptions = true
                    }
                )
            }

            SettingsSectionLabel("Intelligence")
            SettingsGroup {
                SettingsItem(
                    title = "AI provider",
                    subtitle = if (ai.isConfigured) {
                        "${ai.provider.label} · key set"
                    } else {
                        "OpenAI-compatible, Gemini, or Anthropic"
                    },
                    icon = Icons.Rounded.AutoAwesome,
                    position = groupPositionOf(0, 2),
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = onOpenAiSettings
                )
                SettingsItem(
                    title = "Merchant rules",
                    subtitle = if (mappings.isEmpty()) {
                        "Learned merchant → category mappings"
                    } else {
                        "${mappings.size} saved rule${if (mappings.size == 1) "" else "s"}"
                    },
                    icon = Icons.Rounded.Rule,
                    position = groupPositionOf(1, 2),
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onOpenMerchantMappings
                )
            }

            SettingsSectionLabel("Preferences")
            SettingsGroup {
                SettingsItem(
                    title = "Categories",
                    subtitle = "${categories.size} categories",
                    icon = Icons.Rounded.Category,
                    position = groupPositionOf(0, 3),
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onOpenCategories
                )
                SettingsItem(
                    title = "Currency",
                    subtitle = settings?.currency ?: "₹",
                    icon = Icons.Rounded.Payments,
                    position = groupPositionOf(1, 3),
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = onOpenCurrency
                )
                SettingsItem(
                    title = "Appearance",
                    subtitle = run {
                        val mode = settings?.themeMode
                            ?.replaceFirstChar { it.uppercase() } ?: "System"
                        val palette = PaisaPalette.fromId(settings?.colorPalette).label
                        "$mode · $palette"
                    },
                    icon = Icons.Rounded.Palette,
                    position = groupPositionOf(2, 3),
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onOpenAppearance
                )
            }

            val hideByDefault = settings?.hideBalancesByDefault ?: true
            val biometricEnabled = settings?.biometricEnabled ?: false
            val biometricStatus = remember(context) { BiometricAuth.status(context) }
            val biometricAvailable = biometricStatus == BiometricStatus.Available
            val activity = context as? FragmentActivity

            SettingsSectionLabel("Privacy & security")
            SettingsGroup {
                SettingsItem(
                    title = "Hide balances by default",
                    subtitle = "Home amounts start hidden until you reveal them",
                    icon = Icons.Rounded.VisibilityOff,
                    position = groupPositionOf(0, 2),
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = {
                        haptics.toggle(on = !hideByDefault)
                        viewModel.setHideBalancesByDefault(!hideByDefault)
                    },
                    trailing = {
                        Switch(
                            checked = hideByDefault,
                            onCheckedChange = {
                                haptics.toggle(on = it)
                                viewModel.setHideBalancesByDefault(it)
                            }
                        )
                    }
                )
                SettingsItem(
                    title = "App lock",
                    subtitle = if (biometricAvailable) {
                        if (biometricEnabled) {
                            "Require biometrics when opening Paisa"
                        } else {
                            BiometricAuth.statusMessage(biometricStatus)
                        }
                    } else {
                        BiometricAuth.statusMessage(biometricStatus)
                    },
                    icon = if (biometricEnabled) Icons.Rounded.Lock else Icons.Rounded.Fingerprint,
                    position = groupPositionOf(1, 2),
                    iconContainerColor = if (biometricEnabled) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    },
                    iconContentColor = if (biometricEnabled) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    },
                    onClick = if (biometricAvailable) {
                        {
                            if (biometricEnabled) {
                                haptics.toggle(on = false)
                                viewModel.setBiometricLockEnabled(false)
                            } else if (activity != null) {
                                BiometricAuth.authenticate(
                                    activity = activity,
                                    title = "Enable app lock",
                                    subtitle = "Confirm it's you to require unlock next time",
                                    onSuccess = {
                                        haptics.toggle(on = true)
                                        viewModel.setBiometricLockEnabled(true)
                                        viewModel.showSnackbar("App lock enabled")
                                    },
                                    onError = { msg ->
                                        viewModel.showSnackbar(msg)
                                    }
                                )
                            }
                        }
                    } else {
                        {
                            viewModel.showSnackbar(BiometricAuth.statusMessage(biometricStatus))
                        }
                    },
                    trailing = {
                        Switch(
                            checked = biometricEnabled,
                            enabled = biometricAvailable,
                            onCheckedChange = { enabled ->
                                if (!biometricAvailable) {
                                    viewModel.showSnackbar(
                                        BiometricAuth.statusMessage(biometricStatus)
                                    )
                                    return@Switch
                                }
                                if (!enabled) {
                                    haptics.toggle(on = false)
                                    viewModel.setBiometricLockEnabled(false)
                                    return@Switch
                                }
                                if (activity == null) return@Switch
                                BiometricAuth.authenticate(
                                    activity = activity,
                                    title = "Enable app lock",
                                    subtitle = "Confirm it's you to require unlock next time",
                                    onSuccess = {
                                        haptics.toggle(on = true)
                                        viewModel.setBiometricLockEnabled(true)
                                        viewModel.showSnackbar("App lock enabled")
                                    },
                                    onError = { msg ->
                                        viewModel.showSnackbar(msg)
                                    }
                                )
                            }
                        )
                    }
                )
            }

            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            SettingsGroup {
                SettingsItem(
                    title = "About",
                    subtitle = "Offline-first · SMS on-device · optional BYOK AI",
                    icon = Icons.Rounded.Info,
                    position = groupPositionOf(0, 1),
                    iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    iconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onOpenAbout
                )
            }
        }
    }

    if (showExportOptions) {
        ModalBottomSheet(
            onDismissRequest = { showExportOptions = false },
            sheetState = exportSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.medium)
                    .padding(bottom = MaterialTheme.spacing.larger),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                Text(
                    text = "Export data",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (transactions.isEmpty()) {
                        "No transactions yet — export will create an empty file."
                    } else {
                        "All ${transactions.size} transaction${if (transactions.size == 1) "" else "s"} will be included."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(MaterialTheme.spacing.smaller))

                ExportFormatRow(
                    title = "CSV spreadsheet",
                    subtitle = ".csv · works in Sheets, Excel, Numbers",
                    icon = Icons.Rounded.TableChart,
                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        haptics.click()
                        scope.launch {
                            exportSheetState.hide()
                            showExportOptions = false
                            viewModel.onExternalActivityLaunched()
                            csvExportLauncher.launch("paisa_transactions_$stamp.csv")
                        }
                    }
                )

                ExportFormatRow(
                    title = "PDF statement",
                    subtitle = ".pdf · printable multi-page table",
                    icon = Icons.Rounded.PictureAsPdf,
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        haptics.click()
                        scope.launch {
                            exportSheetState.hide()
                            showExportOptions = false
                            viewModel.onExternalActivityLaunched()
                            pdfExportLauncher.launch("paisa_transactions_$stamp.pdf")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ExportFormatRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small + 2.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}
