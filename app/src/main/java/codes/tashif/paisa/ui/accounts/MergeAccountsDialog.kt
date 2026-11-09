package codes.tashif.paisa.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

@Composable
internal fun MergeAccountsDialog(
    accounts: List<Account>,
    currency: String,
    onDismiss: () -> Unit,
    onMerge: (targetId: Int) -> Unit
) {
    var targetId by remember {
        // Default to the account with the most complete metadata / highest balance
        mutableIntStateOf(
            accounts.maxByOrNull { (if (it.accountLast4 != null) 1 else 0) * 1_000_000 + it.currentBalance }
                ?.id ?: accounts.first().id
        )
    }

    val haptics = rememberHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge accounts") },
        text = {
            Column {
                Text(
                    text = "All transactions move into the account you keep. " +
                        "Its balance becomes the latest SMS-reported balance. " +
                        "The other accounts are deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                accounts.forEach { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (targetId != account.id) haptics.tick()
                                targetId = account.id
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = targetId == account.id,
                            onClick = {
                                if (targetId != account.id) haptics.tick()
                                targetId = account.id
                            }
                        )
                        Column {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = listOfNotNull(
                                    account.bankName,
                                    account.accountLast4?.let { "••$it" },
                                    formatMoney(currency, account.currentBalance)
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                haptics.confirm()
                onMerge(targetId)
            }) {
                Text("Merge into selected")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
