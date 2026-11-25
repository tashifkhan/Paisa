package sh.taf.billi.ui.debts

import sh.taf.billi.ui.components.BilliMood
import sh.taf.billi.ui.platform.PlatformBackHandler
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.VerticalDivider
import sh.taf.billi.data.DebtPersonBalance
import sh.taf.billi.ui.app.CenteredContent
import sh.taf.billi.ui.app.ListPaneWidth
import sh.taf.billi.ui.app.LocalWindowWidth
import sh.taf.billi.ui.app.PanePlaceholder
import sh.taf.billi.ui.app.WindowWidth
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row as LayoutRow
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import sh.taf.billi.ui.format.formatMoney
import kotlin.math.abs
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
import sh.taf.billi.ui.components.EmptyState
import sh.taf.billi.ui.components.BilliTopBar
import sh.taf.billi.ui.debts.split.SplitBillSheet
import sh.taf.billi.ui.format.debtTotals
import sh.taf.billi.ui.theme.spacing
import sh.taf.billi.ui.components.LocalFloatingNavInset
import sh.taf.billi.ui.components.listPlacementSpring


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
        BilliTopBar(title = "Debts") {
            FilledTonalButton(
                onClick = { splittingBill = true },
                modifier = Modifier.padding(end = MaterialTheme.spacing.small)
            ) {
                Icon(Icons.AutoMirrored.Rounded.CallSplit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(MaterialTheme.spacing.smaller))
                Text("Split a bill")
            }
        }
        if (people.isEmpty()) {
            EmptyState(
                title = "Nobody owes anything",
                mood = BilliMood.Happy,
                subtitle = "Add someone with +, use Split a bill, or turn on " +
                    "\"Split with others\" on a transaction"
            )
            return@Column
        }
        val (owedToMe, iOwe) = debtTotals(people)
        // Who owes you, who you owe, then everyone settled up: each its own segmented group.
        val sections = listOf(
            DebtSection("Owe you", people.filter { it.balance >= SETTLED }, owedToMe),
            DebtSection("You owe", people.filter { it.balance <= -SETTLED }, iOwe),
            DebtSection("Settled", people.filter { abs(it.balance) < SETTLED }, null)
        ).filter { it.people.isNotEmpty() }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                bottom = MaterialTheme.spacing.extraLarge * 3 + LocalFloatingNavInset.current
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item(key = "totals") {
                DebtTotalsCard(
                    currency = currency,
                    owedToMe = owedToMe,
                    iOwe = iOwe,
                    owingCount = sections.firstOrNull { it.title == "Owe you" }?.people?.size ?: 0,
                    owedCount = sections.firstOrNull { it.title == "You owe" }?.people?.size ?: 0
                )
            }
            sections.forEach { section ->
                item(key = "header:${section.title}") {
                    DebtSectionHeader(
                        title = section.title,
                        count = section.people.size,
                        total = section.total?.let { formatMoney(currency, it) },
                        modifier = Modifier.animateItem(placementSpec = listPlacementSpring())
                    )
                }
                itemsIndexed(section.people, key = { _, p -> p.person.id }) { index, p ->
                    Box(Modifier.animateItem(placementSpec = listPlacementSpring())) {
                        DebtPersonRow(
                            entry = p,
                            currency = currency,
                            index = index,
                            count = section.people.size,
                            selected = wide && p.person.id == openId,
                            onClick = { viewModel.openDebtPerson(p.person.id) }
                        )
                    }
                }
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

private data class DebtSection(val title: String, val people: List<DebtPersonBalance>, val total: Double?)

/** A group's title in the primary color, its count, and its total on the right. */
@Composable
private fun DebtSectionHeader(title: String, count: Int, total: String?, modifier: Modifier = Modifier) {
    LayoutRow(
        modifier = modifier
            .padding(
                start = MaterialTheme.spacing.small,
                end = MaterialTheme.spacing.small,
                top = MaterialTheme.spacing.large,
                bottom = MaterialTheme.spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "  $count",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (total != null) {
            Text(
                text = total,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
