package sh.taf.billi.ui.debts.split

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sh.taf.billi.data.DebtPersonBalance
import sh.taf.billi.ui.format.debtStatus
import sh.taf.billi.ui.format.formatMoney
import sh.taf.billi.ui.haptics.rememberHaptics
import sh.taf.billi.ui.theme.spacing
import kotlin.math.abs

private const val MAX_RESULTS = 6
private const val MAX_SUGGESTIONS = 6

/**
 * Who's in a split:
 * - a filter chip for You and a removable input chip per person,
 * - a search field that finds your Debts people and adds a new name if nobody matches,
 * - a few one-tap suggestions while the field is empty.
 * The full Debts list only shows up as search results, so a long list never floods the sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SplitPeoplePicker(
    people: List<DebtPersonBalance>,
    plan: SplitPlan,
    onPlanChange: (SplitPlan) -> Unit,
    currency: String
) {
    val haptics = rememberHaptics()
    var query by remember { mutableStateOf("") }
    val q = query.trim()

    fun personFor(p: DebtPersonBalance) = SplitPerson(p.person.id, p.person.name)
    fun toggle(person: SplitPerson) {
        haptics.tick()
        onPlanChange(plan.toggle(person))
    }
    fun addNew(name: String) {
        val known = people.firstOrNull { it.person.name.equals(name, ignoreCase = true) }?.let(::personFor)
        val person = known ?: SplitPerson(null, name)
        if (!plan.has(person)) onPlanChange(plan.copy(people = plan.people + person))
        haptics.tick()
        query = ""
    }

    // Starts-with matches first, then anywhere in the name.
    val matches = if (q.isEmpty()) emptyList() else people
        .filter { it.person.name.contains(q, ignoreCase = true) }
        .sortedBy { if (it.person.name.startsWith(q, ignoreCase = true)) 0 else 1 }
    val exact = people.any { it.person.name.equals(q, ignoreCase = true) } ||
        plan.people.any { it.name.equals(q, ignoreCase = true) }
    val suggestions = if (q.isNotEmpty()) emptyList() else people
        .filterNot { plan.has(personFor(it)) }
        .take(MAX_SUGGESTIONS)

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
        // Who's in
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
        ) {
            FilterChip(
                selected = plan.includeMe,
                onClick = {
                    haptics.tick()
                    onPlanChange(plan.copy(includeMe = !plan.includeMe))
                },
                label = { Text("You") },
                leadingIcon = if (plan.includeMe) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                }
            )
            plan.people.forEach { person ->
                InputChip(
                    selected = true,
                    onClick = { toggle(person) },
                    label = { Text(person.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    avatar = { SplitAvatar(person.name, InputChipDefaults.AvatarSize) },
                    trailingIcon = {
                        Icon(Icons.Rounded.Close, "Remove ${person.name}", Modifier.size(InputChipDefaults.IconSize))
                    }
                )
            }
        }

        // Search or add
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(if (people.isEmpty()) "Type a name to add" else "Search or add a person") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = if (query.isNotEmpty()) {
                { IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear") } }
            } else {
                null
            },
            singleLine = true,
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                // Enter picks the top match, or adds what was typed.
                val top = matches.firstOrNull()
                when {
                    q.isEmpty() -> Unit
                    top != null -> {
                        if (!plan.has(personFor(top))) toggle(personFor(top))
                        query = ""
                    }
                    else -> addNew(q)
                }
            }),
            modifier = Modifier.fillMaxWidth()
        )

        // One-tap adds while nothing is typed
        if (suggestions.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                suggestions.forEach { p ->
                    AssistChip(
                        onClick = { toggle(personFor(p)) },
                        label = { Text(p.person.name, maxLines = 1) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Add, null, Modifier.size(AssistChipDefaults.IconSize))
                        }
                    )
                }
            }
        }

        // Results while typing
        AnimatedVisibility(visible = q.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.extraSmall),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column {
                    // Known people first; "Add" goes last unless nobody matches.
                    matches.take(MAX_RESULTS).forEachIndexed { i, p ->
                        if (i > 0) RowDivider()
                        val person = personFor(p)
                        val inSplit = plan.has(person)
                        ResultRow(
                            title = p.person.name,
                            subtitle = balanceLine(p, currency),
                            avatar = { SplitAvatar(p.person.name) },
                            checked = inSplit,
                            onClick = {
                                toggle(person)
                                query = ""
                            }
                        )
                    }
                    if (!exact) {
                        if (matches.isNotEmpty()) RowDivider()
                        ResultRow(
                            title = "Add \"$q\"",
                            subtitle = "New person in Debts",
                            avatar = { NewAvatar() },
                            checked = false,
                            onClick = { addNew(q) }
                        )
                    }
                    if (matches.size > MAX_RESULTS) {
                        Text(
                            "${matches.size - MAX_RESULTS} more match. Keep typing to narrow it down.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.spacing.medium,
                                vertical = MaterialTheme.spacing.small
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun balanceLine(p: DebtPersonBalance, currency: String): String =
    if (abs(p.balance) < 0.005) "Settled" else "${debtStatus(p.balance)} ${formatMoney(currency, abs(p.balance))}"

/** Inset divider between rows of a list that sits in a tinted container. */
@Composable
internal fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun ResultRow(
    title: String,
    subtitle: String,
    avatar: @Composable () -> Unit,
    checked: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(subtitle) },
        leadingContent = avatar,
        trailingContent = if (checked) {
            { Icon(Icons.Rounded.Check, "In the split", tint = MaterialTheme.colorScheme.primary) }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/** A person's initial in a tinted circle. */
@Composable
internal fun SplitAvatar(name: String, size: Dp = 40.dp) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                name.trim().take(1).uppercase().ifEmpty { "?" },
                style = if (size >= 40.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun NewAvatar() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PersonAdd, null, modifier = Modifier.size(20.dp))
        }
    }
}
