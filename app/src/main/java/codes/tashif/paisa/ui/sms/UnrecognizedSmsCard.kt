package codes.tashif.paisa.ui.sms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import codes.tashif.paisa.data.UnrecognizedSms
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

@Composable
fun UnrecognizedSmsCard(
    sms: UnrecognizedSms,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberHaptics()
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Text(sms.sender, fontWeight = FontWeight.SemiBold)
            Text(
                sms.body.take(180) + if (sms.body.length > 180) "…" else "",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(MaterialTheme.spacing.smaller))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    MaterialTheme.spacing.smaller,
                    Alignment.End
                )
            ) {
                TextButton(
                    onClick = {
                        haptics.reject()
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
                OutlinedButton(
                    onClick = {
                        haptics.click()
                        onDismiss()
                    }
                ) {
                    Text("Dismiss")
                }
            }
        }
    }
}
