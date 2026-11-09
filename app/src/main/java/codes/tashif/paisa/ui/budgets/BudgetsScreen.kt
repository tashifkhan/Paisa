package codes.tashif.paisa.ui.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import codes.tashif.paisa.ui.components.EmptyState
import codes.tashif.paisa.ui.components.PaisaTopBar
import codes.tashif.paisa.ui.theme.spacing
import java.util.Locale

@Composable
fun BudgetsScreen(viewModel: BudgetsViewModel = hiltViewModel()) {
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currency ?: "₹"

    Column(modifier = Modifier.fillMaxSize()) {
        PaisaTopBar(title = "Budgets")

        if (budgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.spacing.medium),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "No budgets yet",
                    subtitle = "Set monthly limits to stay on track"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.medium,
                    end = MaterialTheme.spacing.medium,
                    bottom = MaterialTheme.spacing.extraLarge
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                items(budgets, key = { it.id }) { budget ->
                    val category = budget.categoryId?.let { id ->
                        categories.firstOrNull { it.id == id }
                    }
                    val monthPrefix = String.format(
                        Locale.ROOT,
                        "%04d-%02d",
                        budget.year,
                        budget.month
                    )
                    val spent = transactions
                        .filter { tx ->
                            tx.type == "expense" &&
                                tx.transactionDate.startsWith(monthPrefix) &&
                                (budget.categoryId == null || tx.categoryId == budget.categoryId)
                        }
                        .sumOf { it.amount }
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
    }
}
