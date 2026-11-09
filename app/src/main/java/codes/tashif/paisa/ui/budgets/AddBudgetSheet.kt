package codes.tashif.paisa.ui.budgets

import androidx.compose.foundation.layout.Arrangement
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetSheet(
    onDismiss: () -> Unit,
    viewModel: BudgetsViewModel = koinViewModel()
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
