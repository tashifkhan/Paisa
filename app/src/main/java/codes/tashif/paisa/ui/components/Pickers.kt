package codes.tashif.paisa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.data.Category
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/** Icon + color helpers for account types, mirroring [CategoryVisuals]. */
object AccountVisuals {
    fun icon(type: String?, iconName: String? = null): ImageVector {
        return when (type) {
            "Cash" -> Icons.Rounded.Payments
            "Credit Card", "Debit Card" -> Icons.Rounded.CreditCard
            "UPI" -> Icons.Rounded.PhoneAndroid
            "Wallet" -> Icons.Rounded.Wallet
            "Bank Account" -> Icons.Rounded.AccountBalance
            else -> CategoryVisuals.icon(iconName)
        }
    }

    fun color(hex: String?): Color? = CategoryVisuals.color(hex)
}

/**
 * Appearance-style dual/triple choice cards in a row.
 * Used for expense/income, theme mode, etc.
 */
@Composable
fun ChoiceCards(
    options: List<ChoiceOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
    ) {
        options.forEach { option ->
            val selected = selectedId == option.id
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        if (!selected) haptics.tick()
                        onSelect(option.id)
                    },
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MaterialTheme.spacing.medium),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    option.icon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(Modifier.size(6.dp))
                    }
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

data class ChoiceOption(
    val id: String,
    val label: String,
    val icon: ImageVector? = null
)

/**
 * Settings-group selectable list row — same visual language as More/Appearance.
 * Selected rows get a soft primary tint + checkmark.
 */
