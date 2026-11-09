package codes.tashif.paisa.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import codes.tashif.paisa.ui.components.AccountVisuals
import codes.tashif.paisa.ui.components.ChoiceOption
import codes.tashif.paisa.ui.components.OptionChipGrid
import codes.tashif.paisa.ui.components.SheetSectionLabel
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

private val accountSeedColors = listOf(
    "#4CAF50", "#2196F3", "#FF9800", "#9C27B0",
    "#E91E63", "#00BCD4", "#FFC107", "#795548"
)

private val accountTypes = listOf(
    "Bank Account", "Cash", "UPI", "Credit Card", "Debit Card", "Wallet", "Other"
)

/** Add-account bottom sheet wired to the ViewModel; opened from the Accounts FAB. */
@Composable
fun AddAccountSheetHost(
    viewModel: AccountsViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    AddAccountSheet(
        onDismiss = onDismiss,
        onAdd = { name, type, openingBalance ->
            viewModel.addAccount(
                name = name,
                type = type,
                openingBalance = openingBalance,
                icon = if (type == "Credit Card" || type == "Debit Card") {
                    "credit_card"
                } else {
                    "account_balance"
                },
                color = accountSeedColors[
                    kotlin.math.abs(name.hashCode()) % accountSeedColors.size
                ]
            )
            onDismiss()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddAccountSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, type: String, openingBalance: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(accountTypes.first()) }
    var balanceText by remember { mutableStateOf("") }
    val haptics = rememberHaptics()

    val typeOptions = remember {
        accountTypes.map { option ->
            ChoiceOption(
                id = option,
                label = option,
                icon = AccountVisuals.icon(option)
            )
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
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
                text = "Add account",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Name it and pick how you spend from it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Account name") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            SheetSectionLabel("Type")
            OptionChipGrid(
                options = typeOptions,
                selectedId = type,
                onSelect = { type = it }
            )
            OutlinedTextField(
                value = balanceText,
                onValueChange = { text ->
                    if (text.isEmpty() || text.toDoubleOrNull() != null || text == "-") {
                        balanceText = text
                    }
                },
                label = { Text("Opening balance") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    haptics.confirm()
                    onAdd(name, type, balanceText.toDoubleOrNull() ?: 0.0)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text("Add account", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
