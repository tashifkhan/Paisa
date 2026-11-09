package codes.tashif.paisa.ui.debts

import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import codes.tashif.paisa.ui.haptics.rememberHaptics


/** Confirms adding one entry of [amount] (already formatted) that evens things out. */
@Composable
internal fun SettleDebtDialog(
    name: String,
    amount: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val haptics = rememberHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settle up with $name?") },
        text = {
            Text("Adds one entry for $amount so you're even. History stays.")
        },
        confirmButton = {
            TextButton(onClick = {
                haptics.confirm()
                onConfirm()
            }) { Text("Settle up") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Confirms removing a person and their whole history from Debts. */
@Composable
internal fun DeleteDebtPersonDialog(
    name: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val haptics = rememberHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete $name?") },
        text = { Text("Removes them and their whole history from Debts. Your transactions stay as they are.") },
        confirmButton = {
            TextButton(onClick = {
                haptics.reject()
                onConfirm()
            }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