@Composable
fun SelectableOptionRow(
    title: String,
    selected: Boolean,
    position: GroupPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null
) {
    val haptics = rememberHaptics()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = groupShape(position),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (!selected) haptics.tick()
                    onClick()
                }
                .padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.small + 2.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.size(MaterialTheme.spacing.small))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (selected) {
                Spacer(Modifier.size(MaterialTheme.spacing.smaller))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

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
    label: String = "Category"
) {
    val selected = categories.firstOrNull { it.id == selectedId }
    var showSheet by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    if (categories.isEmpty()) {
        Text(
            text = emptyText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = MaterialTheme.spacing.smaller)
        )
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
            if (filtered.isEmpty()) {
                PickerEmptySearch(query)
            } else {
                SettingsGroup {
                    filtered.forEachIndexed { index, category ->
                        SelectableOptionRow(
                            title = category.name,
                            selected = selectedId == category.id,
                            position = groupPositionOf(index, filtered.size),
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
            }
        }
    }
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
    val haptics = rememberHaptics()

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
            if (rows.isEmpty()) {
                PickerEmptySearch(query)
            } else {
                SettingsGroup {
                    rows.forEachIndexed { index, category ->
                        if (category == null) {
                            SelectableOptionRow(
                                title = overallLabel,
                                subtitle = overallSubtitle,
                                selected = selectedId == null,
                                position = groupPositionOf(index, rows.size),
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
                                position = groupPositionOf(index, rows.size),
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
                }
            }
        }
    }
}

/** Collapsed account field + searchable sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPicker(
    accounts: List<Account>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    emptyText: String = "Add an account first from the Accounts tab",
    label: String = "Account"
) {
    val selected = accounts.firstOrNull { it.id == selectedId }
    var showSheet by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    if (accounts.isEmpty()) {
        Text(
            text = emptyText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = MaterialTheme.spacing.smaller)
        )
        return
    }

    PickerCollapsedRow(
        modifier = modifier,
        label = label,
        title = selected?.name ?: "Select account",
        subtitle = selected?.type,
        placeholder = selected == null,
        onClick = {
            haptics.click()
            showSheet = true
        },
        leading = {
            if (selected != null) {
                val accent = AccountVisuals.color(selected.color)
                    ?: MaterialTheme.colorScheme.primaryContainer
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AccountVisuals.icon(selected.type, selected.icon),
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                PickerPlaceholderIcon(Icons.Rounded.AccountBalanceWallet)
            }
        }
    )

    if (showSheet) {
        SearchablePickerSheet(
            title = "Choose account",
            searchPlaceholder = "Search accounts",
            onDismiss = { showSheet = false }
        ) { query ->
            val filtered = remember(accounts, query) {
                val q = query.trim()
                if (q.isEmpty()) accounts
                else accounts.filter {
                    it.name.contains(q, ignoreCase = true) ||
                        it.type.contains(q, ignoreCase = true)
                }
            }
            if (filtered.isEmpty()) {
                PickerEmptySearch(query)
            } else {
                SettingsGroup {
                    filtered.forEachIndexed { index, account ->
                        val accent = AccountVisuals.color(account.color)
                            ?: MaterialTheme.colorScheme.primaryContainer
                        SelectableOptionRow(
                            title = account.name,
                            subtitle = account.type,
                            selected = selectedId == account.id,
                            position = groupPositionOf(index, filtered.size),
                            onClick = {
                                onSelect(account.id)
                                showSheet = false
                            },
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(accent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = AccountVisuals.icon(
                                            account.type,
                                            account.icon
                                        ),
                                        contentDescription = null,
                                        tint = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerCollapsedRow(
    label: String,
    title: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    placeholder: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.small + 2.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading()
            Spacer(Modifier.size(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (placeholder) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PickerPlaceholderIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PickerEmptySearch(query: String) {
    Text(
        text = if (query.isBlank()) "Nothing here" else "No matches for “$query”",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = MaterialTheme.spacing.large)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchablePickerSheet(
    title: String,
    searchPlaceholder: String,
    onDismiss: () -> Unit,
    content: @Composable (query: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        // Soft focus so the user can type immediately without the keyboard
        // fighting the sheet animation on every device.
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
            // Focus can fail if the sheet isn't attached yet; safe to ignore.
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.size(MaterialTheme.spacing.small))
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {
                            keyboardController?.hide()
                        },
                        expanded = false,
                        onExpandedChange = {},
                        modifier = Modifier.focusRequester(focusRequester),
                        placeholder = { Text(searchPlaceholder) },
                        leadingIcon = {
                            SearchLeadingIcon(
                                query = query,
                                onClear = { query = "" }
                            )
                        },
                        trailingIcon = null
                    )
                },
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier.fillMaxWidth(),
                colors = SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shadowElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {}
            Spacer(Modifier.size(MaterialTheme.spacing.small))
            // Cap height so long lists scroll inside the sheet
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                content(query)
            }
        }
    }
}

/**
 * Compact wrap of type chips as soft cards (for account type, short option sets).
 * Prefer [ChoiceCards] when there are 2–3 options.
 */
@Composable
fun OptionChipGrid(
    options: List<ChoiceOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    // 2-column grid of soft selection cards
    val rows = options.chunked(2)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                row.forEach { option ->
                    val selected = selectedId == option.id
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (!selected) haptics.tick()
                                onSelect(option.id)
                            },
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = MaterialTheme.spacing.small,
                                    vertical = MaterialTheme.spacing.small + 2.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            option.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Spacer(Modifier.size(8.dp))
                            }
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Soft field card used inside bottom sheets (date/time, amount hero, etc.). */
@Composable
fun SheetFieldCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val haptics = rememberHaptics()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        haptics.click()
                        onClick()
                    }
                } else {
                    Modifier
                }
            ),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Box(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            content()
        }
    }
}

/** Appearance-style toggle card for sheet switches. */
@Composable
fun SheetToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val haptics = rememberHaptics()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptics.toggle(on = !checked)
                onCheckedChange(!checked)
            },
        shape = MaterialTheme.shapes.extraLarge,
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
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.size(MaterialTheme.spacing.small))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.size(MaterialTheme.spacing.smaller))
            androidx.compose.material3.Switch(
                checked = checked,
                onCheckedChange = {
                    haptics.toggle(on = it)
                    onCheckedChange(it)
                }
            )
        }
    }
}

@Composable
fun SheetSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            start = MaterialTheme.spacing.extraSmall,
            top = MaterialTheme.spacing.small,
            bottom = MaterialTheme.spacing.smaller
        )
    )
}
