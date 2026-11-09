package codes.tashif.paisa.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import codes.tashif.paisa.ui.platform.handCursor
import codes.tashif.paisa.ui.platform.onSecondaryClick
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen
import codes.tashif.paisa.ui.theme.spacing

/** One transaction card: category badge, title, details line and signed amount. */
@Composable
fun TransactionRow(
    tx: TransactionWithDetails,
    currency: String,
    balancesHidden: Boolean = false,
    /** Shown in the detail pane next to the list, on wide windows. */
    selected: Boolean = false,
    /** Adds Delete to the right-click menu. */
    onDelete: (() -> Unit)? = null,
    /** Where the row sits in a day's group, which sets its corners, like More's rows. */
    position: GroupPosition = GroupPosition.Single,
    onClick: () -> Unit = {}
) {
    var menuAt by remember { mutableStateOf<DpOffset?>(null) }
    val density = LocalDensity.current
    val isIncome = tx.type == "income"
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val sign = if (isIncome) "+" else "−"
    val sourceLabel = when (tx.source) {
        "sms" -> "SMS"
        "statement" -> "Statement"
        else -> null
    }
    val haptics = rememberHaptics()

    var rowHeight by remember { mutableStateOf(0.dp) }
    Box(Modifier.onSizeChanged { rowHeight = with(density) { it.height.toDp() } }) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onSecondaryClick { at ->
                menuAt = with(density) { DpOffset(at.x.toDp(), at.y.toDp()) }
            }
            .clickable {
                haptics.click()
                onClick()
            }
            .handCursor(),
        shape = groupShape(position),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryBadge(
                iconName = tx.categoryIcon,
                categoryName = tx.categoryName,
                colorHex = tx.categoryColor,
                size = 42.dp
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.merchantName?.takeIf { it.isNotBlank() }
                        ?: tx.note.takeIf { it.isNotBlank() }
                        ?: tx.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = listOfNotNull(
                        tx.categoryName,
                        tx.accountName,
                        tx.bankName?.takeIf { tx.source != "manual" },
                        sourceLabel
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MoneyText(
                currency = currency,
                amount = tx.amount,
                hidden = balancesHidden,
                style = MaterialTheme.typography.titleSmall,
                color = amountColor,
                fontWeight = FontWeight.Bold,
                prefix = sign,
                maskCount = 5
            )
        }
    }
    // Right-click menu, anchored where the click happened.
    DropdownMenu(
        expanded = menuAt != null,
        onDismissRequest = { menuAt = null },
        // The offset counts from the row's bottom edge.
        offset = menuAt?.let { DpOffset(it.x, it.y - rowHeight) } ?: DpOffset.Zero
    ) {
        DropdownMenuItem(
            text = { Text("Open") },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
            onClick = {
                menuAt = null
                onClick()
            }
        )
        if (onDelete != null) {
            DropdownMenuItem(
                text = { Text("Delete…") },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                onClick = {
                    menuAt = null
                    onDelete()
                }
            )
        }
    }
    }
}
