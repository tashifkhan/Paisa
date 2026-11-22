package sh.taf.billi.ui.budgets

import sh.taf.billi.ui.components.BilliMood
import androidx.compose.foundation.layout.Arrangement
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.unit.dp
import sh.taf.billi.ui.platform.VerticalScrollbarFor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import sh.taf.billi.ui.components.EmptyState
import sh.taf.billi.ui.components.InfoTip
import sh.taf.billi.ui.components.BilliTopBar
import sh.taf.billi.ui.theme.spacing
import sh.taf.billi.ui.components.LocalFloatingNavInset
import sh.taf.billi.ui.components.listPlacementSpring

@Composable
fun BudgetsScreen(viewModel: BudgetsViewModel = koinViewModel()) {
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currency ?: "₹"

    Column(modifier = Modifier.fillMaxSize()) {
        BilliTopBar(
            title = "Budgets",
            actions = {
                InfoTip(
                    title = "How budgets work",
                    text = "A budget is a monthly limit for one category, or for all spending. " +
                        "The wavy bar fills as you spend this month and starts over on the 1st."
                )
            }
        )

        if (budgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.spacing.medium),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "No budgets yet",
                    mood = BilliMood.Curious,
                    subtitle = "Set monthly limits to stay on track"
                )
            }
        } else {
            // One column on a phone; side by side once there's room for two.
            val gridState = rememberLazyGridState()
            Box(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.medium,
                    end = MaterialTheme.spacing.medium,
                    bottom = MaterialTheme.spacing.extraLarge + LocalFloatingNavInset.current
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                items(budgets, key = { it.id }) { budget ->
                    val category = budget.categoryId?.let { id ->
                        categories.firstOrNull { it.id == id }
                    }
                    val monthPrefix = "${budget.year.toString().padStart(4, '0')}-" +
                        budget.month.toString().padStart(2, '0')
                    val spent = transactions
                        .filter { tx ->
                            tx.type == "expense" &&
                                tx.transactionDate.startsWith(monthPrefix) &&
                                (budget.categoryId == null || tx.categoryId == budget.categoryId)
                        }
                        .sumOf { it.amount }
                    Box(Modifier.animateItem(placementSpec = listPlacementSpring())) {
                    BudgetCard(
                        budget = budget,
                        title = budget.budgetName ?: category?.name ?: "Overall spending",
                        spent = spent,
                        currency = currency,
                        categoryIcon = category?.icon,
                        categoryName = category?.name,
                        categoryColor = category?.color
                    )
                    }
                }
            }
            VerticalScrollbarFor(gridState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
            }
        }
    }
}
