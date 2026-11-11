package codes.tashif.paisa.ui.sms

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.UnrecognizedSms
import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.ui.components.GroupPosition
import codes.tashif.paisa.ui.components.groupShape
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/**
 * One SMS that looked financial but matched no bank parser. Reads like a
 * message: sender avatar, sender and time, then the text, which expands on tap.
 */
@Composable
fun UnrecognizedSmsCard(
    sms: UnrecognizedSms,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    position: GroupPosition = GroupPosition.Single,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    var expanded by rememberSaveable(sms.id) { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = groupShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .animateContentSize()
                .padding(MaterialTheme.spacing.medium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            senderInitial(sms.sender),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(MaterialTheme.spacing.small))
                Column(Modifier.weight(1f)) {
                    Text(
                        sms.sender,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatDate(sms.timestamp, "d MMM, h:mm a"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            Text(
                sms.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller, Alignment.End)
            ) {
                TextButton(
                    onClick = {
                        haptics.reject()
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete")
                }
                FilledTonalButton(
                    onClick = {
                        haptics.click()
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Dismiss")
                }
            }
        }
    }
}

/**
 * Indian bank senders look like "TX-SWIGGY-S" or "AD-HDFCBK": the brand is the
 * middle part, so its first letter makes a better avatar than the "T" of "TX".
 */
private fun senderInitial(sender: String): String {
    val parts = sender.split('-').filter { it.isNotBlank() }
    val brand = if (parts.size >= 2 && parts[0].length <= 2) parts[1] else parts.firstOrNull().orEmpty()
    return brand.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "#"
}
