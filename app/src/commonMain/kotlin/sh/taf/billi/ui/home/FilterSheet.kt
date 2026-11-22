package sh.taf.billi.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import sh.taf.billi.platform.formatDate
import sh.taf.billi.platform.formatGrouped
import sh.taf.billi.ui.components.AdaptiveSheet
import sh.taf.billi.ui.components.CategoryBadge
import sh.taf.billi.ui.components.ConnectedToggleGroup
import sh.taf.billi.ui.components.FilterActionTile
import sh.taf.billi.ui.components.FilterSectionCard
import sh.taf.billi.ui.components.ToggleChoices
import sh.taf.billi.ui.components.ToggleOption
import sh.taf.billi.ui.haptics.rememberHaptics

private const val CATEGORIES_COLLAPSED = 8

/**
 * The filter sheet. With [focus] set it shows that one picker, the way a
 * dropdown chip in Gmail or Drive opens just its own choices. Without it,
 * every group shows as its own card. Changes apply as you make them, so the
 * list behind and the count on the button stay live; the button only closes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterSheet(
    filters: TxFilters,
    onFiltersChange: (TxFilters) -> Unit,
    categories: List<FilterMenuOption>,
    accounts: List<FilterMenuOption>,
    currency: String,
    resultCount: Int,
    onDismiss: () -> Unit,
    focus: FilterSection? = null
) {
    val haptics = rememberHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRangePicker by remember { mutableStateOf(false) }
    val sections = FilterSection.entries.filter { section ->
        when (section) {
            FilterSection.CATEGORY -> categories.isNotEmpty()
            FilterSection.ACCOUNT -> accounts.isNotEmpty()
            else -> true
        }
    }
    val shown = if (focus != null) listOf(focus) else sections
    val canReset = if (focus != null) focus.summary(filters, currency, categories, accounts) != null else filters.isActive

    AdaptiveSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            SheetHeader(
                title = focus?.title ?: "Filters",
                count = resultCount,
                active = if (focus == null) filters.activeCount else 0
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                shown.forEach { section ->
                    val body: @Composable () -> Unit = {
                        SectionBody(
                            section = section,
                            filters = filters,
                            onFiltersChange = onFiltersChange,
                            categories = categories,
                            accounts = accounts,
                            currency = currency,
                            onPickRange = { showRangePicker = true }
                        )
                    }
                    if (focus != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { body() }
                    } else {
                        FilterSectionCard(
                            title = section.title,
                            summary = section.summary(filters, currency, categories, accounts),
                            onClear = {
                                haptics.click()
                                onFiltersChange(section.clear(filters))
                            }
                        ) { body() }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        haptics.click()
                        onFiltersChange(focus?.clear(filters) ?: TxFilters())
                    },
                    enabled = canReset,
                    modifier = Modifier.height(56.dp)
                ) { Text(if (focus != null) "Clear" else "Reset all") }
                Button(
                    onClick = {
                        haptics.click()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    AnimatedContent(
                        targetState = resultCount,
                        transitionSpec = {
                            val up = targetState > initialState
                            (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith
                                (slideOutVertically { if (up) -it else it } + fadeOut())
                        },
                        label = "count"
                    ) { n ->
                        Text(
                            when (n) {
                                0 -> "No matches"
                                1 -> "Show 1 transaction"
                                else -> "Show ${formatGrouped(n.toDouble(), 0)} transactions"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
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
private fun SheetHeader(title: String, count: Int, active: Int) {
    Column(Modifier.padding(start = 4.dp, top = 4.dp, bottom = 16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        AnimatedContent(
            targetState = count to active,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "header"
        ) { (n, on) ->
            val matches = when (n) {
                0 -> "Nothing matches"
                1 -> "1 transaction matches"
                else -> "${formatGrouped(n.toDouble(), 0)} transactions match"
            }
            Text(
                if (on > 0) "$matches · $on ${if (on == 1) "filter" else "filters"} on" else matches,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionBody(
    section: FilterSection,
    filters: TxFilters,
    onFiltersChange: (TxFilters) -> Unit,
    categories: List<FilterMenuOption>,
    accounts: List<FilterMenuOption>,
    currency: String,
    onPickRange: () -> Unit
) {
    when (section) {
        FilterSection.TYPE -> ConnectedToggleGroup(
            options = listOf(
                ToggleOption<String?>(null, "All", typeIcon(null)),
                ToggleOption<String?>("expense", "Expenses", typeIcon("expense")),
                ToggleOption<String?>("income", "Income", typeIcon("income"))
            ),
            isChecked = { it == filters.type },
            onClick = { value ->
                // Drop categories that don't belong to the new type.
                val keep = if (value == null) filters.categoryIds else {
                    filters.categoryIds.filter { id ->
                        categories.firstOrNull { it.id == id }?.type.let { t -> t == null || t == value }
                    }.toSet()
                }
                onFiltersChange(filters.copy(type = value, categoryIds = keep))
            }
        )

        FilterSection.DATE -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ToggleChoices(
                options = DatePreset.entries.map { ToggleOption(it, it.label) },
                isChecked = { it == filters.datePreset },
                onClick = { preset ->
                    onFiltersChange(
                        if (filters.datePreset == preset) filters.withoutDate()
                        else filters.withoutDate().copy(datePreset = preset)
                    )
                }
            )
            val custom = filters.datePreset == null && filters.dateFrom != null
            FilterActionTile(
                title = "Custom range",
                subtitle = if (custom) fullRangeLabel(filters.dateFrom!!, filters.dateTo) else "Pick start and end days",
                selected = custom,
                onClick = onPickRange
            )
        }

        FilterSection.AMOUNT -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ToggleChoices(
                options = AMOUNT_BANDS.map { (lo, hi) -> ToggleOption(lo to hi, amountLabel(lo, hi, currency).orEmpty()) },
                isChecked = { (lo, hi) -> filters.minAmount == lo && filters.maxAmount == hi },
                onClick = { (lo, hi) ->
                    val on = filters.minAmount == lo && filters.maxAmount == hi
                    onFiltersChange(if (on) filters.withoutAmount() else filters.copy(minAmount = lo, maxAmount = hi))
                }
            )
            AmountFields(
                min = filters.minAmount,
                max = filters.maxAmount,
                currency = currency,
                onChange = { min, max -> onFiltersChange(filters.copy(minAmount = min, maxAmount = max)) }
            )
        }

        FilterSection.CATEGORY -> {
            val shownCategories = categories.filter { filters.type == null || it.type == null || it.type == filters.type }
            CategoryGrid(shownCategories, filters.categoryIds) { onFiltersChange(filters.copy(categoryIds = it)) }
        }

        FilterSection.ACCOUNT -> AccountList(accounts, filters.accountIds) { onFiltersChange(filters.copy(accountIds = it)) }

        FilterSection.SOURCE -> ToggleChoices(
            options = TxSource.entries.map { ToggleOption(it.id, it.label, it.icon) },
            isChecked = { it in filters.sources },
            onClick = { id ->
                onFiltersChange(filters.copy(sources = if (id in filters.sources) filters.sources - id else filters.sources + id))
            }
        )
    }
}

/** "16 Nov 2026 to 22 Nov 2026" for the custom range tile. */
private fun fullRangeLabel(from: String, to: String?): String {
    fun long(d: String) = runCatching {
        formatDate(LocalDate.parse(d).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(), "d MMM yyyy", utc = true)
    }.getOrDefault(d)
    return if (to == null || to == from) long(from) else "${long(from)} to ${long(to)}"
}

