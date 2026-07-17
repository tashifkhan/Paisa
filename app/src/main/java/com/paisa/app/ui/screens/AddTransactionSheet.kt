package com.paisa.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.paisa.app.data.PaisaViewModel
import com.paisa.app.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    viewModel: PaisaViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var type by remember { mutableStateOf("expense") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var selectedAccountId by remember { mutableStateOf<Int?>(null) }

    val filteredCategories = categories.filter { it.type == type }

    LaunchedEffect(type, filteredCategories) {
        if (selectedCategoryId == null || filteredCategories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = filteredCategories.firstOrNull()?.id
        }
    }
    LaunchedEffect(accounts) {
        if (selectedAccountId == null) {
            selectedAccountId = accounts.firstOrNull()?.id
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge)
        ) {
            Text(
                text = "Add transaction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
                FilterChip(
                    selected = type == "expense",
                    onClick = { type = "expense" },
                    label = { Text("Expense") }
                )
                FilterChip(
                    selected = type == "income",
                    onClick = { type = "income" },
                    label = { Text("Income") }
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Amount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            Text("Category", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.smaller))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                filteredCategories.take(6).forEach { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { selectedCategoryId = category.id },
                        label = { Text(category.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            Text("Account", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.smaller))
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                accounts.forEach { account ->
                    FilterChip(
                        selected = selectedAccountId == account.id,
                        onClick = { selectedAccountId = account.id },
                        label = { Text(account.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    val categoryId = selectedCategoryId
                    val accountId = selectedAccountId
                    if (amount == null || amount <= 0.0 || categoryId == null || accountId == null) {
                        viewModel.showSnackbar("Enter a valid amount, category, and account")
                        return@Button
                    }
                    viewModel.addTransaction(
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        accountId = accountId,
                        note = note.trim()
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
