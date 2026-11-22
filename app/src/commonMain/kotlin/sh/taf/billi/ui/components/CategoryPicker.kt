package sh.taf.billi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sh.taf.billi.data.Category
import sh.taf.billi.ui.haptics.rememberHaptics
import sh.taf.billi.ui.categories.CategoriesViewModel
import sh.taf.billi.ui.categories.CategoryDialog
import sh.taf.billi.ui.theme.spacing
import org.koin.compose.viewmodel.koinViewModel

/**
 * Collapsed category field. Tap opens a searchable sheet of options.
 * Keeps forms short while still supporting long category lists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPicker(
    categories: List<Category>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    emptyText: String = "No categories yet",
    label: String = "Category",
    /**
     * The type a category made from this picker gets. Null takes it from
     * [categories] when they all share one, and asks otherwise.
     */
    type: String? = null
) {
    val selected = categories.firstOrNull { it.id == selectedId }
    var showSheet by remember { mutableStateOf(false) }
    // Non-null while the new-category dialog is open; holds the name to start with.
    var newName by remember { mutableStateOf<String?>(null) }
    val haptics = rememberHaptics()

    newName?.let { name ->
        NewCategoryDialog(
            initialName = name,
            type = type ?: categories.map { it.type }.distinct().singleOrNull(),
            onDismiss = { newName = null },
            onAdded = { id ->
                onSelect(id)
                newName = null
                showSheet = false
            }
        )
    }

    if (categories.isEmpty()) {
        Text(
            text = emptyText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(top = MaterialTheme.spacing.smaller)
        )
        TextButton(onClick = { haptics.click(); newName = "" }) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("New category", modifier = Modifier.padding(start = MaterialTheme.spacing.smaller))
        }
        return
    }

    PickerCollapsedRow(
        modifier = modifier,
        label = label,
        title = selected?.name ?: "Select category",
        subtitle = if (selected == null) "Tap to choose" else null,
        placeholder = selected == null,
        onClick = {
            haptics.click()
            showSheet = true
        },
        leading = {
            if (selected != null) {
                CategoryBadge(
                    iconName = selected.icon,
                    categoryName = selected.name,
                    colorHex = selected.color,
                    size = 40.dp
                )
            } else {
                PickerPlaceholderIcon(Icons.Rounded.Search)
            }
        }
    )

    if (showSheet) {
        SearchablePickerSheet(
            title = "Choose category",
            searchPlaceholder = "Search categories",
            onDismiss = { showSheet = false }
        ) { query ->
            val filtered = remember(categories, query) {
                val q = query.trim()
                if (q.isEmpty()) categories
                else categories.filter { it.name.contains(q, ignoreCase = true) }
            }
            if (filtered.isEmpty()) PickerEmptySearch(query)
            SettingsGroup {
                    filtered.forEachIndexed { index, category ->
                        SelectableOptionRow(
                            title = category.name,
                            selected = selectedId == category.id,
                            position = groupPositionOf(index, filtered.size + 1),
                            onClick = {
                                onSelect(category.id)
                                showSheet = false
                            },
                            leading = {
                                CategoryBadge(
                                    iconName = category.icon,
                                    categoryName = category.name,
                                    colorHex = category.color,
                                    size = 40.dp
                                )
                            }
                        )
                    }
                    NewCategoryRow(
                        query = query,
                        position = groupPositionOf(filtered.size, filtered.size + 1),
                        onClick = { newName = it }
                    )
            }
        }
    }
}

