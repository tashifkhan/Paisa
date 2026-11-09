package codes.tashif.paisa.ui.ai

import androidx.compose.foundation.layout.Column
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ai.CatalogState
import codes.tashif.paisa.ai.ProviderStatus
import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.ai.ladder.Rung
import codes.tashif.paisa.ai.provider.ApiStyle
import codes.tashif.paisa.ai.provider.RouteMode
import codes.tashif.paisa.ai.provider.ThinkingLevel
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.theme.spacing

/** One ladder: the Default one when [service] is null, otherwise that service's. */
@Composable
fun AiLadderScreen(
    service: AiService?,
    onBack: () -> Unit,
    viewModel: AiLadderViewModel = koinViewModel()
) {
    val ladders by viewModel.ladders.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    var picking by remember { mutableStateOf(false) }

    val usesDefault = service != null && ladders.services.firstOrNull { it.service == service }?.usesDefault != false
    val rungs = if (service == null) ladders.default else ladders.forService(service)
    LaunchedEffect(rungs) { viewModel.loadRouteOptions(rungs) }
    val rows = rungs.mapIndexed { index, rung ->
        rungRow(index, rungs.size, rung, service, providers, catalog, viewModel.thinkingChoices(rung))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(title = ladderTitle(service), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge)
        ) {
            Text(
                service?.description ?: "Every service without its own ladder uses this one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (usesDefault) {
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                NoteCard("Uses the default ladder. Any change here gives ${service?.label} its own ladder.")
            }
            SettingsSectionLabel(if (rows.isEmpty()) "Models" else "Models · ${rows.size}")
            if (rows.isEmpty()) {
                NoteCard("No models yet. Add one below, then save a key for its provider.")
            } else {
                SettingsGroup {
                    rows.forEachIndexed { index, row ->
                        LadderRungCard(
                            row = row,
                            position = groupPositionOf(index, rows.size),
                            onMoveUp = { viewModel.move(service, index, -1) },
                            onMoveDown = { viewModel.move(service, index, 1) },
                            onRemove = { viewModel.remove(service, index) },
                            onThinking = { viewModel.setThinking(service, index, it) },
                            onRouteMode = { viewModel.setRouteMode(service, index, it) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            Button(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(MaterialTheme.spacing.extraSmall))
                Text("Add model")
            }
            if (service != null && !usesDefault) {
                Spacer(Modifier.height(MaterialTheme.spacing.smaller))
                OutlinedButton(onClick = { viewModel.useDefault(service) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Use default ladder")
                }
            }
            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            Text(
                "Models are tried top to bottom. If one fails or returns something unreadable, the next one runs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small)
            )
        }
    }

    if (picking) {
        val needsImage = service?.needsImage == true
        ModelPickerSheet(
            groups = pickerGroups(providers, catalog, needsImage),
            markUnknownImage = needsImage,
            onPick = { providerId, modelId ->
                viewModel.add(service, providerId, modelId)
                picking = false
            },
            onDismiss = { picking = false }
        )
    }
}

private fun rungRow(
    index: Int,
    count: Int,
    rung: Rung,
    service: AiService?,
    providers: List<ProviderStatus>,
    catalog: CatalogState,
    thinkingChoices: List<ThinkingLevel>,
): RungRow {
    val status = providers.firstOrNull { it.info.id == rung.providerId }
    val model = catalog.model(rung.providerId, rung.modelId)
    val relay = status?.info?.style == ApiStyle.RELAYROOM
    val routeChoices = if (!relay) {
        emptyList()
    } else {
        val allowed = model?.routeModes.orEmpty()
        RouteMode.entries.filter { it == RouteMode.AUTO || allowed.isEmpty() || it in allowed }
    }
    val warning = when {
        status == null -> "Its provider was removed. This step is skipped."
        !status.hasKey -> "No key saved for ${status.info.name}. This step is skipped."
        service?.needsImage == true && model?.acceptsImage == false -> "Doesn't read images. Skipped for photos."
        else -> null
    }
    return RungRow(
        number = index + 1,
        modelName = model?.name ?: rung.modelId,
        providerName = status?.info?.name ?: rung.providerId,
        logoProviderId = modelLogoProvider(rung.providerId, rung.modelId),
        customProvider = status?.info?.custom == true,
        thinking = rung.thinking,
        thinkingChoices = thinkingChoices,
        routeMode = rung.routeMode,
        routeChoices = routeChoices,
        warning = warning,
        canMoveUp = index > 0,
        canMoveDown = index < count - 1,
    )
}
