package codes.tashif.paisa.ui.home

import codes.tashif.paisa.ui.components.PillTabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.ui.components.AdaptiveSheet
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/** The name a row shows, which is what bulk rename starts from. */
internal fun TransactionWithDetails.displayTitle(): String =
    merchantName?.takeIf { it.isNotBlank() } ?: note.takeIf { it.isNotBlank() } ?: categoryName

// ── Selection bar ──────────────────────────────────────────────────────────

/** Takes the search bar's place while selecting: close, count, select all / none. */
@Composable
internal fun SelectionBar(count: Int, total: Int, onClose: () -> Unit, onToggleAll: () -> Unit) {
    val all = count >= total && total > 0
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium)
            .padding(bottom = MaterialTheme.spacing.smaller),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Stop selecting") }
            Text(
                "$count selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onToggleAll) {
                Icon(if (all) Icons.Rounded.Deselect else Icons.Rounded.SelectAll, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (all) "Select none" else "Select all $total")
            }
        }
    }
}

// ── Floating action toolbar ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BulkToolbar(
    enabled: Boolean,
    onRename: () -> Unit,
    onCategory: () -> Unit,
    onAccount: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    fun tap(action: () -> Unit) = { haptics.click(); action() }
    // Vibrant colours so the bar stands off the rows it floats over.
    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        modifier = modifier.shadow(8.dp, CircleShape)
    ) {
        ToolbarAction(Icons.Rounded.DriveFileRenameOutline, "Rename", enabled, tap(onRename))
        ToolbarAction(Icons.Rounded.Category, "Category", enabled, tap(onCategory))
        ToolbarAction(Icons.Rounded.AccountBalance, "Account", enabled, tap(onAccount))
        FilledIconButton(
            onClick = tap(onDelete),
            enabled = enabled,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) { Icon(Icons.Rounded.Delete, contentDescription = "Delete") }
    }
}

@Composable
private fun ToolbarAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) { Icon(icon, contentDescription = label) }
}

// ── Rename ─────────────────────────────────────────────────────────────────

private enum class RenameMode(val label: String) { SAME("Same name"), REPLACE("Find & replace"), AFFIX("Add text") }

/**
 * Renames many at once. Same name sets one title for all; Find & replace
 * edits each title (optionally as a regex, with $1 groups); Add text puts a
 * prefix and/or suffix around each. A live preview shows what will change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BulkRenameSheet(
    items: List<TransactionWithDetails>,
    onApply: (Map<Int, String>) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberHaptics()
    var mode by remember { mutableStateOf(RenameMode.SAME) }
    // Same name starts from the selection's shared title, if they all have one.
    var name by remember { mutableStateOf(items.map { it.displayTitle() }.distinct().singleOrNull().orEmpty()) }
    var find by remember { mutableStateOf("") }
    var replace by remember { mutableStateOf("") }
    var regex by remember { mutableStateOf(false) }
    var matchCase by remember { mutableStateOf(false) }
    var prefix by remember { mutableStateOf("") }
    var suffix by remember { mutableStateOf("") }

    val pattern: Result<Regex>? = if (mode == RenameMode.REPLACE && regex && find.isNotEmpty()) {
        runCatching { Regex(find, if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)) }
    } else {
        null
    }

    fun rename(old: String): String = when (mode) {
        RenameMode.SAME -> name.trim().ifEmpty { old }
        RenameMode.REPLACE -> when {
            find.isEmpty() -> old
            regex -> pattern?.getOrNull()?.let { r -> runCatching { r.replace(old, replace) }.getOrDefault(old) } ?: old
            else -> old.replace(find, replace, ignoreCase = !matchCase)
        }.trim().ifEmpty { old }
        RenameMode.AFFIX -> "$prefix$old$suffix".trim()
    }

    val changes = items.associate { it.id to (it.displayTitle() to rename(it.displayTitle())) }
        .filterValues { (old, new) -> new != old }

    AdaptiveSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
        ) {
            Text("Rename ${items.size}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            PillTabs(
                options = RenameMode.entries.map { it.label to it },
                selected = mode,
                onSelect = { mode = it }
            )
            Spacer(Modifier.height(MaterialTheme.spacing.medium))

            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                when (mode) {
                    RenameMode.SAME -> Field("New name", name) { name = it }
                    RenameMode.REPLACE -> {
                        Field(if (regex) "Find (pattern)" else "Find", find, isError = pattern?.isFailure == true) { find = it }
                        Field("Replace with", replace) { replace = it }
                        Toggle("Use a pattern (regex)", if (regex) "Use \$1, \$2 for groups" else "Plain text match", regex) { regex = it }
                        Toggle("Match case", null, matchCase) { matchCase = it }
                        if (pattern?.isFailure == true) {
                            Text(
                                "That pattern isn't valid",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    RenameMode.AFFIX -> {
                        Field("Add before", prefix) { prefix = it }
                        Field("Add after", suffix) { suffix = it }
                    }
                }

                Preview(changes.values.toList(), unchanged = items.size - changes.size)
            }

            Button(
                onClick = {
                    haptics.confirm()
                    onApply(changes.mapValues { it.value.second })
                },
                enabled = changes.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.spacing.small)
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    if (changes.isEmpty()) "Nothing to rename" else "Rename ${changes.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, isError: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Toggle(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Up to six "old → new" lines, then how many more and how many stay as they are. */
@Composable
private fun Preview(changes: List<Pair<String, String>>, unchanged: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Preview",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (changes.isEmpty()) {
                Text(
                    "No names change yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Same-name renames repeat the same line; show each distinct change once.
            val distinct = changes.distinct()
            distinct.take(6).forEach { (old, new) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        old,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward, null,
                        Modifier.padding(horizontal = 6.dp).size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        new,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            val more = distinct.size - 6
            val notes = listOfNotNull(
                if (more > 0) "+$more more" else null,
                if (unchanged > 0) "$unchanged stay the same" else null
            )
            if (notes.isNotEmpty()) {
                Text(
                    notes.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Pick a category or account ─────────────────────────────────────────────

internal data class PickOption(val id: Int, val name: String, val subtitle: String?, val icon: ImageVector)

/** One list sheet for "move these to…". Sections keep expense and income categories apart. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BulkPickSheet(
    title: String,
    sections: List<Pair<String?, List<PickOption>>>,
    onPick: (PickOption) -> Unit,
    onDismiss: () -> Unit
) {
    AdaptiveSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.large)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            sections.forEach { (label, options) ->
                if (options.isEmpty()) return@forEach
                if (label != null) SettingsSectionLabel(label) else Spacer(Modifier.height(MaterialTheme.spacing.small))
                SettingsGroup {
                    options.forEachIndexed { i, o ->
                        SettingsItem(
                            title = o.name,
                            subtitle = o.subtitle,
                            icon = o.icon,
                            position = groupPositionOf(i, options.size),
                            onClick = { onPick(o) }
                        )
                    }
                }
            }
        }
    }
}

// ── Delete ─────────────────────────────────────────────────────────────────

@Composable
internal fun BulkDeleteDialog(count: Int, fromSms: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DeleteOutline, null) },
        title = { Text(if (count == 1) "Delete 1 transaction?" else "Delete $count transactions?") },
        text = {
            Text(
                buildString {
                    append("Account balances update to match. This can't be undone.")
                    if (fromSms > 0) {
                        append(
                            if (fromSms == count) " They came from SMS, so a rescan won't bring them back."
                            else " The $fromSms from SMS won't come back on a rescan."
                        )
                    }
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