/**
 * Categories as a grid of their own coloured badges. A picked badge turns
 * from a circle into a rounded square with a check, and its tile fills.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryGrid(options: List<FilterMenuOption>, selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val haptics = rememberHaptics()
    var showAll by remember { mutableStateOf(false) }
    // Picked ones always show, even when the grid is collapsed.
    val visible = if (showAll || options.size <= CATEGORIES_COLLAPSED) options else {
        (options.take(CATEGORIES_COLLAPSED) + options.filter { it.id in selected }).distinct()
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth >= 560.dp -> 6
            maxWidth >= 420.dp -> 5
            else -> 4
        }
        val gap = 8.dp
        val tileWidth = (maxWidth - gap * (columns - 1)) / columns
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            maxItemsInEachRow = columns
        ) {
            visible.forEach { option ->
                val on = option.id in selected
                CategoryTile(option, on, Modifier.width(tileWidth)) {
                    haptics.toggle(on = !on)
                    onChange(if (on) selected - option.id else selected + option.id)
                }
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (options.size > CATEGORIES_COLLAPSED) {
            TextButton(onClick = { showAll = !showAll }) {
                Text(if (showAll) "Show fewer" else "Show all ${options.size}")
            }
        }
        Spacer(Modifier.weight(1f))
        if (options.size > 1) {
            val all = options.all { it.id in selected }
            TextButton(onClick = {
                haptics.click()
                onChange(if (all) selected - options.map { it.id }.toSet() else selected + options.map { it.id })
            }) {
                Icon(Icons.Rounded.DoneAll, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (all) "Pick none" else "Pick all")
            }
        }
    }
}

@Composable
private fun CategoryTile(option: FilterMenuOption, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0f)
    )
    val tileCorner by animateDpAsState(if (selected) 24.dp else 16.dp, spring(stiffness = Spring.StiffnessMediumLow))
    val badgeCorner by animateDpAsState(
        if (selected) 14.dp else 24.dp,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(tileCorner))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            CategoryBadge(
                iconName = option.iconName,
                categoryName = option.name,
                colorHex = option.colorHex,
                size = 48.dp,
                modifier = Modifier.clip(RoundedCornerShape(badgeCorner))
            )
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }
        // Long single words like "Entertainment" shrink rather than break mid-word.
        BasicText(
            option.name,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center
            ),
            // One line for a single word, so autosize shrinks it instead of wrapping.
            maxLines = if (' ' in option.name.trim()) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 12.sp, stepSize = 0.5.sp),
            modifier = Modifier.padding(top = 6.dp).heightIn(min = 32.dp)
        )
    }
}

/** Accounts as an Expressive segmented list with a checkbox on each row. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AccountList(options: List<FilterMenuOption>, selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val haptics = rememberHaptics()
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        options.forEachIndexed { index, option ->
            val on = option.id in selected
            SegmentedListItem(
                checked = on,
                onCheckedChange = {
                    haptics.toggle(on = !on)
                    onChange(if (on) selected - option.id else selected + option.id)
                },
                shapes = ListItemDefaults.segmentedShapes(index = index, count = options.size),
                colors = ListItemDefaults.segmentedColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                leadingContent = {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            option.icon,
                            null,
                            tint = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                trailingContent = { Checkbox(checked = on, onCheckedChange = null) }
            ) {
                Text(option.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Min and max fields. Blank means no bound; text that isn't a number is ignored. */
@Composable
private fun AmountFields(min: Double?, max: Double?, currency: String, onChange: (Double?, Double?) -> Unit) {
    fun show(v: Double?) = v?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()
    var minText by remember { mutableStateOf(show(min)) }
    var maxText by remember { mutableStateOf(show(max)) }
    // Follow changes made elsewhere, like a band or Reset, without fighting the typing.
    fun parse(t: String) = t.replace(",", "").trim().toDoubleOrNull()?.takeIf { it >= 0 }
    if (parse(minText) != min) minText = show(min)
    if (parse(maxText) != max) maxText = show(max)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(1f)
        )
        Text("to", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            shape = RoundedCornerShape(16.dp),
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
