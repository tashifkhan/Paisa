package sh.taf.billi.ui.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sh.taf.billi.data.DebtPersonBalance
import sh.taf.billi.ui.components.elasticCard
import sh.taf.billi.ui.components.groupPositionOf
import sh.taf.billi.ui.components.groupShape
import sh.taf.billi.ui.components.rowTone
import sh.taf.billi.ui.format.dayLabel
import sh.taf.billi.ui.format.formatMoney
import sh.taf.billi.ui.haptics.rememberHaptics
import sh.taf.billi.ui.platform.handCursor
import sh.taf.billi.ui.theme.ExpenseRed
import sh.taf.billi.ui.theme.IncomeGreen
import sh.taf.billi.ui.theme.spacing
import kotlin.math.abs

/**
 * One person in a segmented group: a shaped avatar with their initials, how
 * many entries and when the last one was, and the balance in a tonal pill.
 * The avatar's shape and tone come from the name, so a person keeps theirs.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DebtPersonRow(
    entry: DebtPersonBalance,
    currency: String,
    index: Int,
    count: Int,
    /** Open in the pane beside the list, on wide windows. */
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val haptics = rememberHaptics()
    val seed = abs(entry.person.name.lowercase().hashCode())
    val tone = rowTone(seed)
    val avatarShape = avatarShapes[seed % avatarShapes.size].toShape()
    Surface(
        onClick = {
            haptics.click()
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .elasticCard()
            .handCursor(),
        shape = groupShape(groupPositionOf(index, count)),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    ) {
        Row(
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                top = 14.dp,
                bottom = 14.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(avatarShape)
                    .background(tone.container),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials(entry.person.name),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tone.content
                )
            }
            Spacer(Modifier.width(MaterialTheme.spacing.medium))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = entry.person.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                activityLine(entry)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(MaterialTheme.spacing.small))
            BalancePill(currency = currency, balance = entry.balance)
        }
    }
}

/** The amount on a soft wash of its own color: green owed to you, red you owe. */
@Composable
private fun BalancePill(currency: String, balance: Double) {
    val color = when {
        balance >= SETTLED -> IncomeGreen
        balance <= -SETTLED -> ExpenseRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = if (abs(balance) < SETTLED) "Settled" else formatMoney(currency, abs(balance)),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** "3 entries · Yesterday", or null for someone with nothing recorded. */
private fun activityLine(entry: DebtPersonBalance): String? {
    if (entry.entryCount == 0) return null
    val entries = if (entry.entryCount == 1) "1 entry" else "${entry.entryCount} entries"
    val last = entry.lastEntryDate?.take(10)?.let { dayLabel(it).removeYearIfCurrent() }
    return listOfNotNull(entries, last).joinToString(" · ")
}

/** "Mon, 3 Nov 2026" → "Mon, 3 Nov" in the current year. Today and Yesterday pass through. */
private fun String.removeYearIfCurrent(): String {
    val year = sh.taf.billi.platform.formatDate(sh.taf.billi.platform.nowMillis(), "yyyy")
    return removeSuffix(" $year")
}

/** Up to two initials: "Omar Shahab" → "OS", "Radhika" → "R". */
private fun initials(name: String): String =
    name.trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifEmpty { "?" }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val avatarShapes = listOf(
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Sunny,
    MaterialShapes.Cookie6Sided,
    MaterialShapes.Pentagon,
    MaterialShapes.Circle
)
