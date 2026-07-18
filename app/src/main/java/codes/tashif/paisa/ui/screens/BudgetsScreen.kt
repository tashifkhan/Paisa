package codes.tashif.paisa.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.Budget
import codes.tashif.paisa.data.PaisaViewModel
import codes.tashif.paisa.ui.components.CategoryBadge
import codes.tashif.paisa.ui.components.EmptyState
import codes.tashif.paisa.ui.components.PaisaTopBar
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun BudgetsScreen(viewModel: PaisaViewModel) {
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val summary by viewModel.homeSummary.collectAsState()

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
                        currency = summary.currency,
                        categoryIcon = category?.icon,
                        categoryName = category?.name,
                        categoryColor = category?.color
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetSheet(
    viewModel: PaisaViewModel,
    onDismiss: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currency ?: "₹"
    val expenseCategories = categories.filter { it.type == "expense" }
    var selectedCategoryId by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<Int?>(null)
    }
    var amountText by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf("")
    }
    val haptics = rememberHaptics()
    val keyboardController = LocalSoftwareKeyboardController.current

    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = "Add budget",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Set a monthly limit. Pick a category, or Overall for total spending.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            codes.tashif.paisa.ui.components.CategoryPickerWithOverall(
                categories = expenseCategories,
                selectedId = selectedCategoryId,
                onSelect = { selectedCategoryId = it }
            )
            androidx.compose.material3.OutlinedTextField(
                value = amountText,
                onValueChange = { text ->
                    if (text.isEmpty() || text.toDoubleOrNull() != null) {
                        amountText = text
                    }
                },
                label = { Text("Monthly limit") },
                prefix = { Text(currency) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            androidx.compose.material3.Button(
                onClick = {
                    haptics.confirm()
                    viewModel.addBudget(
                        categoryId = selectedCategoryId,
                        amount = amountText.toDoubleOrNull() ?: 0.0,
                        name = null
                    )
                    onDismiss()
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text("Add budget", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BudgetCard(
    budget: Budget,
    title: String,
    spent: Double,
    currency: String,
    categoryIcon: String? = null,
    categoryName: String? = null,
    categoryColor: String? = null
) {
    val progress = if (budget.budgetAmount > 0) {
        (spent / budget.budgetAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val overBudget = spent > budget.budgetAmount
    val periodLabel = runCatching {
        val calendar = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, budget.year)
            set(Calendar.MONTH, budget.month - 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    }.getOrDefault("${budget.month}/${budget.year}")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(
                    iconName = categoryIcon,
                    categoryName = categoryName ?: title,
                    colorHex = categoryColor,
                    size = 42.dp
                )
                Spacer(Modifier.size(MaterialTheme.spacing.small))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${formatMoney(currency, spent)} / " +
                        formatMoney(currency, budget.budgetAmount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (overBudget) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.smaller))
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (overBudget) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            if (overBudget) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                Text(
                    text = "Over budget by ${formatMoney(currency, spent - budget.budgetAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
