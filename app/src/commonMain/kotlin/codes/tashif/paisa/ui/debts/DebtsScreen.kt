package codes.tashif.paisa.ui.debts

import codes.tashif.paisa.ui.platform.PlatformBackHandler
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.VerticalDivider
import codes.tashif.paisa.data.DebtPersonBalance
import codes.tashif.paisa.ui.app.CenteredContent
import codes.tashif.paisa.ui.app.ListPaneWidth
import codes.tashif.paisa.ui.app.LocalWindowWidth
import codes.tashif.paisa.ui.app.PanePlaceholder
import codes.tashif.paisa.ui.app.WindowWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.components.EmptyState
import codes.tashif.paisa.ui.components.PaisaTopBar
import codes.tashif.paisa.ui.debts.split.SplitBillSheet
import codes.tashif.paisa.ui.format.debtTotals
import codes.tashif.paisa.ui.theme.spacing


/** Money between you and other people. Lives apart from accounts, so it never moves a balance. */
@Composable
fun DebtsScreen(viewModel: DebtsViewModel = koinViewModel()) {
    val people by viewModel.debtPeople.collectAsState()
    val openId by viewModel.openDebtPersonId.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currency ?: "₹"
    var splittingBill by remember { mutableStateOf(false) }

    val open = people.firstOrNull { it.person.id == openId }
    val person: @Composable (DebtPersonBalance) -> Unit = { p ->
        PlatformBackHandler { viewModel.closeDebtPerson() }
        val entries by viewModel.openDebtEntries.collectAsState()
        DebtPersonScreen(
            entry = p,
            entries = entries,
            currency = currency,
            onBack = { viewModel.closeDebtPerson() },
            onAddEntry = { amount, note -> viewModel.addDebtEntry(p.person.id, amount, note) },
            onUpdateEntry = viewModel::updateDebtEntry,
            onDeleteEntry = { viewModel.deleteDebtEntry(it) },
            onOpenTransaction = viewModel::openLinkedTransaction,
            onSettle = { viewModel.settleDebt(p.person) },
            onRename = { viewModel.renameDebtPerson(p.person, it) },
            onDelete = { viewModel.deleteDebtPerson(p.person) }
        )
    }
    // Wide windows: people on the left, the open person on the right.
    val wide = LocalWindowWidth.current >= WindowWidth.EXPANDED
    // Side by side, an empty pane is wasted space: start on the first person.
    var autoOpened by remember { mutableStateOf(false) }
    LaunchedEffect(wide, people.isEmpty()) {
        if (wide && !autoOpened && openId == null && people.isNotEmpty()) {
            autoOpened = true
            viewModel.openDebtPerson(people.first().person.id)
        }
    }
    if (open != null && !wide) {
        person(open)
        return
    }

    if (splittingBill) {
        SplitBillSheet(
            people = people,
            currency = currency,
            onDismiss = { splittingBill = false },
            onSave = { shares, note, date ->
                viewModel.recordBillSplit(shares, note, date)
                splittingBill = false
            }
        )
    }

    Row(Modifier.fillMaxSize()) {
    Column(modifier = if (wide && people.isNotEmpty()) Modifier.width(ListPaneWidth).fillMaxHeight() else Modifier.fillMaxSize()) {
        PaisaTopBar(title = "Debts") {
            TextButton(onClick = { splittingBill = true }) {
                Icon(Icons.AutoMirrored.Rounded.CallSplit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(MaterialTheme.spacing.smaller))
                Text("Split a bill")
            }
        }
        if (people.isEmpty()) {
            EmptyState(
                title = "Nobody owes anything",
                subtitle = "Add someone with +, use Split a bill, or turn on " +
                    "\"Split with others\" on a transaction"
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
                    selected = wide && p.person.id == openId,
                    onClick = { viewModel.openDebtPerson(p.person.id) }
                )
            }
        }
    }
    if (wide && people.isNotEmpty()) {
        VerticalDivider()
        Box(Modifier.weight(1f).fillMaxHeight()) {
            if (open != null) {
                CenteredContent { person(open) }
            } else {
                PanePlaceholder(title = "Debts", subtitle = "Pick someone to see what you owe each other")
            }
        }
    }
    }
}
