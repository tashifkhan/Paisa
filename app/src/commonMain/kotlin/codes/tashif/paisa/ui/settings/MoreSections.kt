package codes.tashif.paisa.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import codes.tashif.paisa.data.AppTab
import codes.tashif.paisa.data.Settings
import codes.tashif.paisa.ui.ai.AiSummary
import codes.tashif.paisa.ui.app.icon
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.sms.SmsScanProgress
import codes.tashif.paisa.ui.theme.PaisaPalette

// The groups on the More screen. Each emits its label and group into the
// caller's Column.

@Composable
fun MoreScreensSection(
    hiddenTabs: List<AppTab>,
    onOpenTab: (AppTab) -> Unit
) {
    SettingsSectionLabel("Screens")
    SettingsGroup {
        hiddenTabs.forEachIndexed { index, tab ->
            SettingsItem(
                title = tab.label,
                subtitle = when (tab) {
                    AppTab.ANALYTICS -> "Spending by category and month"
                    AppTab.BUDGETS -> "Monthly limits per category"
                    AppTab.ACCOUNTS -> "Banks, cards, cash and wallets"
                    AppTab.DEBTS -> "Who owes you, and whom you owe"
                    else -> null
                },
                icon = tab.icon,
                position = groupPositionOf(index, hiddenTabs.size),
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = { onOpenTab(tab) }
            )
        }
    }
}

@Composable
fun MoreImportSection(
    scan: SmsScanProgress,
    unrecognizedCount: Int,
    ai: AiSummary,
    onOpenSmsSetup: () -> Unit,
    onOpenStatementImport: () -> Unit
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
            subtitle = if (ai.ready) {
                "PDF/CSV via ${ai.providerName}"
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
}

@Composable
fun MoreDataSection(
    isExporting: Boolean,
    transactionCount: Int,
    onOpenBackup: () -> Unit,
    onExport: () -> Unit
) {
    SettingsSectionLabel("Data")
    SettingsGroup {
        SettingsItem(
            title = "Backup",
            subtitle = "Google Drive, Nextcloud, or a file",
            icon = Icons.Rounded.CloudUpload,
            position = groupPositionOf(0, 2),
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onOpenBackup
        )
        SettingsItem(
            title = "Export data",
            subtitle = when {
                isExporting -> "Exporting…"
                transactionCount == 0 -> "No transactions to export yet"
                else ->
                    "$transactionCount transaction${if (transactionCount == 1) "" else "s"} · CSV or PDF"
            },
            icon = Icons.Rounded.Share,
            position = groupPositionOf(1, 2),
            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onExport
        )
    }
}

@Composable
fun MoreIntelligenceSection(
    ai: AiSummary,
    merchantRuleCount: Int,
    onOpenAiSettings: () -> Unit,
    onOpenMerchantMappings: () -> Unit
) {
    SettingsSectionLabel("Intelligence")
    SettingsGroup {
        SettingsItem(
            title = "AI",
            subtitle = if (ai.ready) {
                "${ai.modelName} · ${ai.providerName}"
            } else {
                "Providers, models and fallbacks"
            },
            icon = Icons.Rounded.AutoAwesome,
            position = groupPositionOf(0, 2),
            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onOpenAiSettings
        )
        SettingsItem(
            title = "Merchant rules",
            subtitle = if (merchantRuleCount == 0) {
                "Learned merchant → category mappings"
            } else {
                "$merchantRuleCount saved rule${if (merchantRuleCount == 1) "" else "s"}"
            },
            icon = Icons.Rounded.Rule,
            position = groupPositionOf(1, 2),
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onOpenMerchantMappings
        )
    }
}

@Composable
fun MorePreferencesSection(
    categoryCount: Int,
    settings: Settings?,
    onOpenCategories: () -> Unit,
    onOpenCurrency: () -> Unit,
    onOpenAppearance: () -> Unit
) {
    SettingsSectionLabel("Preferences")
    SettingsGroup {
        SettingsItem(
            title = "Categories",
            subtitle = "$categoryCount categories",
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
}

@Composable
fun MoreAboutSection(onOpenAbout: () -> Unit) {
    SettingsGroup {
        SettingsItem(
            title = "About",
            subtitle = "Version, where your data goes, and links",
            icon = Icons.Rounded.Info,
            position = groupPositionOf(0, 1),
            iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            iconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onOpenAbout
        )
    }
}
