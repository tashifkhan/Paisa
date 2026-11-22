package sh.taf.billi.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import sh.taf.billi.ui.components.DropdownFilterChip
import sh.taf.billi.ui.haptics.rememberHaptics
import sh.taf.billi.ui.theme.spacing

/**
 * The row under the search bar, laid out like search filters in Gmail,
 * Drive and Photos. "All filters" opens the full sheet. Then one chip per
 * group, always in the same order, each opening just its own picker and
 * showing what's picked once something is.
 */
@Composable
internal fun FilterChipRow(
    filters: TxFilters,
    hasQuery: Boolean,
    currency: String,
    categories: List<FilterMenuOption>,
    accounts: List<FilterMenuOption>,
    onClearAll: () -> Unit,
    onOpenSheet: (FilterSection?) -> Unit
) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = MaterialTheme.spacing.smaller),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DropdownFilterChip(
            label = "Filters",
            selected = filters.isActive,
            icon = Icons.Rounded.Tune,
            count = filters.activeCount,
            dropdown = false,
            onClick = { onOpenSheet(null) }
        )
        FilterSection.entries.forEach { section ->
            if (section == FilterSection.CATEGORY && categories.isEmpty()) return@forEach
            if (section == FilterSection.ACCOUNT && accounts.isEmpty()) return@forEach
            val summary = section.summary(filters, currency, categories, accounts)
            DropdownFilterChip(
                label = summary ?: section.title,
                selected = summary != null,
                icon = if (section == FilterSection.TYPE) typeIcon(filters.type) else section.icon,
                onClick = { onOpenSheet(section) }
            )
        }
        if (filters.isActive || hasQuery) {
            TextButton(onClick = {
                haptics.click()
                onClearAll()
            }) { Text("Clear all") }
        }
    }
}
