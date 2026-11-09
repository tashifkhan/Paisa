package codes.tashif.paisa.ui.ai

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import codes.tashif.paisa.ai.catalog.ModelInfo
import codes.tashif.paisa.ai.provider.AiProviderInfo
import codes.tashif.paisa.ai.provider.ApiStyle
import codes.tashif.paisa.ai.provider.BuiltInProviders
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.theme.spacing

@Composable
fun AiProviderScreen(
    providerId: String,
    onBack: () -> Unit,
    viewModel: AiProviderViewModel = hiltViewModel()
) {
    val providers by viewModel.providers.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    val test by viewModel.test.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    val status = providers.firstOrNull { it.info.id == providerId }

    // A deleted provider has nothing to show.
    LaunchedEffect(status == null, providers.isEmpty()) {
        if (status == null && providers.isNotEmpty()) onBack()
    }
    if (status == null) return
    val info = status.info

    // Plain remember: a key must never go into the activity's saved state.
    // After process death the draft reloads from encrypted storage.
    var key by remember(providerId) { mutableStateOf(viewModel.savedKey(providerId)) }
    var baseUrl by rememberSaveable(providerId) { mutableStateOf(status.baseUrl) }
    var baseUrlError by remember(providerId) { mutableStateOf<String?>(null) }
    var ladderPick by remember { mutableStateOf<ModelInfo?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Follow the saved key when it's removed or saved elsewhere.
    LaunchedEffect(status.hasKey) { key = viewModel.savedKey(providerId) }

    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(title = info.name, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge)
        ) {
            ProviderAvatar(
                providerId = info.id,
                custom = info.custom,
                configured = status.hasKey,
                size = 64.dp,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
            )
            Text(
                providerBlurb(info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ProviderKeyCard(
                state = ProviderKeyState(
                    key = key,
                    hasKey = status.hasKey,
                    baseUrl = baseUrl.takeIf { info.baseUrlEditable },
                    baseUrlHint = baseUrlHint(info),
                    baseUrlError = baseUrlError,
                    keyLabel = if (info.style == ApiStyle.RELAYROOM) "Gateway key" else "API key",
                    testResult = test?.takeIf { it.first == providerId }?.second,
                ),
                onKeyChange = { key = it },
                onBaseUrlChange = {
                    baseUrl = it
                    baseUrlError = null
                },
                onSave = {
                    viewModel.save(
                        status, key, baseUrl.takeIf { info.baseUrlEditable },
                        onError = { baseUrlError = it }
                    )
                },
                onTest = { viewModel.testConnection(providerId) },
                onRemove = { viewModel.removeKey(status) }
            )
            status.liveError?.let {
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                NoteCard("Couldn't list models: $it")
            }
            ProviderModelList(
                providerId = providerId,
                models = catalog.models[providerId].orEmpty(),
                refreshing = refreshing == providerId,
                canRefresh = status.hasKey,
                onRefresh = { viewModel.refreshModels(providerId) },
                onAddToLadder = { ladderPick = it },
                onRemoveManual = { viewModel.removeModel(providerId, it.id) },
                onAddById = { viewModel.addModel(providerId, it) }
            )
            if (info.custom) {
                Spacer(Modifier.height(MaterialTheme.spacing.large))
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete provider", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    ladderPick?.let { model ->
        AddToLadderDialog(
            model = model,
            onPick = { service ->
                viewModel.addToLadder(providerId, model.id, service)
                ladderPick = null
            },
            onDismiss = { ladderPick = null }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${info.name}?") },
            text = { Text("This removes its key, its models and every ladder step that uses it.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteProvider(status, onDeleted = onBack)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

private fun providerBlurb(info: AiProviderInfo): String = when {
    info.style == ApiStyle.RELAYROOM ->
        "Your own gateway. One key reaches every provider it routes, with aliases and subscription or API key routing."
    info.custom -> "${info.style.label} server you added."
    else -> "Tap a model to add it to a ladder."
}

private fun baseUrlHint(info: AiProviderInfo): String = when {
    info.id == BuiltInProviders.RELAYROOM ->
        "On a phone, use your computer's LAN address, like http://192.168.1.20:8317/v1. " +
            "10.0.2.2 works on an emulator."
    info.custom -> "${info.style.label} endpoint"
    else -> "Default: ${info.defaultBaseUrl}. Clear it to go back to the default."
}
