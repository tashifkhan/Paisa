package codes.tashif.paisa.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import codes.tashif.paisa.ui.theme.spacing

internal data class FilterMenuOption(
    val id: Int,
    val name: String,
    val icon: ImageVector,
    /** For categories: "income" or "expense", so the sheet can follow the type filter. */
    val type: String? = null
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
    accounts: List<FilterMenuOption>,
    currency: String,
    /** How many transactions the current search and filters show, for the sheet's button. */
    resultCount: Int
) {
    var showSheet by remember { mutableStateOf(false) }
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

        FilterChipRow(
            filters = filters,
            hasQuery = searchQuery.isNotBlank(),
            currency = currency,
            categories = categories,
            accounts = accounts,
            onFiltersChange = onFiltersChange,
            onClearAll = onClearAll,
            onOpenSheet = { showSheet = true }
        )
    }

    if (showSheet) {
        FilterSheet(
            filters = filters,
            onFiltersChange = onFiltersChange,
            categories = categories,
            accounts = accounts,
            currency = currency,
            resultCount = resultCount,
            onDismiss = { showSheet = false }
        )
    }
}
