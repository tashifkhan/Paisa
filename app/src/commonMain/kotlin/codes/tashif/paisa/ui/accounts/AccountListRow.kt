package codes.tashif.paisa.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.ui.components.AccountVisuals
import codes.tashif.paisa.ui.components.GroupPosition
import codes.tashif.paisa.ui.components.groupShape
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun AccountListRow(
    account: Account,
    currency: String,
    position: GroupPosition,
    selectionMode: Boolean,
    selected: Boolean,
    isDragging: Boolean,
    dragOffset: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pixel-settings surface: large corners on group ends, 6dp between rows.
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = dragOffset
                shadowElevation = if (isDragging) 8.dp.toPx() else 0f
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = groupShape(position),
        color = when {
            isDragging -> MaterialTheme.colorScheme.surfaceContainerHigh
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.small + 2.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                val haptics = rememberHaptics()
                Checkbox(
                    checked = selected,
                    onCheckedChange = {
                        haptics.toggle(on = !selected)
                        onLongClick()
                    }
                )
            } else {
                val accent = AccountVisuals.color(account.color)
                    ?: MaterialTheme.colorScheme.primaryContainer
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AccountVisuals.icon(account.type, account.icon),
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.size(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (account.isDefault) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = "Default account",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(16.dp)
                        )
                    }
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = listOfNotNull(
                        account.type,
                        account.bankName,
                        account.accountLast4?.let { "••$it" }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.size(MaterialTheme.spacing.smaller))
            Text(
                text = formatMoney(currency, account.currentBalance),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (!selectionMode) {
                Icon(
                    imageVector = Icons.Rounded.DragIndicator,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .padding(start = MaterialTheme.spacing.extraSmall)
                        .size(22.dp)
                        .pointerInput(account.id) {
                            detectDragGestures(
                                onDragStart = { onDragStart() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.y)
                                },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() }
                            )
                        }
                )
            }
        }
    }
}
