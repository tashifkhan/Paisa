package codes.tashif.paisa.ui.accounts

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.ui.haptics.rememberHaptics

/** Confirms deleting an account together with every transaction recorded against it. */
@Composable
internal fun DeleteAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) = DeleteAccountsDialog(listOf(account), onDismiss, onConfirm)

/** Confirms deleting one or more accounts and every transaction recorded against them. */
@Composable
internal fun DeleteAccountsDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val haptics = rememberHaptics()
    val single = accounts.size == 1
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (single) "Delete ${accounts.first().name}?" else "Delete ${accounts.size} accounts?")
        },
        text = {
            Text(
                if (single) {
                    "This permanently deletes the account and every transaction " +
                        "recorded against it. This cannot be undone."
                } else {
                    "This permanently deletes " +
                        accounts.joinToString(", ") { it.name } +
                        ", and every transaction recorded against them. This cannot be undone."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                haptics.reject()
                onConfirm()
            }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