/** Last row of a category list. Carries what's typed in the search box into the dialog. */
@Composable
private fun NewCategoryRow(query: String, position: GroupPosition, onClick: (String) -> Unit) {
    val haptics = rememberHaptics()
    val typed = query.trim()
    SelectableOptionRow(
        title = if (typed.isEmpty()) "New category" else "Create \"$typed\"",
        selected = false,
        position = position,
        onClick = {
            haptics.click()
            onClick(typed)
        },
        leading = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}

/** The Categories screen's dialog, saving straight to the database. */
@Composable
private fun NewCategoryDialog(
    initialName: String,
    type: String?,
    onDismiss: () -> Unit,
    onAdded: (Int) -> Unit,
    viewModel: CategoriesViewModel = koinViewModel()
) {
    // Subscribed so the view model knows how many categories there are when it orders the new one.
    viewModel.categories.collectAsState()
    CategoryDialog(
        title = "New category",
        initial = null,
        initialName = initialName,
        fixedType = type,
        onDismiss = onDismiss,
        onSave = { name, savedType, color, icon -> viewModel.addCategory(name, savedType, color, icon, onAdded) }
    )
}

/**
 * Collapsed category picker with an “Overall” option (budgets).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerWithOverall(
    categories: List<Category>,
    selectedId: Int?,
    onSelect: (Int?) -> Unit,
    overallLabel: String = "Overall",
    overallSubtitle: String = "All spending",
    modifier: Modifier = Modifier,
    label: String = "Category"
) {
    val selected = categories.firstOrNull { it.id == selectedId }
    val isOverall = selectedId == null
    var showSheet by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf<String?>(null) }
    val haptics = rememberHaptics()

    newName?.let { name ->
        NewCategoryDialog(
            initialName = name,
            // Budgets track spending.
            type = "expense",
            onDismiss = { newName = null },
            onAdded = { id ->
                onSelect(id)
                newName = null
                showSheet = false
            }
        )
    }

    PickerCollapsedRow(
        modifier = modifier,
        label = label,
        title = if (isOverall) overallLabel else (selected?.name ?: "Select category"),
        subtitle = if (isOverall) overallSubtitle else null,
        placeholder = !isOverall && selected == null,
        onClick = {
            haptics.click()
            showSheet = true
        },
        leading = {
            if (isOverall) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (selected != null) {
                CategoryBadge(
                    iconName = selected.icon,
                    categoryName = selected.name,
                    colorHex = selected.color,
                    size = 40.dp
                )
            } else {
                PickerPlaceholderIcon(Icons.Rounded.Search)
            }
        }
    )

    if (showSheet) {
        SearchablePickerSheet(
            title = "Choose category",
            searchPlaceholder = "Search categories",
            onDismiss = { showSheet = false }
        ) { query ->
            val q = query.trim()
            val overallMatches = q.isEmpty() ||
                overallLabel.contains(q, ignoreCase = true) ||
                overallSubtitle.contains(q, ignoreCase = true)
            val filtered = remember(categories, q) {
                if (q.isEmpty()) categories
                else categories.filter { it.name.contains(q, ignoreCase = true) }
            }
            val rows = buildList {
                if (overallMatches) add(null as Category?) // sentinel for Overall
                addAll(filtered)
            }
            if (rows.isEmpty()) PickerEmptySearch(query)
            SettingsGroup {
                    rows.forEachIndexed { index, category ->
                        if (category == null) {
                            SelectableOptionRow(
                                title = overallLabel,
                                subtitle = overallSubtitle,
                                selected = selectedId == null,
                                position = groupPositionOf(index, rows.size + 1),
                                onClick = {
                                    onSelect(null)
                                    showSheet = false
                                },
                                leading = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                MaterialTheme.colorScheme.secondaryContainer,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Payments,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            )
                        } else {
                            SelectableOptionRow(
                                title = category.name,
                                selected = selectedId == category.id,
                                position = groupPositionOf(index, rows.size + 1),
                                onClick = {
                                    onSelect(category.id)
                                    showSheet = false
                                },
                                leading = {
                                    CategoryBadge(
                                        iconName = category.icon,
                                        categoryName = category.name,
                                        colorHex = category.color,
                                        size = 40.dp
                                    )
                                }
                            )
                        }
                    }
                    NewCategoryRow(
                        query = query,
                        position = groupPositionOf(rows.size, rows.size + 1),
                        onClick = { newName = it }
                    )
            }
        }
    }
}
