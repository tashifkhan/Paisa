package codes.tashif.paisa.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import codes.tashif.paisa.data.PaisaViewModel
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.components.EmptyState
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantMappingsScreen(
    viewModel: PaisaViewModel,
    onBack: () -> Unit
) {
    val mappings by viewModel.merchantMappings.collectAsState()

    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(title = "Merchant rules", onBack = onBack)

        if (mappings.isEmpty()) {
            EmptyState(
                title = "No learned merchants yet",
                subtitle = "When you change a transaction category and enable “Always use for this merchant”, the rule appears here."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.small
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                items(mappings, key = { it.merchantName }) { mapping ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MaterialTheme.spacing.medium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mapping.merchantName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${mapping.categoryName} · ${mapping.categoryType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val haptics = rememberHaptics()
                            IconButton(onClick = {
                                haptics.reject()
                                viewModel.deleteMerchantMapping(mapping.merchantName)
                            }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete mapping")
                            }
                        }
                    }
                }
            }
        }
    }
}
