package codes.tashif.paisa.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import codes.tashif.paisa.ui.app.LocalShellState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.components.PaisaTopBar
import codes.tashif.paisa.ui.components.SearchLeadingIcon
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

internal data class FilterMenuOption(
    val id: Int,
    val name: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchAndFilters(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filters: TxFilters,
    onFiltersChange: (TxFilters) -> Unit,
    onClearAll: () -> Unit,
    categories: List<FilterMenuOption>,
    accounts: List<FilterMenuOption>
) {
    val haptics = rememberHaptics()
    // "/" and Ctrl K jump here on a keyboard.
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val shell = LocalShellState.current
    val focusPending = shell?.searchFocusPending == true
    LaunchedEffect(focusPending) {
        if (focusPending) {
            runCatching { focus.requestFocus() }
            shell?.searchFocused()
        }
    }
    Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium)) {
        // M3 search bar — collapsed input that filters the list in place.
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = onSearchChange,
                    onSearch = { onSearchChange(it) },
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text("Search transactions…") },
                    leadingIcon = {
                        SearchLeadingIcon(
                            query = searchQuery,
                            onClear = { onSearchChange("") }
                        )
                    },
                    trailingIcon = null,
                    modifier = Modifier
                        .focusRequester(focus)
                        // Esc leaves the box, so single-key shortcuts work again.
                        .onPreviewKeyEvent { e ->
                            if (e.key == Key.Escape && e.type == KeyEventType.KeyDown) {
                                focusManager.clearFocus()
                                true
                            } else {
                                false
                            }
                        }
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier.fillMaxWidth(),
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shadowElevation = 0.dp,
            // Nested under PaisaTopBar — don't re-apply status-bar insets (causes a huge top gap).
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {}

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = MaterialTheme.spacing.smaller),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
        ) {
            FilterChip(
                selected = filters.type == "expense",
                onClick = {
                    haptics.toggle(on = filters.type != "expense")
                    onFiltersChange(
                        filters.copy(type = if (filters.type == "expense") null else "expense")
                    )
                },
                label = { Text("Expenses") }
            )
            FilterChip(
                selected = filters.type == "income",
                onClick = {
                    haptics.toggle(on = filters.type != "income")
                    onFiltersChange(
                        filters.copy(type = if (filters.type == "income") null else "income")
                    )
                },
                label = { Text("Income") }
            )
            MultiSelectFilterChip(
                emptyLabel = "Category",
                selectedIds = filters.categoryIds,
                options = categories,
                clearIcon = Icons.Rounded.Category,
                onSelectionChange = { onFiltersChange(filters.copy(categoryIds = it)) }
            )
            MultiSelectFilterChip(
                emptyLabel = "Account",
                selectedIds = filters.accountIds,
                options = accounts,
                clearIcon = Icons.Rounded.AccountBalanceWallet,
                onSelectionChange = { onFiltersChange(filters.copy(accountIds = it)) }
            )
            FilterChip(
                selected = filters.source == "sms",
                onClick = {
                    haptics.toggle(on = filters.source != "sms")
                    onFiltersChange(
                        filters.copy(source = if (filters.source == "sms") null else "sms")
                    )
                },
                label = { Text("From SMS") }
            )
            if (filters.isActive || searchQuery.isNotBlank()) {
                FilterChip(
                    selected = false,
                    onClick = {
                        haptics.click()
                        onClearAll()
                    },
                    label = { Text("Clear") },
                    trailingIcon = {
                        Icon(
                            Icons.Rounded.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MultiSelectFilterChip(
    emptyLabel: String,
    selectedIds: Set<Int>,
    options: List<FilterMenuOption>,
    clearIcon: ImageVector,
    onSelectionChange: (Set<Int>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()
    val hasSelection = selectedIds.isNotEmpty()
    // +1 when a "Clear all" row is shown so item shapes stay correct.
    val itemCount = options.size + if (hasSelection) 1 else 0
    val menuScroll = rememberScrollState()
    // Cap height so long category/account lists scroll instead of overflowing the screen.
    val maxMenuHeight = (screenHeightDp() * 0.55f)
        .coerceIn(240f, 420f)
        .dp

    val chipLabel = when {
        selectedIds.isEmpty() -> emptyLabel
        selectedIds.size == 1 ->
            options.firstOrNull { it.id in selectedIds }?.name ?: emptyLabel
        else -> "$emptyLabel · ${selectedIds.size}"
    }

    Box {
        FilterChip(
            selected = hasSelection,
            onClick = {
                haptics.click()
                expanded = true
            },
            label = { Text(chipLabel) }
        )
        // Multi-select menu: toggle items without dismissing; clear all resets.
        // Extra-round bottom corners so the popup feels like an M3 cookie surface.
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = maxMenuHeight),
            scrollState = menuScroll,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 28.dp,
                bottomEnd = 28.dp
            )
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1)
            ) {
                var index = 0
                if (hasSelection) {
                    DropdownMenuItem(
                        checked = false,
                        onCheckedChange = {
                            haptics.tick()
                            onSelectionChange(emptySet())
                        },
                        text = { Text("Clear all") },
                        shapes = MenuDefaults.itemShape(index = index, count = itemCount),
                        leadingIcon = {
                            Icon(clearIcon, contentDescription = null)
                        }
                    )
                    index++
                    HorizontalDivider(
                        modifier = Modifier.padding(MenuDefaults.HorizontalDividerPadding)
                    )
                }
                options.forEach { option ->
                    val itemIndex = index
                    val isSelected = option.id in selectedIds
                    DropdownMenuItem(
                        checked = isSelected,
                        onCheckedChange = {
                            haptics.tick()
                            val next = if (isSelected) {
                                selectedIds - option.id
                            } else {
                                selectedIds + option.id
                            }
                            onSelectionChange(next)
                            // Keep menu open for multi-select
                        },
                        text = { Text(option.name) },
                        shapes = MenuDefaults.itemShape(index = itemIndex, count = itemCount),
                        leadingIcon = {
                            Icon(option.icon, contentDescription = null)
                        },
                        checkedLeadingIcon = {
                            Icon(Icons.Rounded.Check, contentDescription = null)
                        }
                    )
                    index++
                }
            }
        }
    }
}
