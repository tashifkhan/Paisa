package sh.taf.billi.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import sh.taf.billi.ui.components.DetailHeader
import sh.taf.billi.ui.components.EmptyState
import sh.taf.billi.ui.components.SettingsSectionLabel
import sh.taf.billi.ui.haptics.rememberHaptics
import sh.taf.billi.ui.platform.PlatformBackHandler
import sh.taf.billi.ui.theme.spacing
import org.koin.compose.viewmodel.koinViewModel

/** The rules Billi learned from your edits: merchant renames, then merchant categories. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantMappingsScreen(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = koinViewModel()
) {
    val mappings by viewModel.merchantMappings.collectAsState()
    val renames by viewModel.merchantRenames.collectAsState()

    PlatformBackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(title = "Merchant rules", onBack = onBack)

        if (mappings.isEmpty() && renames.isEmpty()) {
            EmptyState(
                title = "No merchant rules yet",
                subtitle = "When editing a transaction, turn on a rename or category rule above Save."
            )
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
        ) {
            if (renames.isNotEmpty()) {
                item { SettingsSectionLabel("Renames") }
                items(renames, key = { "rename:${it.rawName}" }) { rename ->
                    RuleCard(
                        title = rename.displayName,
                        subtitle = "Saved instead of “${rename.rawName}”",
                        deleteLabel = "Delete rename",
                        onDelete = { viewModel.deleteMerchantRename(rename.rawName) }
                    )
                }
            }
            if (mappings.isNotEmpty()) {
                item { SettingsSectionLabel("Categories") }
                items(mappings, key = { "category:${it.merchantName}" }) { mapping ->
                    RuleCard(
                        title = mapping.merchantName,
                        subtitle = "${mapping.categoryName} · ${mapping.categoryType}",
                        deleteLabel = "Delete mapping",
                        onDelete = { viewModel.deleteMerchantMapping(mapping.merchantName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleCard(title: String, subtitle: String, deleteLabel: String, onDelete: () -> Unit) {
    val haptics = rememberHaptics()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = {
                haptics.reject()
                onDelete()
            }) {
                Icon(Icons.Rounded.Delete, contentDescription = deleteLabel)
            }
        }
    }
}
