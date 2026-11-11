package codes.tashif.paisa.ui.home

import codes.tashif.paisa.ui.components.PillTabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.ui.components.AdaptiveSheet
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

private const val CATEGORIES_COLLAPSED = 10

/**
 * Every filter in one sheet. Changes apply as you make them, so the list
 * behind and the count on the button are always live; the button only closes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FilterSheet(
    filters: TxFilters,
    onFiltersChange: (TxFilters) -> Unit,
    categories: List<FilterMenuOption>,
    accounts: List<FilterMenuOption>,
    currency: String,
    resultCount: Int,
    onDismiss: () -> Unit
) {
    val haptics = rememberHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRangePicker by remember { mutableStateOf(false) }

    AdaptiveSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Filters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        haptics.click()
                        onFiltersChange(TxFilters())
                    },
                    enabled = filters.isActive
                ) { Text("Reset") }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                Section("Type")
                PillTabs(
                    options = listOf<Pair<String, String?>>("All" to null, "Expenses" to "expense", "Income" to "income"),
                    selected = filters.type,
                    onSelect = { value ->
                        // Drop categories that don't belong to the new type.
                        val keep = if (value == null) filters.categoryIds else {
                            filters.categoryIds.filter { id ->
                                categories.firstOrNull { it.id == id }?.type.let { t -> t == null || t == value }
                            }.toSet()
                        }
                        onFiltersChange(filters.copy(type = value, categoryIds = keep))
                    }
                )

                Section("Date")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
                ) {
                    DatePreset.entries.forEach { preset ->
                        val on = filters.datePreset == preset
                        Chip(on, preset.label) {
                            haptics.tick()
                            onFiltersChange(
                                if (on) filters.withoutDate() else filters.withoutDate().copy(datePreset = preset)
                            )
                        }
                    }
                    val custom = filters.datePreset == null && filters.dateFrom != null
                    FilterChip(
                        selected = custom,
                        onClick = {
                            haptics.click()
                            showRangePicker = true
                        },
                        label = { Text(if (custom) customRangeLabel(filters.dateFrom!!, filters.dateTo) else "Custom range") },
                        leadingIcon = {
                            Icon(Icons.Rounded.DateRange, null, Modifier.size(FilterChipDefaults.IconSize))
                        }
                    )
                }

                Section("Amount")
                AmountFields(
                    min = filters.minAmount,
                    max = filters.maxAmount,
                    currency = currency,
                    onChange = { min, max -> onFiltersChange(filters.copy(minAmount = min, maxAmount = max)) }
                )

                val shownCategories = categories.filter { filters.type == null || it.type == null || it.type == filters.type }
                if (shownCategories.isNotEmpty()) {
                    var showAll by remember { mutableStateOf(false) }
                    // Selected ones always show, even when the list is collapsed.
                    val visible = if (showAll || shownCategories.size <= CATEGORIES_COLLAPSED) {
                        shownCategories
                    } else {
                        (shownCategories.take(CATEGORIES_COLLAPSED) +
                            shownCategories.filter { it.id in filters.categoryIds }).distinct()
                    }
                    Section("Category", count = filters.categoryIds.size)
                    OptionChips(visible, filters.categoryIds) { onFiltersChange(filters.copy(categoryIds = it)) }
                    if (shownCategories.size > CATEGORIES_COLLAPSED) {
                        TextButton(onClick = { showAll = !showAll }) {
                            Text(if (showAll) "Show fewer" else "Show all ${shownCategories.size}")
                        }
                    }
                }

                if (accounts.isNotEmpty()) {
                    Section("Account", count = filters.accountIds.size)
                    OptionChips(accounts, filters.accountIds) { onFiltersChange(filters.copy(accountIds = it)) }
                }

                Section("Source")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
                ) {
                    TxSource.entries.forEach { src ->
                        val on = src.id in filters.sources
                        Chip(on, src.label) {
                            haptics.tick()
                            onFiltersChange(filters.copy(sources = if (on) filters.sources - src.id else filters.sources + src.id))
                        }
                    }
                }
                Spacer(Modifier.height(MaterialTheme.spacing.medium))
            }

            Button(
                onClick = {
                    haptics.click()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.spacing.small)
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    when (resultCount) {
                        0 -> "No matches. Show anyway"
                        1 -> "Show 1 transaction"
                        else -> "Show $resultCount transactions"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showRangePicker) {
        DateRangeDialog(
            from = filters.dateFrom,
            to = filters.dateTo,
            onDismiss = { showRangePicker = false },
            onApply = { from, to ->
                showRangePicker = false
                onFiltersChange(filters.withoutDate().copy(dateFrom = from, dateTo = to))
            }
        )
    }
}

@Composable
private fun Section(title: String, count: Int = 0) {
    Text(
        text = if (count > 0) "$title · $count" else title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = MaterialTheme.spacing.medium, bottom = MaterialTheme.spacing.smaller)
    )
}

@Composable
private fun Chip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        }
    )
}

/** Multi-select chips with each option's own icon; a check replaces it when picked. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionChips(options: List<FilterMenuOption>, selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val haptics = rememberHaptics()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
    ) {
        options.forEach { option ->
            val on = option.id in selected
            FilterChip(
                selected = on,
                onClick = {
                    haptics.tick()
                    onChange(if (on) selected - option.id else selected + option.id)
                },
                label = { Text(option.name) },
                leadingIcon = {
                    Icon(
                        if (on) Icons.Rounded.Check else option.icon,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            )
        }
    }
}

/** Min and max fields. Blank means no bound; text that isn't a number is ignored. */
@Composable
private fun AmountFields(min: Double?, max: Double?, currency: String, onChange: (Double?, Double?) -> Unit) {
    fun show(v: Double?) = v?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()
    var minText by remember { mutableStateOf(show(min)) }
    var maxText by remember { mutableStateOf(show(max)) }
    // Reset clears the fields too.
    if (min == null && minText.toDoubleOrNull() != null) minText = ""
    if (max == null && maxText.toDoubleOrNull() != null) maxText = ""

    fun parse(t: String) = t.replace(",", "").trim().toDoubleOrNull()?.takeIf { it >= 0 }

    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        OutlinedTextField(
            value = minText,
            onValueChange = {
                minText = it
                onChange(parse(it), parse(maxText))
            },
            label = { Text("Min") },
            prefix = { Text(currency) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = maxText,
            onValueChange = {
                maxText = it
                onChange(parse(minText), parse(it))
            },
            label = { Text("Max") },
            prefix = { Text(currency) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeDialog(
    from: String?,
    to: String?,
    onDismiss: () -> Unit,
    onApply: (String, String?) -> Unit
) {
    // The picker works in UTC midnight millis; our dates are plain yyyy-MM-dd.
    fun toMillis(d: String?) = d?.let { runCatching { LocalDate.parse(it).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() }.getOrNull() }
    fun toDay(ms: Long) = formatDate(ms, "yyyy-MM-dd", utc = true)

    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = toMillis(from),
        initialSelectedEndDateMillis = toMillis(to)
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = state.selectedStartDateMillis ?: return@TextButton
                    onApply(toDay(start), state.selectedEndDateMillis?.let(::toDay))
                },
                enabled = state.selectedStartDateMillis != null
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DateRangePicker(
            state = state,
            showModeToggle = false,
            modifier = Modifier.weight(1f)
        )
    }
}
