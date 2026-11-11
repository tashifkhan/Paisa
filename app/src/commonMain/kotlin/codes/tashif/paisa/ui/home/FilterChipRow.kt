package codes.tashif.paisa.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/**
 * The row under the search bar. A Filters chip with a count always leads and
 * opens the full sheet. With nothing applied the row offers quick one-tap
 * filters. Once something is on, each filter shows as its own chip with an
 * ✕, so you can see what's narrowing the list and drop any one of them.
 */
@Composable
internal fun FilterChipRow(
    filters: TxFilters,
    hasQuery: Boolean,
    currency: String,
    categories: List<FilterMenuOption>,
    accounts: List<FilterMenuOption>,
    onFiltersChange: (TxFilters) -> Unit,
    onClearAll: () -> Unit,
    onOpenSheet: () -> Unit
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
        FilterChip(
            selected = filters.isActive,
            onClick = {
                haptics.click()
                onOpenSheet()
            },
            label = { Text("Filters") },
            leadingIcon = { Icon(Icons.Rounded.Tune, null, Modifier.size(FilterChipDefaults.IconSize)) },
            trailingIcon = if (filters.isActive) {
                { Badge { Text(filters.activeCount.toString()) } }
            } else {
                null
            }
        )

        // Applied filters first, each removable…
        activeChips(filters, currency, categories, accounts).forEach { chip ->
            key(chip.label) {
                ActiveChip(chip) {
                    haptics.tick()
                    onFiltersChange(chip.remove(filters))
                }
            }
        }
        // …then the quick filters that would still add something, so they stack.
        if (filters.type == null) {
            QuickChip("Expenses", Icons.AutoMirrored.Rounded.TrendingDown) {
                onFiltersChange(filters.copy(type = "expense"))
            }
            QuickChip("Income", Icons.AutoMirrored.Rounded.TrendingUp) {
                onFiltersChange(filters.copy(type = "income"))
            }
        }
        if (!filters.hasDate) {
            QuickChip("This month", Icons.Rounded.CalendarMonth) {
                onFiltersChange(filters.copy(datePreset = DatePreset.THIS_MONTH))
            }
        }
        if (filters.sources.isEmpty()) {
            QuickChip("From SMS", Icons.Rounded.Sms) {
                onFiltersChange(filters.copy(sources = setOf(TxSource.SMS.id)))
            }
        }

        if (filters.isActive || hasQuery) {
            TextButton(onClick = {
                haptics.click()
                onClearAll()
            }) { Text("Clear all") }
        }
    }
}

@Composable
private fun QuickChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    val haptics = rememberHaptics()
    FilterChip(
        selected = false,
        onClick = {
            haptics.toggle(on = true)
            onClick()
        },
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, Modifier.size(FilterChipDefaults.IconSize)) }
    )
}

private class ActiveFilter(
    val label: String,
    val icon: ImageVector,
    val remove: (TxFilters) -> TxFilters
)

/** An applied filter. Tapping it removes it, as the ✕ says; the Filters chip edits. */
@Composable
private fun ActiveChip(chip: ActiveFilter, onRemove: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(chip.label, maxLines = 1) },
        leadingIcon = { Icon(chip.icon, null, Modifier.size(InputChipDefaults.IconSize)) },
        trailingIcon = {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Remove ${chip.label}",
                modifier = Modifier.size(InputChipDefaults.IconSize)
            )
        }
    )
}

private fun activeChips(
    f: TxFilters,
    currency: String,
    categories: List<FilterMenuOption>,
    accounts: List<FilterMenuOption>
): List<ActiveFilter> = buildList {
    f.type?.let {
        add(
            ActiveFilter(
                if (it == "income") "Income" else "Expenses",
                if (it == "income") Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown
            ) { x -> x.copy(type = null) }
        )
    }
    if (f.hasDate) {
        val label = f.datePreset?.label ?: customRangeLabel(f.dateFrom!!, f.dateTo)
        add(ActiveFilter(label, Icons.Rounded.CalendarMonth) { x -> x.withoutDate() })
    }
    if (f.categoryIds.isNotEmpty()) {
        val label = if (f.categoryIds.size == 1) {
            categories.firstOrNull { it.id in f.categoryIds }?.name ?: "1 category"
        } else {
            "${f.categoryIds.size} categories"
        }
        add(ActiveFilter(label, Icons.Rounded.Category) { x -> x.copy(categoryIds = emptySet()) })
    }
    if (f.accountIds.isNotEmpty()) {
        val label = if (f.accountIds.size == 1) {
            accounts.firstOrNull { it.id in f.accountIds }?.name ?: "1 account"
        } else {
            "${f.accountIds.size} accounts"
        }
        add(ActiveFilter(label, Icons.Rounded.AccountBalanceWallet) { x -> x.copy(accountIds = emptySet()) })
    }
    if (f.hasAmount) {
        val min = f.minAmount?.let { formatMoney(currency, it) }
        val max = f.maxAmount?.let { formatMoney(currency, it) }
        val label = when {
            min != null && max != null -> "$min – $max"
            min != null -> "$min or more"
            else -> "Up to $max"
        }
        add(ActiveFilter(label, Icons.Rounded.Payments) { x -> x.withoutAmount() })
    }
    if (f.sources.isNotEmpty()) {
        val label = TxSource.entries.filter { it.id in f.sources }.joinToString(", ") { it.label }
        add(ActiveFilter(label, Icons.Rounded.Sms) { x -> x.copy(sources = emptySet()) })
    }
}
