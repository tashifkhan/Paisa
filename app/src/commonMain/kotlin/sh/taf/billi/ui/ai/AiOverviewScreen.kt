package sh.taf.billi.ui.ai

import androidx.compose.foundation.layout.Column
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sh.taf.billi.ai.CatalogState
import sh.taf.billi.ai.ProviderStatus
import sh.taf.billi.ai.ladder.AiService
import sh.taf.billi.ai.ladder.LadderState
import sh.taf.billi.ai.local.LocalAiCompatibility
import sh.taf.billi.ai.local.LocalAiState
import sh.taf.billi.ai.local.LocalAiStatus
import sh.taf.billi.ui.platform.isWebPlatform
import sh.taf.billi.ui.platform.thisDevice
import sh.taf.billi.ui.components.DetailHeader
import sh.taf.billi.ui.components.SettingsGroup
import sh.taf.billi.ui.components.SettingsItem
import sh.taf.billi.ui.components.rowTone
import sh.taf.billi.ui.components.SettingsSectionLabel
import sh.taf.billi.ui.components.groupPositionOf
import sh.taf.billi.ui.theme.spacing
import sh.taf.billi.ui.haptics.rememberHaptics

@Composable
fun AiOverviewScreen(
    onBack: () -> Unit,
    onOpenProvider: (String) -> Unit,
    onOpenLadder: (AiService?) -> Unit,
    onOpenLocalAi: () -> Unit,
    viewModel: AiViewModel = koinViewModel()
) {
    val providers by viewModel.providers.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    val ladders by viewModel.ladders.collectAsState()
    val localStatus by viewModel.localStatus.collectAsState()
    val useLocal by viewModel.useLocalAi.collectAsState()
    var addingProvider by remember { mutableStateOf(false) }
    val aiEnabled = LocalAiEnabled.current
    val haptics = rememberHaptics()

    LaunchedEffect(Unit) { viewModel.onOpen() }

    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(title = "AI", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge)
        ) {
            SettingsGroup(modifier = Modifier.padding(top = MaterialTheme.spacing.small)) {
                SettingsItem(
                    title = "Use AI",
                    subtitle = if (aiEnabled) {
                        "Add with AI, statement import and bill scan"
                    } else {
                        "Off. Billi shows no AI features"
                    },
                    icon = Icons.Rounded.AutoAwesome,
                    position = groupPositionOf(0, 1),
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = {
                        haptics.toggle(on = !aiEnabled)
                        viewModel.setAiEnabled(!aiEnabled)
                    },
                    trailing = {
                        Switch(
                            checked = aiEnabled,
                            onCheckedChange = {
                                haptics.toggle(on = it)
                                viewModel.setAiEnabled(it)
                            }
                        )
                    }
                )
            }
            // Turned off, the page is just the switch. Keys and models stay saved.
            if (!aiEnabled) return@Column
            // On a phone without Gemini Nano the row stays, greyed out; tapping it explains why.
            // The web has no on-device model at all, so it doesn't show the row.
            if (!isWebPlatform) {
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                SettingsGroup {
                    SettingsItem(
                        title = "Use local AI",
                        subtitle = localRowSubtitle(localStatus, useLocal),
                        icon = Icons.Rounded.Smartphone,
                        position = groupPositionOf(0, 1),
                        iconContainerColor = if (localStatus.ready) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        iconContentColor = if (localStatus.ready) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        onClick = onOpenLocalAi,
                        trailing = {
                            Switch(
                                checked = useLocal,
                                enabled = localStatus.ready,
                                onCheckedChange = {
                                    haptics.toggle(on = it)
                                    viewModel.setUseLocalAi(it)
                                }
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            NoteCard(
                text = if (localStatus.supported) {
                    "Use your own cloud key, or Gemini Nano on this phone. Only what you send to AI " +
                        "leaves the phone, and only to the providers you pick."
                } else {
                    "Use your own cloud key. Only what you send to AI leaves $thisDevice, and only to " +
                        "the providers you pick."
                },
                icon = Icons.Rounded.AutoAwesome,
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer
            )
            CatalogSection(catalog = catalog, onRefresh = viewModel::refreshCatalog)
            ServicesSection(
                ladders = ladders,
                catalog = catalog,
                providers = providers,
                onOpenLadder = onOpenLadder
            )
            ProvidersSection(
                providers = providers,
                onOpenProvider = onOpenProvider,
                onAddCustom = { addingProvider = true }
            )
        }
    }

    if (addingProvider) {
        AddCustomProviderSheet(
            onDismiss = { addingProvider = false },
            onAdd = { name, style, baseUrl, onError ->
                viewModel.addCustomProvider(
                    name, style, baseUrl,
                    onAdded = {
                        addingProvider = false
                        onOpenProvider(it.id)
                    },
                    onError = onError
                )
            }
        )
    }
}

@Composable
private fun CatalogSection(catalog: CatalogState, onRefresh: () -> Unit) {
    SettingsSectionLabel("Model catalogue")
    SettingsGroup {
        SettingsItem(
            title = "Model catalogue",
            subtitle = catalogSubtitle(catalog),
            icon = Icons.Rounded.Inventory2,
            position = groupPositionOf(0, 1),
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            trailing = {
                if (catalog.refreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh catalogue")
                    }
                }
            }
        )
    }
}

@Composable
private fun ServicesSection(
    ladders: LadderState,
    catalog: CatalogState,
    providers: List<ProviderStatus>,
    onOpenLadder: (AiService?) -> Unit
) {
    SettingsSectionLabel("Services")
    SettingsGroup {
        val services = ladders.services.filter { it.service in availableAiServices() }
        services.forEachIndexed { index, ladder ->
            SettingsItem(
                title = ladder.service.label,
                subtitle = if (ladder.usesDefault) {
                    "Uses default"
                } else {
                    ladderSummary(ladder.effective, catalog, providers)
                },
                icon = when (ladder.service) {
                    AiService.STATEMENT_IMPORT -> Icons.Rounded.UploadFile
                    AiService.STATEMENT_PHOTOS -> Icons.Rounded.Image
                    AiService.QUICK_ADD -> Icons.Rounded.AutoAwesome
                    AiService.MESSAGE_PARSE -> Icons.Rounded.Sms
                },
                position = groupPositionOf(index, services.size),
                iconContainerColor = rowTone(index).container,
                iconContentColor = rowTone(index).content,
                onClick = { onOpenLadder(ladder.service) }
            )
        }
    }
    Spacer(Modifier.height(MaterialTheme.spacing.small))
    SettingsGroup {
        SettingsItem(
            title = "Default ladder",
            subtitle = ladderSummary(ladders.default, catalog, providers),
            icon = Icons.Rounded.Layers,
            position = groupPositionOf(0, 1),
            onClick = { onOpenLadder(null) }
        )
    }
}

@Composable
private fun ProvidersSection(
    providers: List<ProviderStatus>,
    onOpenProvider: (String) -> Unit,
    onAddCustom: () -> Unit
) {
    val cloud = providers.filterNot { it.info.local }
    val withKeys = cloud.count { it.ready }
    SettingsSectionLabel(if (withKeys > 0) "Providers · $withKeys with keys" else "Providers")
    val count = cloud.size + 1
    SettingsGroup {
        cloud.forEachIndexed { index, status ->
            SettingsItem(
                title = status.info.name,
                subtitle = providerSubtitle(status),
                icon = Icons.Rounded.Dns,
                position = groupPositionOf(index, count),
                iconContainerColor = if (status.ready) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                iconContentColor = if (status.ready) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                onClick = { onOpenProvider(status.info.id) },
                leading = {
                    ProviderAvatar(
                        providerId = status.info.id,
                        custom = status.info.custom,
                        configured = status.ready
                    )
                },
                trailing = if (status.ready) {
                    {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "Key saved",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    null
                }
            )
        }
        SettingsItem(
            title = "Add custom provider",
            subtitle = "Any OpenAI, Anthropic or Gemini compatible endpoint",
            icon = Icons.Rounded.Add,
            position = groupPositionOf(count - 1, count),
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onAddCustom
        )
    }
}

private fun localRowSubtitle(status: LocalAiStatus, useLocal: Boolean): String = when (status.state) {
    LocalAiState.AVAILABLE -> if (useLocal) "On · first in the Default ladder" else "Ready on this phone"
    LocalAiState.DOWNLOADABLE -> "Ready to download"
    LocalAiState.DOWNLOADING -> "Downloading…"
    LocalAiState.CHECKING -> "Checking…"
    LocalAiState.UNSUPPORTED ->
        if (status.detail == LocalAiCompatibility.EMULATOR) "Not available on the emulator"
        else "Not available on this phone"
    LocalAiState.ERROR -> "Couldn't start · tap for details"
}
