package codes.tashif.paisa.ui.debts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import codes.tashif.paisa.ui.components.EmptyState
import codes.tashif.paisa.ui.components.PaisaTopBar
import codes.tashif.paisa.ui.format.debtTotals
import codes.tashif.paisa.ui.theme.spacing


/** Money between you and other people. Lives apart from accounts, so it never moves a balance. */
@Composable
fun DebtsScreen(viewModel: DebtsViewModel = hiltViewModel()) {
    val people by viewModel.debtPeople.collectAsState()
    val openId by viewModel.openDebtPersonId.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currency ?: "₹"

    val open = people.firstOrNull { it.person.id == openId }
    if (open != null) {
        BackHandler { viewModel.openDebtPerson(null) }
        val entries by viewModel.openDebtEntries.collectAsState()
        DebtPersonScreen(
            entry = open,
            entries = entries,
            currency = currency,
            onBack = { viewModel.openDebtPerson(null) },
            onAddEntry = { amount, note -> viewModel.addDebtEntry(open.person.id, amount, note) },
            onDeleteEntry = { viewModel.deleteDebtEntry(it) },
            onSettle = { viewModel.settleDebt(open.person) },
            onRename = { viewModel.renameDebtPerson(open.person, it) },
            onDelete = { viewModel.deleteDebtPerson(open.person) }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PaisaTopBar(title = "Debts")
        if (people.isEmpty()) {
            EmptyState(
                title = "Nobody owes anything",
                subtitle = "Add someone with +, or mark a transaction with " +
                    "\"Someone owes me for this\""
            )
            return@Column
        }
        val (owedToMe, iOwe) = debtTotals(people)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                bottom = MaterialTheme.spacing.extraLarge * 3
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                DebtTotalsCard(currency = currency, owedToMe = owedToMe, iOwe = iOwe)
                Spacer(Modifier.height(MaterialTheme.spacing.medium))
            }
            itemsIndexed(people, key = { _, p -> p.person.id }) { index, p ->
                DebtPersonRow(
                    entry = p,
                    currency = currency,
                    index = index,
                    count = people.size,
                    onClick = { viewModel.openDebtPerson(p.person.id) }
                )
            }
        }
    }
}
