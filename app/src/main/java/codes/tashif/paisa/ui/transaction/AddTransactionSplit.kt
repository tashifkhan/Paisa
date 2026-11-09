package codes.tashif.paisa.ui.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.data.repository.DebtShare
import codes.tashif.paisa.ui.components.SheetToggleCard
import codes.tashif.paisa.ui.debts.split.SplitEditor
import codes.tashif.paisa.ui.debts.split.SplitPlan
import codes.tashif.paisa.ui.debts.split.SplitPreview
import codes.tashif.paisa.ui.debts.split.compute
import codes.tashif.paisa.ui.debts.split.splitProblemText
import codes.tashif.paisa.ui.theme.spacing

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
    currency: String
) {
    Spacer(Modifier.height(MaterialTheme.spacing.small))
    SheetToggleCard(
        icon = Icons.Rounded.Handshake,
        title = personLinkTitle(isExpense),
        subtitle = personLinkSubtitle(isExpense),
        checked = state.on,
        onCheckedChange = { on ->
            state.on = on
            if (on && !isExpense) {
                if (state.personId == null) state.personId = people.firstOrNull()?.person?.id
                if (state.amountText.isEmpty()) state.amountText = amountText
            }
        }
    )
    AnimatedVisibility(visible = state.on) {
        Column(modifier = Modifier.padding(top = MaterialTheme.spacing.smaller)) {
            if (isExpense) {
                val outcome = state.plan.compute(amountText.toDoubleOrNull())
                SplitEditor(
                    people = people,
                    plan = state.plan,
                    onPlanChange = { state.plan = it },
                    outcome = outcome,
                    currency = currency
                )
                SplitPreview(
                    outcome = outcome,
                    currency = currency,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.small)
                )
                splitProblemText(outcome.problem)?.takeIf { state.plan.people.isNotEmpty() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = MaterialTheme.spacing.extraSmall)
                    )
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
