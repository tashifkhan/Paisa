package sh.taf.billi.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sh.taf.billi.ui.haptics.rememberHaptics

/** One choice in a [ConnectedToggleGroup] or [ToggleChoices]. */
data class ToggleOption<T>(val value: T, val label: String, val icon: ImageVector? = null)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun choiceColors(): ToggleButtonColors {
    // Built by hand: the defaults helper has a different name in androidx and in Compose Multiplatform.
    val c = MaterialTheme.colorScheme
    return ToggleButtonColors(
        containerColor = c.surfaceContainerHighest,
        contentColor = c.onSurfaceVariant,
        disabledContainerColor = c.onSurface.copy(alpha = 0.1f),
        disabledContentColor = c.onSurface.copy(alpha = 0.38f),
        checkedContainerColor = c.primary,
        checkedContentColor = c.onPrimary
    )
}

/**
 * The Material 3 Expressive connected button group: options share one row,
 * the outer ends are round, and the picked one morphs to a full pill.
 * [isChecked] decides each button, so it works for single or multi select.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ConnectedToggleGroup(
    options: List<ToggleOption<T>>,
    isChecked: (T) -> Boolean,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    radio: Boolean = true
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            val checked = isChecked(option.value)
            ToggleButton(
                checked = checked,
                onCheckedChange = {
                    haptics.toggle(on = !checked)
                    onClick(option.value)
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .semantics { role = if (radio) Role.RadioButton else Role.Checkbox },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = choiceColors(),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                option.icon?.let { icon ->
                    Icon(if (checked) Icons.Rounded.Check else icon, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                }
                Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * Separate toggle buttons that wrap onto more lines, for lists too long for
 * one connected row. Each one turns from a rounded square into a pill when
 * picked, the Expressive shape change.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun <T> ToggleChoices(
    options: List<ToggleOption<T>>,
    isChecked: (T) -> Boolean,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val checked = isChecked(option.value)
            ToggleButton(
                checked = checked,
                onCheckedChange = {
                    haptics.toggle(on = !checked)
                    onClick(option.value)
                },
                colors = choiceColors(),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                if (checked || option.icon != null) {
                    Icon(if (checked) Icons.Rounded.Check else option.icon!!, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                }
                Text(option.label, maxLines = 1)
            }
        }
    }
}

/**
 * A rounded card for one group of filters: the title, and
 * what's picked on the right, with a Clear action once something is.
 */
@Composable
fun FilterSectionCard(
    title: String,
    summary: String?,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val active = summary != null
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 48.dp)) {
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                AnimatedContent(
                    targetState = summary ?: "Any",
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "summary"
                ) { text ->
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (onClear != null && active) {
                TextButton(onClick = onClear) { Text("Clear") }
            }
        }
        content()
    }
}

/**
 * A filter chip that opens a picker, as in Gmail, Drive and Photos search.
 * It shows the picked value once there is one; [count] adds a badge.
 */
@Composable
fun DropdownFilterChip(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    count: Int = 0,
    dropdown: Boolean = true
) {
    val haptics = rememberHaptics()
    FilterChip(
        selected = selected,
        onClick = {
            haptics.click()
            onClick()
        },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 8.dp)) },
        leadingIcon = { Icon(icon, null, Modifier.size(FilterChipDefaults.IconSize)) },
        trailingIcon = when {
            count > 0 -> {
                { Badge { Text(count.toString()) } }
            }
            dropdown -> {
                { Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(FilterChipDefaults.IconSize)) }
            }
            else -> null
        },
        shape = RoundedCornerShape(12.dp)
    )
}

/**
 * A wide tappable row inside a filter card, for things that open a dialog:
 * a custom date range, say. Tinted when it's the active choice.
 */
@Composable
fun FilterActionTile(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val haptics = rememberHaptics()
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    )
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val corner by animateDpAsState(if (selected) 28.dp else 16.dp, spring(stiffness = Spring.StiffnessMediumLow))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .clickable {
                haptics.click()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = fg)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) trailing() else Icon(Icons.Rounded.ChevronRight, null, tint = fg)
    }
}
