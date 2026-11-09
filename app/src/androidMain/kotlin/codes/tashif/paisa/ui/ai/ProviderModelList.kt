package codes.tashif.paisa.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ai.catalog.ModelInfo
import codes.tashif.paisa.ai.catalog.ModelSource
import codes.tashif.paisa.ui.components.SearchLeadingIcon
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupShape
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.theme.spacing

/** How many models show before "Show more". */
internal const val MODEL_PAGE = 40

/** Models matching [query] by name or id, case-insensitive. */
internal fun filterModels(models: List<ModelInfo>, query: String): List<ModelInfo> {
    val q = query.trim()
    if (q.isEmpty()) return models
    return models.filter { it.id.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true) }
}

/** Search, the first [MODEL_PAGE] matches with "Show more", and "Add model by id". */
@Composable
internal fun ProviderModelList(
    providerId: String,
    models: List<ModelInfo>,
    refreshing: Boolean,
    canRefresh: Boolean,
    onRefresh: () -> Unit,
    onAddToLadder: (ModelInfo) -> Unit,
    onRemoveManual: (ModelInfo) -> Unit,
    onAddById: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var shown by remember(query) { mutableIntStateOf(MODEL_PAGE) }
    val matches = remember(models, query) { filterModels(models, query) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { SettingsSectionLabel("Models · ${models.size}") }
        if (refreshing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else if (canRefresh) {
            IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh models") }
        }
    }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search models") },
        leadingIcon = { SearchLeadingIcon(query = query, onClear = { query = "" }) },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge
    )
    Spacer(Modifier.height(MaterialTheme.spacing.small))
    if (matches.isEmpty()) {
        NoteCard(if (models.isEmpty()) "No models listed yet. Save a key to list them, or add one by id." else "No models match.")
    } else {
        val visible = matches.take(shown)
        SettingsGroup {
            visible.forEachIndexed { index, model ->
                ModelRow(
                    model = model,
                    logoProviderId = relayLogo(providerId, model),
                    position = groupPositionOf(index, visible.size),
                    onAddToLadder = { onAddToLadder(model) },
                    onRemove = if (model.source == ModelSource.MANUAL) ({ onRemoveManual(model) }) else null
                )
            }
        }
        if (matches.size > shown) {
            TextButton(onClick = { shown += MODEL_PAGE }, modifier = Modifier.fillMaxWidth()) {
                Text("Show more (${matches.size - shown} left)")
            }
        }
    }
    AddModelById(onAdd = onAddById)
}

@Composable
private fun ModelRow(
    model: ModelInfo,
    logoProviderId: String?,
    position: codes.tashif.paisa.ui.components.GroupPosition,
    onAddToLadder: () -> Unit,
    onRemove: (() -> Unit)?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = groupShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onAddToLadder
    ) {
        Row(
            modifier = Modifier.padding(start = MaterialTheme.spacing.medium, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (logoProviderId != null) {
                ProviderAvatar(providerId = logoProviderId, size = 36.dp)
                Spacer(Modifier.size(MaterialTheme.spacing.small))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(model.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (model.name != model.id) {
                    Text(
                        model.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ModelBadges(model, extra = if (model.source == ModelSource.MANUAL) "Added by you" else null)
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, contentDescription = "Remove ${model.id}") }
            }
            IconButton(onClick = onAddToLadder) {
                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "Add ${model.name} to a ladder")
            }
        }
    }
}

@Composable
private fun AddModelById(onAdd: (String) -> Unit) {
    var id by remember { mutableStateOf("") }
    SettingsSectionLabel("Add model by id")
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("vendor/model-name") },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii
            )
        )
        Spacer(Modifier.size(MaterialTheme.spacing.smaller))
        FilledTonalButton(
            onClick = {
                onAdd(id)
                id = ""
            },
            enabled = id.isNotBlank()
        ) { Text("Add") }
    }
}

/** RelayRoom lists many upstreams, so each of its models shows its own provider's logo. */
internal fun relayLogo(providerId: String, model: ModelInfo): String? =
    if (providerId == codes.tashif.paisa.ai.provider.BuiltInProviders.RELAYROOM) {
        modelLogoProvider(providerId, model.id)
    } else {
        null
    }
