package codes.tashif.paisa.ui.ai

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import codes.tashif.paisa.ai.CatalogState
import codes.tashif.paisa.ai.ProviderStatus
import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.ai.ladder.LadderState
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.theme.spacing

@Composable
fun AiOverviewScreen(
    onBack: () -> Unit,
    onOpenProvider: (String) -> Unit,
    onOpenLadder: (AiService?) -> Unit,
    viewModel: AiViewModel = hiltViewModel()
) {
    val providers by viewModel.providers.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    val ladders by viewModel.ladders.collectAsState()
    var addingProvider by remember { mutableStateOf(false) }

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
            NoteCard(
                text = "Bring your own key for statement import. SMS stays on this device. " +
                    "Only the files you import are sent, and only to the providers you pick.",
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
        ladders.services.forEachIndexed { index, ladder ->
            SettingsItem(
                title = ladder.service.label,
                subtitle = if (ladder.usesDefault) {
                    "Uses default"
                } else {
                    ladderSummary(ladder.effective, catalog, providers)
                },
                icon = if (ladder.service.needsImage) Icons.Rounded.Image else Icons.Rounded.UploadFile,
                position = groupPositionOf(index, ladders.services.size),
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
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
    val withKeys = providers.count { it.hasKey }
    SettingsSectionLabel(if (withKeys > 0) "Providers · $withKeys with keys" else "Providers")
    val count = providers.size + 1
    SettingsGroup {
        providers.forEachIndexed { index, status ->
            SettingsItem(
                title = status.info.name,
                subtitle = providerSubtitle(status),
                icon = Icons.Rounded.Dns,
                position = groupPositionOf(index, count),
                iconContainerColor = if (status.hasKey) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                iconContentColor = if (status.hasKey) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                onClick = { onOpenProvider(status.info.id) },
                leading = {
                    ProviderAvatar(
                        providerId = status.info.id,
                        custom = status.info.custom,
                        configured = status.hasKey
                    )
                },
                trailing = if (status.hasKey) {
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
