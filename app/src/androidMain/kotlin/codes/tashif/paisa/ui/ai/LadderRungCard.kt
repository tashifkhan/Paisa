package codes.tashif.paisa.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ai.provider.RouteMode
import codes.tashif.paisa.ai.provider.ThinkingLevel
import codes.tashif.paisa.ui.components.GroupPosition
import codes.tashif.paisa.ui.components.groupShape
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/** One rung as the ladder screen shows it. */
data class RungRow(
    val number: Int,
    val modelName: String,
    val providerName: String,
    /** Whose logo to show: the upstream provider for RelayRoom models. */
    val logoProviderId: String,
    val customProvider: Boolean,
    val thinking: ThinkingLevel?,
    /** Supported levels, not counting Auto. Empty hides the thinking chips. */
    val thinkingChoices: List<ThinkingLevel>,
    val routeMode: RouteMode,
    /** Empty hides the route chips; only RelayRoom rungs have them. */
    val routeChoices: List<RouteMode>,
    /** Why the runner will skip this rung, if it will. */
    val warning: String?,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LadderRungCard(
    row: RungRow,
    position: GroupPosition,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onThinking: (ThinkingLevel?) -> Unit,
    onRouteMode: (RouteMode) -> Unit
) {
    val haptics = rememberHaptics()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = groupShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(start = MaterialTheme.spacing.medium, end = 4.dp, top = 10.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "${row.number}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.size(MaterialTheme.spacing.small))
                ProviderAvatar(
                    providerId = row.logoProviderId,
                    custom = row.customProvider,
                    size = 36.dp
                )
                Spacer(Modifier.size(MaterialTheme.spacing.small))
                Column(Modifier.weight(1f)) {
                    Text(row.modelName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        row.providerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onMoveUp, enabled = row.canMoveUp) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move ${row.modelName} up")
                }
                IconButton(onClick = onMoveDown, enabled = row.canMoveDown) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move ${row.modelName} down")
                }
                IconButton(onClick = {
                    haptics.reject()
                    onRemove()
                }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove ${row.modelName}")
                }
            }
            row.warning?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp, end = MaterialTheme.spacing.small)
                )
            }
            if (row.routeChoices.isNotEmpty()) {
                ChipLine("Route through") {
                    row.routeChoices.forEach { mode ->
                        FilterChip(
                            selected = row.routeMode == mode,
                            onClick = {
                                haptics.tick()
                                onRouteMode(mode)
                            },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }
            if (row.thinkingChoices.isNotEmpty()) {
                ChipLine("Thinking") {
                    (listOf<ThinkingLevel?>(null) + row.thinkingChoices).forEach { level ->
                        FilterChip(
                            selected = row.thinking == level,
                            onClick = {
                                haptics.tick()
                                onThinking(level)
                            },
                            label = { Text(ThinkingLevel.label(level)) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipLine(label: String, chips: @Composable () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = MaterialTheme.spacing.smaller)
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) { chips() }
}
