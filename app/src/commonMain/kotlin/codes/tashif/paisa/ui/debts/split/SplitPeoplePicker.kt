package codes.tashif.paisa.ui.debts.split

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.ui.format.debtStatus
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing
import kotlin.math.abs

private const val MAX_RESULTS = 6

/**
 * Who's in a split, built to cope with a long list of people:
 * - the chosen people (and You) as removable chips,
 * - one field that searches your Debts people and adds a new name if nobody matches.
 * Nobody is listed until you type, so a long Debts list never floods the sheet.
 */
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

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
        // ── Who's in ──
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InputChip(
                selected = plan.includeMe,
                onClick = {
                    haptics.tick()
                    onPlanChange(plan.copy(includeMe = !plan.includeMe))
                },
                label = { Text("You") },
                leadingIcon = {
                    Avatar("Y", plan.includeMe)
                },
                trailingIcon = if (plan.includeMe) {
                    { Icon(Icons.Rounded.Check, "Included", Modifier.size(InputChipDefaults.IconSize)) }
                } else {
                    null
                },
                avatar = null
            )
            plan.people.forEach { person ->
                InputChip(
                    selected = true,
                    onClick = { toggle(person) },
                    label = { Text(person.name, maxLines = 1) },
                    leadingIcon = { Avatar(person.name, true) },
                    trailingIcon = {
                        Icon(Icons.Rounded.Close, "Remove ${person.name}", Modifier.size(InputChipDefaults.IconSize))
                    }
                )
            }
            if (plan.people.isEmpty()) {
                Text(
                    "Add who you split with",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // ── Search or add ──
        OutlinedTextField(
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
            shape = MaterialTheme.shapes.extraLarge,
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

        // ── Results while typing ──
        AnimatedVisibility(visible = q.isNotEmpty()) {
            ResultList {
                // Known people first; "Add" goes last unless nobody matches.
                matches.take(MAX_RESULTS).forEachIndexed { i, p ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    val person = personFor(p)
                    ResultRow(
                        title = p.person.name,
                        subtitle = balanceLine(p, currency),
                        avatar = { Avatar(p.person.name, plan.has(person), large = true) },
                        checked = plan.has(person),
                        onClick = {
                            toggle(person)
                            query = ""
                        }
                    )
                }
                if (!exact) {
                    if (matches.isNotEmpty()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
                        modifier = Modifier.padding(MaterialTheme.spacing.small)
                    )
                }
            }
        }

    }
}

private fun balanceLine(p: DebtPersonBalance, currency: String): String =
    if (abs(p.balance) < 0.005) "Settled" else "${debtStatus(p.balance)} ${formatMoney(currency, abs(p.balance))}"

@Composable
private fun ResultList(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column { content() }
    }
}

@Composable
private fun ResultRow(
    title: String,
    subtitle: String?,
    avatar: @Composable () -> Unit,
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.spacing.small, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        avatar()
        Spacer(Modifier.width(MaterialTheme.spacing.small))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (checked) Icon(Icons.Rounded.Check, "In the split", tint = MaterialTheme.colorScheme.primary)
    }
}

/** Initial in a circle; filled when the person is in the split. */
@Composable
private fun Avatar(name: String, on: Boolean, large: Boolean = false) {
    val size = if (large) 36.dp else InputChipDefaults.AvatarSize
    Surface(
        shape = CircleShape,
        color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                name.trim().take(1).uppercase().ifEmpty { "?" },
                style = if (large) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NewAvatar() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(36.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.PersonAdd, null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
