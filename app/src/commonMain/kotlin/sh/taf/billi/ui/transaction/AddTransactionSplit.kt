package sh.taf.billi.ui.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import sh.taf.billi.data.DebtPersonBalance
import sh.taf.billi.data.repository.DebtShare
import sh.taf.billi.ui.debts.split.SplitEditor
import sh.taf.billi.ui.debts.split.SplitPlan
import sh.taf.billi.ui.debts.split.SplitCaption
import sh.taf.billi.ui.debts.split.compute
import sh.taf.billi.ui.debts.split.splitProblemText
import sh.taf.billi.ui.haptics.rememberHaptics
import sh.taf.billi.ui.theme.spacing

/**
 * The Debts part of a new transaction. An expense can be split with others;
 * income can be someone paying you back.
 */
@Stable
class AddSplitState {
    var on by mutableStateOf(false)
    var plan by mutableStateOf(SplitPlan())

    // "Someone paid me back": one person and how much
    var personId by mutableStateOf<Int?>(null)
    var newName by mutableStateOf("")
    var amountText by mutableStateOf("")

    /**
     * What to record for a transaction of [amount], with positive amounts.
     * Empty when off. Null when it's on but can't be saved yet.
     */
    fun shares(isExpense: Boolean, amount: Double): List<DebtShare>? {
        if (!on) return emptyList()
        if (isExpense) {
            val outcome = plan.compute(amount)
            return if (outcome.isValid) outcome.owedToMe() else null
        }
        val link = personLinkOrNull(personId, newName, amountText.ifEmpty { formatPlain(amount) }) ?: return null
        return listOf(DebtShare(link.personId, link.newName, link.amount))
    }

    /** Why [shares] is null, for the snackbar. */
    fun problem(isExpense: Boolean, amount: Double): String =
        if (isExpense) splitProblemText(plan.compute(amount).problem) ?: "Check the split"
        else "Pick who paid you back and how much"
}

@Composable
fun rememberAddSplitState(): AddSplitState = remember { AddSplitState() }

@Composable
internal fun AddTransactionSplitSection(
    state: AddSplitState,
    isExpense: Boolean,
    people: List<DebtPersonBalance>,
    amountText: String,
    currency: String,
    onScannedTotal: (Double) -> Unit = {}
) {
    val haptics = rememberHaptics()
    fun setOn(on: Boolean) {
        haptics.toggle(on = on)
        state.on = on
        if (on && !isExpense) {
            if (state.personId == null) state.personId = people.firstOrNull()?.person?.id
            if (state.amountText.isEmpty()) state.amountText = amountText
        }
    }

    Spacer(Modifier.height(MaterialTheme.spacing.smaller))
    // One container for the switch and everything it turns on, so the split
    // reads as a single section of the sheet.
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        ListItem(
            headlineContent = { Text(personLinkTitle(isExpense)) },
            supportingContent = { Text(personLinkSubtitle(isExpense)) },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Handshake,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            // The row is the switch, so the Switch itself takes no clicks.
            trailingContent = { Switch(checked = state.on, onCheckedChange = null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.toggleable(value = state.on, role = Role.Switch, onValueChange = ::setOn)
        )
        AnimatedVisibility(visible = state.on) {
            Column(
                modifier = Modifier.padding(
                    start = MaterialTheme.spacing.medium,
                    end = MaterialTheme.spacing.medium,
                    bottom = MaterialTheme.spacing.medium
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))
                if (isExpense) {
                    val outcome = state.plan.compute(amountText.toDoubleOrNull())
                    SplitEditor(
                        people = people,
                        plan = state.plan,
                        onPlanChange = { state.plan = it },
                        outcome = outcome,
                        currency = currency,
                        onScannedTotal = onScannedTotal
                    )
                    splitProblemText(outcome.problem)?.takeIf { state.plan.people.isNotEmpty() }?.let {
                        SplitCaption(it)
                    }
                } else {
                    PersonLinkFields(
                        people = people,
                        selectedPersonId = state.personId,
                        onSelectPerson = { state.personId = it },
                        newName = state.newName,
                        onNewNameChange = { state.newName = it },
                        amountText = state.amountText,
                        onAmountChange = { state.amountText = it },
                        amountLabel = "Amount"
                    )
                }
            }
        }
    }
}
