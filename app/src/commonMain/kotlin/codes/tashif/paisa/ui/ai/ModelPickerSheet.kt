package codes.tashif.paisa.ui.ai

import codes.tashif.paisa.ui.components.AdaptiveSheet

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ai.CatalogState
import codes.tashif.paisa.ai.ProviderStatus
import codes.tashif.paisa.ai.catalog.ModelInfo
import codes.tashif.paisa.ui.components.SearchLeadingIcon
import codes.tashif.paisa.ui.theme.spacing

/** The models of one provider that has a key. */
data class PickerGroup(val providerId: String, val providerName: String, val models: List<ModelInfo>)

/**
 * Providers with keys and their models. With [needsImage], models known not
 * to read images are left out; unknown ones stay, since the runner tries them.
 */
fun pickerGroups(providers: List<ProviderStatus>, catalog: CatalogState, needsImage: Boolean): List<PickerGroup> =
    providers.filter { it.hasKey }.map { status ->
        val models = catalog.models[status.info.id].orEmpty()
        PickerGroup(
            status.info.id,
            status.info.name,
            if (needsImage) models.filter { it.acceptsImage != false } else models,
        )
    }

/** A searchable list across [groups], at most [MODEL_PAGE] per provider until "Show more". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModelPickerSheet(
    groups: List<PickerGroup>,
    markUnknownImage: Boolean,
    onPick: (providerId: String, modelId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var limit by remember(query) { mutableIntStateOf(MODEL_PAGE) }
    val filtered = remember(groups, query) {
        groups.map { it.copy(models = filterModels(it.models, query)) }.filter { it.models.isNotEmpty() }
    }
    AdaptiveSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text("Add model", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search models") },
                leadingIcon = { SearchLeadingIcon(query = query, onClear = { query = "" }) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge
            )
            when {
                groups.isEmpty() -> NoteCard("Save a key for a provider first. Only providers with keys are listed.")
                filtered.isEmpty() -> NoteCard("No models match.")
                else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    filtered.forEach { group ->
                        item(key = "h:${group.providerId}") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = MaterialTheme.spacing.small, bottom = 4.dp)
                            ) {
                                ProviderAvatar(providerId = group.providerId, size = 28.dp)
                                Spacer(Modifier.size(MaterialTheme.spacing.smaller))
                                Text(
                                    "${group.providerName} · ${group.models.size}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        items(group.models.take(limit), key = { "m:${group.providerId}:${it.id}" }) { model ->
                            PickerRow(
                                model = model,
                                logoProviderId = relayLogo(group.providerId, model),
                                unknownImage = markUnknownImage && model.acceptsImage == null,
                                onClick = { onPick(group.providerId, model.id) }
                            )
                        }
                        if (group.models.size > limit) {
                            item(key = "more:${group.providerId}") {
                                TextButton(onClick = { limit += MODEL_PAGE }) { Text("Show more") }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(MaterialTheme.spacing.larger)) }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(model: ModelInfo, logoProviderId: String?, unknownImage: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (logoProviderId != null) {
            ProviderAvatar(providerId = logoProviderId, size = 36.dp)
            Spacer(Modifier.size(MaterialTheme.spacing.small))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(model.name, style = MaterialTheme.typography.titleSmall)
            if (model.name != model.id) {
                Text(model.id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ModelBadges(model, extra = if (unknownImage) "Image support unknown" else null)
        }
    }
}
