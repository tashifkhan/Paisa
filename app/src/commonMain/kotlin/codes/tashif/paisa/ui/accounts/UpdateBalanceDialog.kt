package codes.tashif.paisa.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

@Composable
internal fun UpdateBalanceDialog(
    account: Account,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var balanceText by remember(account.id) {
        mutableStateOf(manualBalanceInputText(account.currentBalance))
    }
    val parsedBalance = parseManualBalanceInput(balanceText)
    val haptics = rememberHaptics()
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update balance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
                Text(
                    text = "Set the current balance for ${account.name}. " +
                        "This won't add a transaction or change its opening balance.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Current balance") },
                    supportingText = { Text("Use a minus sign for money owed or overdrawn.") },
                    isError = balanceText.isNotBlank() &&
                        balanceText != "-" && parsedBalance == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    haptics.confirm()
                    onSave(parsedBalance ?: return@TextButton)
                },
                enabled = parsedBalance != null && parsedBalance != account.currentBalance
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

internal fun parseManualBalanceInput(value: String): Double? =
    value.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
